# BeforeTheFall — Atomic Survival (Paper 1.21.4)

Paper plugin for an atomic-disaster survival scenario. Requires Minecraft 1.21.4 and Java 21.

## Install

1. Download the latest `BeforeTheFall-Paper` JAR from the successful [GitHub Actions run](https://github.com/login19ruman1-spec/Atomic/actions).
2. Put it in the server's `plugins/` folder and restart Paper.
3. The plugin extracts its embedded resource pack to `plugins/BeforeTheFall/resourcepack.zip` and asks clients to download it on join. Players must accept the pack to see the custom mask, filter, charge, bunker-core and blast textures.

## Crafting recipes

Use a 3×3 crafting table. `-` means an empty slot. The recipes produce plugin-marked items; a vanilla item with the same material is not equivalent.

### Gas mask

```
Leather     Glass Pane  Leather
Iron Ingot  Iron Bars   Iron Ingot
-           Leather    -
```

Equip the mask in the helmet slot. It does not protect from penetrating gamma radiation or direct blast damage.

### Replacement filter

```
Paper       Charcoal    Paper
Redstone    Iron Ingot  Redstone
Paper       Charcoal    Paper
```

The redstone is on the left and right of the centre iron ingot. This is the corrected recipe. Right-click while holding the crafted filter with the mask equipped. One cartridge restores 15 minutes of filtration by default. Filters are plugin-tagged; a normal iron ingot cannot replace one.

### Nuclear TNT charge

```
Gold Ingot    Redstone Block  Gold Ingot
TNT           Clock           TNT
Gold Ingot    Redstone Block  Gold Ingot
```

Place the crafted charge as a TNT block. Ignite that placed block with flint and steel/fire charge, or power it with redstone. Holding the item and right-clicking no longer detonates it in your hand. Ordinary TNT and TNT blocks from Creative do not count as plugin nuclear charges.

### Bunker core (crafted lodestone)

```
Obsidian    Iron Block      Obsidian
Iron Block  Redstone Block  Iron Block
Obsidian    Iron Block      Obsidian
```

Only the plugin-crafted, tagged lodestone activates a bunker. A vanilla lodestone obtained from Creative or commands does not register a bunker. Default protection radius is 12 blocks, with configurable depth and height.

### Yellow radiation suit

Craft each piece separately with leather and yellow dye:

- Helmet: `LYL / L L / - - -`
- Chestplate: `L - L / LYL / L - L`
- Leggings: `LYL / L L / L L`
- Boots: `- - - / L - L / L - L`

`L` = leather, `Y` = yellow dye. Wear all four plugin-crafted pieces to reduce radiation intake. Ordinary yellow leather armor does not count.

## Nuclear detonation

The event has a flash, an expanding shock front, a custom resource-pack blast billboard, a rising mushroom cloud, delayed player impact, camera shake and block damage. The configured block damage radius defaults to 25 blocks and is processed in small batches to reduce server stalls. Persistent fallout is configured to a 400-block radius and 72 hours by default.

Sound layers are sent directly to online players (so buildings and underground positions do not muffle the warning): the distant warning up to 3000 blocks, the shock layer up to 2000 blocks and the close blast within 50 blocks. Current build uses Minecraft's built-in sound events as placeholders; the three uploaded MP3 samples still need to be converted to OGG and included in the pack before those exact recordings can play.

## Ash and fallout

Ash storm particles do not use snow. The resource pack includes an ash-like carpet texture; optional deposits use carpet blocks, not snow layers. The storm can be enabled with `/bft ashstorm on`.

Radiation builds up in fallout zones and causes staged nausea, hunger, weakness, poison and severe illness. A gas-mask filter reduces inhaled radioactive dust; it does not block penetrating gamma radiation. Bunkers suppress the plugin's radiation intake and blast effects for players inside their protected volume.

## Commands

- `/bft status` — show personal radiation and filter status.
- `/bft ashstorm [on|off]` — toggle ash storm (admin).
- `/bft nuke [power]` — test detonation at your position (admin).
- `/bft radiation <player> <0-100>` — set a player's dose (admin).
- `/bft zone` — show configured radiation zones.
- `/bft reload` — reload configuration (admin).

## Build

Requires Java 21 and Gradle 8.10+:

```bash
gradle --no-daemon clean test build
```

The CI workflow compiles, tests, verifies the embedded resource pack and publishes the plugin JAR as an Actions artifact. This is a native Paper implementation, not a binary conversion of the Forge mod. Original mod assets are not redistributed.
