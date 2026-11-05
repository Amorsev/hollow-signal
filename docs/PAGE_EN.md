# Hollow Signal

**A slow-burn psychological horror mod. Something is listening to your world.**

It starts small. A torch you placed is gone. A sign you didn't write stands behind you. Something left a gift in your chest.
Then there's a figure at the edge of the fog that disappears when you look at it.
Then someone joins your singleplayer world, and they start to talk.

> ⚠️ **SAFE by default.** Out of the box everything happens inside the game window. An optional **HARDCORE** mode can break the fourth wall, but only after you read a warning screen and type a confirmation phrase. See [Safety](#safety--hardcore-mode) below.

---

## Features

### A story that builds slowly
- A hidden **director** decides what happens, when, and to whom. It starts after a few quiet in-game days and moves through 5 stages: traces → watching → contact → hostility → finale.
- At default settings you reach the finale after roughly **7 hours** of play. You can make it slower or faster with `intensity`.

### Entities
- **The Listener.** It stands at the edge of your vision and stares, and vanishes when you stare back. It turns your head towards it from behind you. Late in the story it sneaks up while you aren't looking.
- **The Watcher** appears when you stand still for too long. Keep standing still and it calls…
- **The Reporter.** It's small and fast, and it runs away from you. Catch it within 5 seconds or it reports where you are.
- **The Hollow** is the only one that fights. It digs through walls to reach you, and while it's close your hands won't open your inventory. It can't stand bright light, so torches are your defence.

### Traces in your world
Stolen or reddened torches, signs facing you, gifts in your chests, perfectly straight 2×1 tunnels.

### It talks
- "*Name* joined the game." It appears in the Tab list, greets you, and answers what you write. It understands 30+ topics in **English and Russian**, with many phrasings for each.
- It remembers how you treat it: kindness, insults, gifts, broken signs. That **relationship** changes what it says, and it decides the ending.
- If it doesn't understand you, it answers in the enchanting-table glyphs.

### Glitches
Light screen interference with radio static on every encounter. You also get:
- fog that closes in;
- a HUD that flickers out;
- the whole world's sounds slowing down;
- footsteps behind you;
- fake "Loading world" and "Connection Lost" screens;
- a Windows 10-style error dialog.

### Old-school look
An Alpha/Beta-style main menu with a falling stone-block logo, eerie splash texts and a "version" line that is never quite right. It also turns on the built-in **Programmer Art** classic textures. Both can be switched off.

### The finale: *the_static*
A void dimension full of floating fragments of a broken world. Cross the broken bridges, turn on three receivers, touch the core. There are **three endings**: Released, Loop or Record Erased.

### Better with friends
- **Split reality.** Scares and impersonations are sent only to the victim, so your friend standing next to you sees nothing.
- **Mimic.** The Listener can wear a friend's **name and skin** and whisper as them.
- A fake "*friend* left the game" message, while your friend quietly vanishes from your Tab list.
- Chat messages that echo back… slightly wrong.
- **Co-op finale.** Everyone must be at the core. The ending is decided by the group's average relationship.
- Lonely players are targeted more. Groups get fewer jumpscares and more traces.

---

## Safety & HARDCORE mode

| | SAFE (default) | HARDCORE (opt-in) |
|---|---|---|
| Error messages | In-game Windows-style dialog | Real system message box |
| Notes | "It tried to leave you a note" | **New** `.txt` files in your user folders (Desktop, Documents, Downloads…) |
| Bad ending | Fake crash and fake shutdown screen, then back to the game | The world is **saved**, then the game really crashes |
| PC shutdown | — | **Separate opt-in, off by default, even in HARDCORE.** Windows only. A delayed, cancellable shutdown (`shutdown /a` or `cancel_shutdown.bat` on your Desktop) |
| Window title | unchanged | Changes for a minute |

**What HARDCORE will never do:**
- overwrite, modify or delete your files. Notes are created with *create-new* only;
- make network requests;
- take orders from other players. The server only sends an *intent*: your client decides, and all texts come from the mod's own language files.

**Cleaning up and switching off:**
- `/hollowsignal cleanup` (or the button in the mod's settings) removes the notes the mod created, but only ones you haven't edited.
- `/hollowsignal safemode` switches everything off instantly, on any server.
- Server admins can forbid HARDCORE effects for everyone: `allowHardcoreEffects = false`.

---

## Commands
| Command | Who | What |
|---|---|---|
| `/hollowsignal safemode` | anyone (client) | Back to SAFE instantly |
| `/hollowsignal cleanup` | anyone (client) | Remove notes the mod created |
| `/hs status` | op | Day, stage, world dread |
| `/hs events [player]` | op | Every event with its stage, weight and cooldown |
| `/hs trigger <event> [players]` | op | Run an event now (for testing or streams) |
| `/hs dread`, `/hs relationship`, `/hs global` | op | Adjust hidden values |
| `/hs static enter/leave/reset`, `/hs ending <type>` | op | Finale tools |
| `/hs reset` | op | Reset the story |

## Configuration
- **Client** (mod settings screen): safety mode, each HARDCORE effect, old menu, classic textures.
- **Server / world:** grace days, `intensity`, `disabledEvents`, block changes on/off, Hollow behaviour, mimic and fake-leave toggles, `allowHardcoreEffects`.

## Requirements
- Minecraft **1.21.1**
- NeoForge **21.1.x**
- Required on **both** the server and every client.

## Credits
Hollow Signal Team. Inspired by the psychological horror mod genre. Every entity, texture, sound, line of dialogue and piece of the story is original.
License: MIT.
