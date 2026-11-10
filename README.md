# BeforeTheFall — Atomic Survival (Paper 1.21.4)

Paper plugin for an atomic-disaster survival scenario. Requires Minecraft 1.21.4 and Java 21.

## Install
1. Download the latest successful [GitHub Actions artifact](https://github.com/login19ruman1-spec/Atomic/actions).
2. Put the JAR in `plugins/` and restart Paper.
3. The plugin extracts its embedded resource pack to `plugins/BeforeTheFall/resourcepack.zip` and requests it on join. Accept the pack to see the custom gas-mask/filter icons and the first-person mask visor.

The pack is generated during the Gradle build, embedded in the JAR, and published as a public GitHub Release asset so clients can download the exact matching ZIP automatically. The pack currently skins the vanilla snow-layer block as ash while retaining native thin-layer collision and stacking behavior. This is a deliberate visual trade-off: Paper cannot register a truly separate custom thin-layer block with snow collision without a client mod or a more invasive block-state workaround; vanilla snow layers will therefore share the ash texture while this pack is active. The mask view is currently a resource-pack overlay based on the carved-pumpkin helmet overlay; it is not a custom 3D player camera.

## Crafting

All recipes require a 3×3 crafting table. These outputs are plugin-tagged: vanilla lookalike items from Creative/commands do not work as replacements.

### Gas mask
```
Leather     Glass Pane  Leather
Iron Ingot  Iron Bars   Iron Ingot
-           Leather    -
```
Wear it in the helmet slot.

### Replacement filter
```
Paper       Charcoal    Paper
Redstone    Iron Ingot  Redstone
Paper       Charcoal    Paper
```
Wear the mask and right-click while holding the crafted filter. One filter restores 15 minutes by default. The filter is a separate paper-based custom item with its own icon and persistent plugin marker; a normal iron ingot or paper cannot replace it.

### Nuclear charge
```
Gold Ingot    Redstone Block  Gold Ingot
TNT           Clock           TNT
Gold Ingot    Redstone Block  Gold Ingot
```
Place the crafted TNT block, then ignite it with flint and steel/fire charge or redstone. Ordinary TNT does not trigger a nuclear detonation.

### Bunker core (lodestone)
```
Obsidian    Iron Block      Obsidian
Iron Block  Redstone Block  Iron Block
Obsidian    Iron Block      Obsidian
```
Only the crafted/tagged lodestone activates a bunker; vanilla lodestone from Creative does not.

### Yellow radiation suit
Craft each piece separately using leather and yellow dye:
- Helmet: `LYL / L - L / - - -`
- Chestplate: `L - L / LYL / L - L`
- Leggings: `LYL / L - L / L - L`
- Boots: `- - - / L - L / L - L`
`L` = leather, `Y` = yellow dye. Wear all four tagged pieces for suit protection.

## Nuclear event
The detonation uses a flash, expanding shockwave, animated eight-frame pixel-art mushroom cloud, camera shake, distant sound layers and block damage. Aftermath conversion processes already-loaded chunks in batches and also converts chunks as they are explored later. Basalt, gravel, sand and coarse dirt are mixed per column with deterministic randomness to avoid large square patches. Surface ice is broken/melted rather than converted into soil; water is left as water. Ordinary snow remains vanilla. Trees, leaves and plants are removed inside the configured radius. Ashfall accumulates in 1–8 thin layers using snow-layer physics, but the resource pack displays those layers as grey ash. Default block damage radius is 25 blocks; edits are batched to avoid server stalls. Fallout is persistent and defaults to 400 blocks for 72 hours. Fallout dose causes staged nausea, hunger, weakness, poison and severe illness.

The distant warning is sent to players within 3000 blocks, the shock layer within 2000 blocks and the close blast within 50 blocks. The current source uses Minecraft sound events for these layers. The three MP3 samples supplied in the conversation are visible to the build workspace but cannot be attached to the GitHub repository as binary assets through the available GitHub text-file actions; they must be converted to OGG and uploaded as pack assets before the exact recordings can be played. I have not substituted fake audio for those recordings.

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
CI verifies the plugin descriptor and embedded resource pack, then uploads the JAR as a 30-day Actions artifact. This is a native Paper implementation, not a binary conversion of the Forge mod; original All Rights Reserved mod assets are not redistributed.
