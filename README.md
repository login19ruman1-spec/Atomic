# BeforeTheFall — Atomic Survival (Paper 1.21.4)

Paper plugin for an atomic-disaster survival scenario. Requires Minecraft 1.21.4 and Java 21.

## Install
1. Download the latest successful [GitHub Actions artifact](https://github.com/login19ruman1-spec/Atomic/actions).
2. Put the JAR in `plugins/` and restart Paper.
3. The plugin extracts its embedded resource pack and requests it on join. Accept it to see the separate gas-mask and filter icons.

The pack skins vanilla snow layers as ash while retaining their thin-layer collision and stacking. Vanilla snow layers share the ash texture while the pack is active. Paper cannot register a genuinely separate custom thin-layer block with snow physics without a client mod or block-state workaround.

The gas mask is a plugin-tagged leather helmet with its own Blockbench model, not a carved pumpkin. Its model changes to fogged and cracked variants as it breathes and wears. The mask loses one durability point every 40 seconds while filtering; at 50% or below the visor is cracked, and at 0% the mask breaks. Exhalation also emits a visible breath cloud. The original Blockbench source files are kept in `src/main/resources/blockbench/`; Gradle exports their geometry and embedded textures into the generated resource pack. A custom first-person visor overlay that activates only when this helmet is worn still requires a client-side mod; a server resource pack cannot conditionally change the camera overlay.

## Crafting

All recipes require a 3×3 crafting table. Crafted outputs are tagged custom items; ordinary lookalike items from Creative or commands are not accepted.

### Gas mask
```
Leather     Glass Pane  Leather
Iron Ingot  Iron Bars   Iron Ingot
-           Leather    -
```
Wear in the helmet slot.

### Replacement filter
```
Paper       Charcoal    Paper
Redstone    Iron Ingot  Redstone
Paper       Charcoal    Paper
```
An alternate recipe accepts an iron nugget in the centre. Wear the mask and right-click while holding the crafted filter. One cartridge lasts 15 minutes by default.

### Nuclear charge
```
Gold Ingot    Redstone Block  Gold Ingot
TNT           Clock           TNT
Gold Ingot    Redstone Block  Gold Ingot
```
Place the crafted TNT block and ignite it with flint and steel/fire charge or redstone. Ordinary TNT does not trigger a nuclear detonation.

### Bunker core (lodestone)
```
Obsidian    Iron Block      Obsidian
Iron Block  Redstone Block  Iron Block
Obsidian    Iron Block      Obsidian
```
Only the crafted/tagged lodestone activates a bunker.

### Yellow radiation suit
Craft each piece separately using leather and yellow dye:
- Helmet: `LYL / L - L / - - -`
- Chestplate: `L - L / LYL / L - L`
- Leggings: `LYL / L - L / L - L`
- Boots: `- - - / L - L / L - L`

## Commands
- `/bft status` — personal radiation and filter status.
- `/bft ashstorm [on|off]` — toggle ash storm (admin).
- `/bft nuke [power]` — test detonation at your position (admin).
- `/bft radiation <player> <0-100>` — set a player's dose (admin).
- `/bft zone` — show radiation zones.
- `/bft reload` — reload configuration (admin).

## Build
```bash
gradle --no-daemon clean test build
```
CI verifies the plugin descriptor and embedded resource pack, then uploads the JAR as a 30-day Actions artifact. This is a native Paper implementation, not a binary conversion of the Forge mod; original All Rights Reserved assets are not redistributed.
