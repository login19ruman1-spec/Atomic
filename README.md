# BeforeTheFall — Atomic Survival (Paper 1.21.4)

Paper plugin for an atomic-disaster survival scenario. Requires Minecraft 1.21.4 and Java 21.

## Install

1. Download the latest `BeforeTheFall-Paper` JAR from the successful [GitHub Actions run](https://github.com/login19ruman1-spec/Atomic/actions).
2. Put it in the server's `plugins/` folder and restart Paper.
3. The plugin extracts its embedded resource pack to `plugins/BeforeTheFall/resourcepack.zip` and asks clients to download it on join. Set `resource-pack.url` in config if you host the ZIP elsewhere. The client must accept the pack to see custom item textures.

## Crafting recipes

The recipes are registered automatically by the plugin. In the 3×3 crafting table, arrange the ingredients exactly as shown (`-` means an empty slot).

### Gas mask

```
Leather     Glass Pane  Leather
Iron Ingot  Iron Bars   Iron Ingot
-           Leather    -
```

### Replaceable filter cartridge

```
Paper       Charcoal    Paper
Redstone    Iron Ingot  Redstone
Paper       Charcoal    Paper
```

A cartridge is a custom plugin item, not a vanilla item. Right-click while holding a cartridge to replace the exhausted cartridge in the gas mask (keep the mask equipped in the helmet slot). A fresh filter lasts 15 minutes by default. The filter blocks inhaled radioactive dust; it does not stop penetrating gamma radiation or the direct blast.

### Nuclear charge

```
Gold Ingot    Redstone Block  Gold Ingot
TNT           Clock           TNT
Gold Ingot    Redstone Block  Gold Ingot
```

Place the charge item in your hand and right-click to trigger the detonation. The charge is consumed.

### Bunker core

```
Obsidian    Iron Block   Obsidian
Iron Block  Redstone Block Iron Block
Obsidian    Iron Block   Obsidian
```

Place the Lodestone core to register a bunker protection area. Default protection radius is 12 blocks; configure `bunkers.radius`, `bunkers.depth` and `bunkers.height` in `config.yml`. The bunker protects against the plugin's radiation and nuclear shock effects inside the configured volume.

## Nuclear detonation and fallout

A detonation is staged rather than a single instantaneous particle: initial flash and blast sound, expanding visible shock front, delayed player impact as the front reaches them, mushroom-shaped smoke/ash cloud, and a persistent radioactive fallout zone. Block damage is enabled by default and limited to a configurable small radius to reduce server load. Bunkers protect players, not blocks.

Relevant settings:

- `nuclear-blast.default-power`: 4–40, default 18.
- `nuclear-blast.destroy-blocks`: whether the blast damages nearby blocks (default true).
- `nuclear-blast.block-radius`: maximum block-damage radius (default 12).

Fallout zones persist across restarts and irradiate players who remain nearby. Radiation builds up over time and causes staged sickness effects; leaving the zone allows the dose to decay. The mask filter only reduces the inhalation component, not external gamma exposure.

## Commands

- `/bft status` — show your current radiation dose.
- `/bft ashstorm [on|off]` — toggle radioactive ash storm (admin).
- `/bft nuke [power]` — trigger a test detonation (admin).
- `/bft radiation <player> <0-100>` — set a player's dose (admin).
- `/bft zone` — show configured radiation zones.
- `/bft reload` — reload configuration (admin).

## Build from source

Requires Java 21 and Gradle 8.10+:

```bash
gradle --no-daemon clean test build
```

The CI workflow builds the plugin and publishes the JAR as an Actions artifact. This is a native Paper implementation, not a binary conversion of the Forge mod. Original mod assets are not redistributed; use only assets you have permission to include.
