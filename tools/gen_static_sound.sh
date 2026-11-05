#!/usr/bin/env sh
# Generates original "signal interference" static sounds (no third-party audio, no license issues).
# Usage: sh tools/gen_static_sound.sh   (needs ffmpeg with libvorbis)
OUT=src/main/resources/assets/hollowsignal/sounds/static
mkdir -p "$OUT"
gen() { # $1 name  $2 tremolo Hz  $3 duration  $4 seed
  ffmpeg -hide_banner -loglevel error -y \
    -f lavfi -i "anoisesrc=d=$3:c=white:a=0.5:r=44100:s=$4" \
    -f lavfi -i "anoisesrc=d=$3:c=velvet:a=0.9:r=44100:s=$(($4+1))" \
    -f lavfi -i "sine=f=52:d=$3:r=44100" \
    -filter_complex "[0]highpass=f=700,lowpass=f=5200,tremolo=f=$2:d=0.7[n];[1]highpass=f=1800,volume=0.5[c];[2]volume=0.12[h];[n][c][h]amix=inputs=3:normalize=0,afade=t=in:d=0.06,afade=t=out:st=$(awk "BEGIN{print $3-0.5}"):d=0.5,volume=0.8" \
    -ac 1 -c:a libvorbis -q:a 4 "$OUT/$1.ogg"
}
gen static1 11 1.6 7
gen static2 17 1.4 23
gen static3 7 1.8 51
echo "done -> $OUT"

# Two-tone "error" chime for the fake error dialog (original, generated).
mkdir -p src/main/resources/assets/hollowsignal/sounds/ui
ffmpeg -hide_banner -loglevel error -y \
 -f lavfi -i "sine=f=784:d=0.6:r=44100" -f lavfi -i "sine=f=587:d=0.6:r=44100" \
 -filter_complex "[0]afade=t=out:st=0:d=0.5,volume=0.5[a];[1]adelay=110,afade=t=out:st=0.11:d=0.5,volume=0.5[b];[a][b]amix=inputs=2:normalize=0,aecho=0.6:0.4:40:0.25,atrim=0:0.75" \
 -ac 1 -c:a libvorbis -q:a 5 src/main/resources/assets/hollowsignal/sounds/ui/error.ogg

# ---- Listener sounds (original, generated) ----
L=src/main/resources/assets/hollowsignal/sounds/listener
mkdir -p "$L"
# scare: dissonant low cluster + noise burst, sharp attack, ~1.8 s decay
scare() { # $1 name $2 base Hz $3 seed
  ffmpeg -hide_banner -loglevel error -y \
    -f lavfi -i "sine=f=$2:d=1.8:r=44100" -f lavfi -i "sine=f=$(awk "BEGIN{print $2*1.06}"):d=1.8:r=44100" \
    -f lavfi -i "sine=f=$(awk "BEGIN{print $2*1.41}"):d=1.8:r=44100" -f lavfi -i "anoisesrc=d=1.8:c=white:a=0.6:s=$3:r=44100" \
    -filter_complex "[0][1][2]amix=inputs=3:normalize=0,acrusher=bits=6:mix=0.5,tremolo=f=9:d=0.5[t];[3]highpass=f=1500,afade=t=out:st=0:d=0.35[n];[t][n]amix=inputs=2:normalize=0,afade=t=in:d=0.01,afade=t=out:st=0.2:d=1.6,volume=0.5,alimiter=limit=0.6" \
    -ac 1 -c:a libvorbis -q:a 5 "$L/$1.ogg"
}
scare scare1 98 3; scare scare2 87 11; scare scare3 110 29
# vanish: a reversed, filtered noise swell that cuts off
vanish() { # $1 name $2 seed
  ffmpeg -hide_banner -loglevel error -y -f lavfi -i "anoisesrc=d=1.2:c=pink:a=0.8:s=$2:r=44100" \
    -af "lowpass=f=1800,afade=t=out:st=0:d=1.2,areverse,afade=t=out:st=1.1:d=0.1,aecho=0.7:0.5:60:0.3,volume=3.0,alimiter=limit=0.8" \
    -ac 1 -c:a libvorbis -q:a 4 "$L/$1.ogg"
}
vanish vanish1 5; vanish vanish2 17
# presence: slow, distant "breathing" (band-passed brown noise, in/out)
presence() { # $1 name $2 breath Hz $3 seed
  ffmpeg -hide_banner -loglevel error -y -f lavfi -i "anoisesrc=d=3.5:c=brown:a=0.9:s=$3:r=44100" \
    -af "bandpass=f=550:w=500,tremolo=f=$2:d=0.95,afade=t=in:d=0.4,afade=t=out:st=2.8:d=0.7,volume=24,alimiter=limit=0.7" \
    -ac 1 -c:a libvorbis -q:a 4 "$L/$1.ogg"
}
presence presence1 0.55 7; presence presence2 0.45 19; presence presence3 0.65 41
