# BeforeTheFall — Atomic Survival (Paper 1.21.4)

Paper plugin for an atomic-disaster survival scenario. Requires **Minecraft/Paper 1.21.4** and **Java 21**. Build system: Gradle 8.10.

> This repository is an existing BeforeTheFall project extended for the Paper 1.21.4 item-model migration. It is not a guarantee that every feature in the original design brief is implemented exactly as described. See **Current implementation notes** below.

## Download

1. Open [Actions](https://github.com/login19ruman1-spec/Atomic/actions) and select the latest successful **Build Paper plugin** run on `paper-1.21.4-release-docs` (or `main`).
2. Download the `BeforeTheFall-Paper-1.21.4` artifact and extract the ZIP.
3. Put `BeforeTheFall-Paper-*.jar` into the server's `plugins/` directory.
4. Start/restart the server. The plugin creates `plugins/BeforeTheFall/config.yml` and requests the embedded resource pack when a player joins. Accept the pack on the client.

When the GitHub Release asset is available, use the JAR and resource-pack ZIP attached to the [latest resource-pack release](https://github.com/login19ruman1-spec/Atomic/releases/tag/resourcepack-latest).

## Server installation

1. Make a backup of the world and the whole server directory.
2. Use **Paper 1.21.4** with **Java 21**. Do not use Spigot, Bukkit-only forks, or a different Minecraft version for this build.
3. Stop the server.
4. Copy the plugin JAR into `plugins/`.
5. Start the server and check the console for `BeforeTheFall Atomic Survival enabled`.
6. Join with a client on Minecraft 1.21.4 and accept the resource-pack prompt. If the pack is declined, custom item visuals and sounds will not appear.
7. For an existing installation, keep the generated `plugins/BeforeTheFall/config.yml` and back it up before replacing the JAR. New defaults do not overwrite an existing config automatically.

The resource pack is embedded in the plugin and is served using the configured URL in `config.yml`. If you change the pack, publish a new release asset and update `resource-pack.url` to a direct HTTPS download URL.

## Quick test checklist

Run these checks on a **test world**, not on the live survival map:

- [ ] Console has no red startup errors and `/plugins` lists `BeforeTheFall`.
- [ ] Accept the resource pack; verify the mask, filter, bomb and dosimeter visuals.
- [ ] Craft a gas mask and equip it in the helmet slot.
- [ ] Craft a replacement filter and right-click while holding it with the mask equipped.
- [ ] Craft and use a dosimeter; its reading should reflect the local radiation.
- [ ] Craft a nuclear charge, place it, then ignite it with flint and steel/fire charge or a redstone signal.
- [ ] Test `/bft status`, `/bft zone`, and admin-only commands as an operator.
- [ ] Test the radiation suit in a disposable test area.
- [ ] Restart the server and confirm saved radiation/configuration state remains available.

For a controlled detonation test, first make a copy of the world. The blast and terrain effects can permanently alter blocks.

## Crafting recipes

All recipes use a **3×3 crafting table**. Each recipe output is a tagged custom item; ordinary vanilla lookalikes are not accepted by the plugin. In the diagrams, `-` means an empty slot.

### Gas mask

Output: **Противогаз** (leather helmet)

```text
L G L
I B I
- L -
```

- `L` — Leather
- `G` — Glass Pane
- `I` — Iron Ingot
- `B` — Iron Bars

Wear it in the helmet slot.

### Replacement filter

Output: **Сменный фильтр**

```text
P C P
R I R
P C P
```

- `P` — Paper
- `C` — Charcoal
- `R` — Redstone
- `I` — Iron Ingot

Alternate recipe (same output), with an Iron Nugget in the centre:

```text
P C P
R N R
P C P
```

- `N` — Iron Nugget

Wear the gas mask and right-click while holding the crafted filter to install it. The configured default filter life is **10 minutes** on this branch.

### Dosimeter

Output: **Дозиметр ДП-5** (clock)

```text
G C G
R I R
- G -
```

- `G` — Glass Pane
- `C` — Clock
- `R` — Redstone
- `I` — Iron Ingot

Right-click to take a reading. Hold it in either hand to display periodic readings in the action bar.

### Nuclear charge

Output: **Ядерный заряд** (TNT)

```text
G R G
T C T
G R G
```

- `G` — Gold Ingot
- `R` — Redstone Block
- `T` — TNT
- `C` — Clock

Place the crafted TNT block and ignite it with flint and steel/fire charge or redstone. Ordinary TNT does not trigger a nuclear detonation.

### Bunker core

Output: **Магнетит — ядро бункера** (lodestone)

```text
O I O
I R I
O I O
```

- `O` — Obsidian
- `I` — Iron Block
- `R` — Redstone Block

Only the crafted/tagged lodestone activates a bunker.

### Yellow radiation suit

Craft each piece separately. `L` = Leather; `Y` = Yellow Dye; `-` = empty slot.

**Helmet**
```text
L Y L
L - L
- - -
```

**Chestplate**
```text
L - L
L Y L
L - L
```

**Leggings**
```text
L Y L
L - L
L - L
```

**Boots**
```text
- - -
L - L
L - L
```

The complete four-piece set is required for the protective-suit check.

## Commands

The command root is `/beforethefall`; aliases are `/bft` and `/bfall`.

- `/bft status` — personal radiation and filter status.
- `/bft ashstorm [on|off]` — toggle ash storm (admin).
- `/bft nuke [power]` — test detonation at your position (admin).
- `/bft radiation <player> <0-100>` — set a player's dose (admin).
- `/bft zone` — show radiation zones.
- `/bft reload` — reload configuration (admin).

Admin permission: `beforethefall.admin` (default: op). Status permission: `beforethefall.status` (default: everyone).

## Build from source

Requirements: Java 21 and Gradle 8.10 (or use the Gradle wrapper if one is added).

```bash
gradle --no-daemon clean test build
```

The build generates the embedded resource pack and packages it into the plugin JAR. GitHub Actions runs the same build on pushes to `main`, `master` and `paper-1.21.4-release-docs`, on pull requests, and on manual dispatch. Successful runs upload the plugin JAR as a 30-day Actions artifact and publish the plugin JAR plus resource-pack ZIP to the `resourcepack-latest` GitHub Release.

## Models, textures and sounds

- Item rendering uses the 1.21.4 item-model definition system and `ItemMeta#setItemModel`; legacy `CustomModelData` overrides are not the intended integration path for the new items.
- Blockbench source models belong under `src/main/resources/atomic-assets/models/item/` or `src/main/resources/blockbench/`. The Gradle resource-pack task exports the embedded Blockbench texture and cube geometry.
- Optional resource-pack textures belong under `src/main/resources/atomic-assets/textures/`.
- Optional OGG files belong under `src/main/resources/atomic-assets/sounds/`. Sound events are emitted only for files present when the pack is generated.
- The three supplied gas-mask overlay PNGs are stored in `src/main/resources/atomic-assets/textures/gui/` and are selected through the helmet's 1.21.4 `EquippableComponent#setCameraOverlay`. The clean, fogged and cracked states therefore work with the server resource pack when the tagged gas mask is equipped.

The pack skins vanilla snow layers as ash while retaining their thin-layer collision and stacking. Vanilla snow layers share the ash texture while the pack is active.

## Blast and aftermath defaults

The branch defaults are tuned to the requested station scale:

- Crater/block destruction radius: **20 blocks**, processed in batches of **300 block edits per tick**.
- Secondary bursts: **8–12** small bursts in the **25–32 block** ring, scheduled with **1–3 tick** delays.
- Main blast sound: up to **500 blocks**; players below Y=60 or with zero sky light receive the muffled layer.
- Shockwave sound: delayed by **30 ticks (1.5 seconds)**, with reduced volume, up to 500 blocks.
- Fallout propagation radius: **1,500 blocks**.
- Water contamination and terrain aftermath radius: **900 blocks**.
- Gas-mask filter: **10 minutes of wall-clock time**, so low TPS does not extend cartridge life.

These are configurable in `plugins/BeforeTheFall/config.yml`. Always test destructive behavior on a copied world first.

## Current implementation notes and limitations

- This repository already contained the BeforeTheFall survival implementation (radiation dose, fallout/water zones, ash storm, bunker, mask/filter, dosimeter, nuclear charge, suit, recipes and commands). This branch migrates custom item visuals to Paper/Minecraft 1.21.4 `ItemModel` definitions and adds the dosimeter crafting recipe.
- The five Blockbench source models are kept in `src/main/resources/blockbench/` (and the model-export input folder). The three visor PNGs are under `src/main/resources/atomic-assets/textures/gui/`; blast OGG assets are under `src/main/resources/atomic-assets/gas-mask/sounds/blast/`. The build exports the models and embedded textures and copies the supplied overlay/sound assets into the resource pack. CI checks the generated pack contents.
- The current gas mask is a plugin-tagged leather helmet with an `ItemModel`, an `EquippableComponent` head slot and camera overlay. Its gameplay durability/filter logic is retained; clean, fogged and cracked item/overlay variants are selected as the condition changes.
- `EquippableComponent` does not turn arbitrary Blockbench free-format geometry into wearable armor geometry. Wearable equipment assets use Minecraft's equipment/armor asset format; arbitrary multi-cube models need a display-entity approach or a client mod.
- The terrain/blast systems are destructive. Test with backups and region protection enabled. Large-radius world edits should be throttled and tested under realistic player/chunk load.
- This project is a native Paper plugin, not a Forge mod or a client-side mod.

## Troubleshooting

**The plugin does not load:** verify Paper 1.21.4, Java 21, and inspect `logs/latest.log` for the first exception.

**Custom textures/models are missing:** confirm the client accepted the pack, the pack ZIP contains the relevant `assets/beforethefall/` files, and item identifiers match the generated item definitions.

**The server cannot download the pack:** verify the `resource-pack.url` is a direct HTTPS ZIP download and that it is publicly reachable without authentication.

**Recipes do not appear:** use a crafting table, place ingredients in the exact pattern, and verify the output is the tagged item rather than a vanilla lookalike.

## License and asset rights

Only redistribute the Blockbench models, textures, and sounds if you own them or have permission to redistribute them. Keep original editable sources alongside generated resources.
