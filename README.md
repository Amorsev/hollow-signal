# Hollow Signal

Slow-burn psychological horror mod for **Minecraft 1.21.1 / NeoForge**. Something is listening to your world.

- Player-facing description: [docs/PAGE_EN.md](docs/PAGE_EN.md) · [docs/PAGE_RU.md](docs/PAGE_RU.md)
- Design plan: [docs/PLAN.md](docs/PLAN.md) · Changes: [CHANGELOG.md](CHANGELOG.md) · License: [MIT](LICENSE)

## Building

Requires JDK 21 (Gradle downloads it automatically if missing).

```bash
./gradlew build                # -> build/libs/hollowsignal-neoforge-1.21.1-<version>.jar
./gradlew runClient            # dev client
./gradlew runClient2           # second dev client ("Friend") for multiplayer tests over LAN
./gradlew runGameTestServer    # automated in-game tests
```

## Project layout

| Path | What |
|---|---|
| `src/main/java/com/hollowsignal/director` | Director, stages, dread, scheduler, all events (`events/`) |
| `.../entity` | Listener, Watcher, Reporter, Hollow |
| `.../chat` | Fake player, dialogue loader, Tab entry |
| `.../world` | *the_static* dimension, arena, endings |
| `.../client` | Glitch overlay, fake screens, menus, renderers |
| `.../client/fourthwall` | SAFE/HARDCORE fourth-wall effects (client-only) |
| `.../net` | S2C payloads |
| `.../test` | Game tests |
| `src/main/resources/data/hollowsignal` | Dialogue, dimension, biome, features, test structure |
| `tools/` | Generators: dialogue, skins, block textures, logo, sounds; lang checker; pacing simulator |

## Tools

```bash
node tools/gen_dialogue.js        # dialogue_topics.js -> dialogue JSON + lang files
node tools/check_lang.js          # every used translation key exists in en_us and ru_ru
node tools/simulate_pacing.js 1.0 # expected time to reach each stage
node tools/gen_skins.js           # entity skins
node tools/gen_blocks.js          # receiver/core textures
node tools/gen_logo.js            # logo + page icon
sh tools/gen_static_sound.sh      # all generated sounds (needs ffmpeg with libvorbis)
```

## Publishing checklist

1. Bump `mod_version` in `gradle.properties` and add a CHANGELOG entry.
2. `node tools/check_lang.js` and `./gradlew clean build runGameTestServer` all pass.
3. Upload `build/libs/hollowsignal-neoforge-1.21.1-<version>.jar`.
   - Loader: NeoForge. Game version: 1.21.1.
   - Environment: client **and** server required.
4. Page text: `docs/PAGE_EN.md` / `docs/PAGE_RU.md`. Icon: `docs/icon.png`.
5. Keep the **Safety & HARDCORE** section on the page: platforms require clear disclosure of anything that acts outside the game.
