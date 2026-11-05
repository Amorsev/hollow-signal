// Generates original 16x16 block textures for the_static's receiver and core.
// Usage: node tools/gen_blocks.js
const fs = require('fs'), zlib = require('zlib'), path = require('path');
const OUT = path.join(__dirname, '..', 'src', 'main', 'resources', 'assets', 'hollowsignal', 'textures', 'block');
fs.mkdirSync(OUT, { recursive: true });

function tex(name, seed0, paint) {
  const W = 16, H = 16, px = new Uint8Array(W * H * 4);
  let seed = seed0; const rnd = () => (seed = (seed * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff;
  const set = (x, y, r, g, b) => { const i = (y * W + x) * 4; px[i] = r; px[i + 1] = g; px[i + 2] = b; px[i + 3] = 255; };
  paint(set, rnd);
  const crcT = new Uint32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c >>> 0; });
  const crc = b => { let c = 0xffffffff; for (const x of b) c = crcT[(c ^ x) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; };
  const chunk = (t, d) => { const l = Buffer.alloc(4); l.writeUInt32BE(d.length); const td = Buffer.concat([Buffer.from(t), d]); const c = Buffer.alloc(4); c.writeUInt32BE(crc(td)); return Buffer.concat([l, td, c]); };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(W, 0); ihdr.writeUInt32BE(H, 4); ihdr[8] = 8; ihdr[9] = 6;
  const raw = Buffer.alloc((W * 4 + 1) * H);
  for (let y = 0; y < H; y++) Buffer.from(px.buffer, y * W * 4, W * 4).copy(raw, y * (W * 4 + 1) + 1);
  const file = path.join(OUT, name + '.png');
  fs.writeFileSync(file, Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]));
  console.log('wrote', file);
}

// Receiver: dark metal panel, riveted frame, a round "speaker" grille; centre lights red when active.
function receiver(set, rnd, on) {
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const edge = x === 0 || y === 0 || x === 15 || y === 15;
    const v = edge ? 30 : 48 + Math.round((rnd() - 0.5) * 10);
    set(x, y, v, v, v + 4);
  }
  for (const [x, y] of [[1, 1], [14, 1], [1, 14], [14, 14]]) set(x, y, 90, 90, 96);
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const d = Math.hypot(x - 7.5, y - 7.5);
    if (d < 5.5 && d > 4.5) set(x, y, 20, 20, 22);
    else if (d <= 4.5 && (x + y) % 2 === 0) set(x, y, 26, 26, 28);
    if (d < 1.8) on ? set(x, y, 255, 60 + Math.round(rnd() * 30), 50) : set(x, y, 60, 18, 18);
    else if (on && d < 3) set(x, y, 150, 30, 30);
  }
}
tex('signal_receiver_off', 11, (s, r) => receiver(s, r, false));
tex('signal_receiver_on', 11, (s, r) => receiver(s, r, true));

// Core: black, full of white TV static, with a purple ring.
tex('signal_core', 99, (set, rnd) => {
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const d = Math.hypot(x - 7.5, y - 7.5);
    let v = Math.round(rnd() * 120);
    if (d > 6.2) v = Math.round(v * 0.25);
    set(x, y, v, v, v);
    if (d > 5.2 && d < 6.2) set(x, y, 110, 50, 150);
  }
});
