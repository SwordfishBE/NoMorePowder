# NoMorePowder

---

> 🌨️ *No more sinking. No more freezing. Just snow.*

---

[![GitHub Release](https://img.shields.io/github/v/release/SwordfishBE/NoMorePowder?display_name=release&logo=github)](https://github.com/SwordfishBE/NoMorePowder/releases)
[![GitHub Downloads](https://img.shields.io/github/downloads/SwordfishBE/NoMorePowder/total?logo=github)](https://github.com/SwordfishBE/NoMorePowder/releases)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/kPiAZGSa?logo=modrinth&logoColor=white&label=Modrinth%20downloads)](https://modrinth.com/mod/nomorepowder)
[![CurseForge Downloads](https://img.shields.io/curseforge/dt/1490876?logo=curseforge&logoColor=white&label=CurseForge%20downloads)](https://www.curseforge.com/minecraft/mc-mods/nomorepowder)

## 🧾Overview

You're sprinting across a snowy mountain, the landscape looks solid and safe - and then you sink. Powder snow: invisible, silent, and one of Minecraft's most frustrating terrain hazards. No warning, no indication, just a slow freeze creeping up your health bar.

**NoMorePowder** fixes this at the root. Instead of patching around powder snow after the fact, the mod intercepts world generation itself and replaces every naturally-spawned powder snow block with a regular **snow block** before it ever touches the world. The terrain looks identical - it just doesn't swallow you whole anymore.

Your mountain biomes stay snowy and beautiful. You just stop falling through them.

---

## ✨ Features

- 🏔️ **Seamless terrain** - Powder snow patches in frozen and mountain biomes are silently replaced with regular snow blocks during world generation. No visual difference, no more hidden traps.
- 🪣 **Powder snow is not gone** - You can still collect it the intended way: place a cauldron outside during a snowstorm and wait. It can still be placed and used as a block in survival or creative.
- 🏛️ **Trial Chambers untouched** - The powder snow traps inside Trial Chambers are deliberately preserved. They are a designed part of that dungeon's challenge and are generated through a completely different system that this mod does not touch.
- ⚡ **Zero overhead** - No config files, no commands, no runtime processing. The replacement happens once, at worldgen time, and that's it.

---

## ⁉️ What is and isn't affected

| Source | Result |
|---|---|
| Naturally generated powder snow (frozen/mountain biomes) | ✅ Replaced with snow blocks |
| Powder snow collected via cauldron in snowfall | ❌ Unchanged - works as normal |
| Powder snow placed manually (survival/creative) | ❌ Unchanged |
| Trial Chamber powder snow traps | ❌ Unchanged - intentionally preserved |
| Powder snow added by other world generation mods | ⚠️ Only replaced if placed via surface rules |

---

## 📦 Installation

| Platform   | Link |
|------------|------|
| GitHub     | [Releases](https://github.com/SwordfishBE/NoMorePowder/releases) |
| Modrinth   | [NoMorePowder](https://modrinth.com/mod/nomorepowder) |
| CurseForge | [NoMorePowder](https://www.curseforge.com/minecraft/mc-mods/nomorepowder) |

1. Download the latest JAR from your preferred platform above.
2. Place the JAR in your server's `mods/` folder.
3. Start Minecraft — the config file will be created automatically.

> **Server note:** This mod modifies world generation. It must be installed **server-side** (or in singleplayer). Players connecting to a modded server do not need to install it themselves.
> **Existing worlds:** Chunks that were already generated before installing this mod will not be retroactively changed. The effect only applies to newly generated chunks.

---

## 🛠️ Compatibility

- Java **25+**
- Compatible with other mods - the intercept targets only `SurfaceRules$StateRule.tryApply()` and does not affect feature-based placement or structure generation

---

## ⚠️ How it works

Powder snow in frozen biomes (Frozen Peaks, Snowy Slopes, Grove, etc.) is placed via Minecraft's **surface rule system** - not via world generation features. Surface rules determine which block appears at the terrain surface for each biome, and are applied directly to each chunk column during generation, completely bypassing the feature placement pipeline.

NoMorePowder uses a single Mixin on `SurfaceRules$StateRule.tryApply()` with `@ModifyReturnValue` from MixinExtras. This method returns the `BlockState` that a surface rule wants to place. If that state is `POWDER_SNOW`, the mod returns `SNOW_BLOCK` instead - before it ever reaches the chunk.

Trial Chamber powder snow is placed via `StructureTemplate` (NBT-driven structure files) after terrain generation, which is an entirely separate system that never goes through surface rules. It is therefore completely unaffected by this mod.

---

## 🧱 Building from source

```bash
git clone https://github.com/SwordfishBE/NoMorePowder.git
cd NoMorePowder
chmod +x gradlew
./gradlew build
```

The compiled jar will be in `build/libs/`.

---

## 📄 License

Released under the [AGPL-3.0 License](LICENSE).
