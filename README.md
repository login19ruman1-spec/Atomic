# BeforeTheFall — Paper port (Minecraft 1.21.4)

Native server-side reimplementation foundation for Paper 1.21.4 / Java 21. This is not a Forge binary conversion.

## Build

Requirements: Java 21 and Gradle 8.10+.

```bash
gradle clean test build
```

Output: `build/libs/BeforeTheFall-Paper-0.1.0-paper.jar`.

## Implemented in this initial migration slice

- Persistent player radiation values stored in plugin configuration.
- Configurable radiation zones, exposure and decay.
- Radiation sickness stages and potion effects.
- Basic armor-based radiation mitigation (placeholder heuristic, not original suit logic).
- Toggleable ash storm with particles, optional slowness and configurable damage.
- `/beforethefall` (`/bft`) status, ashstorm, radiation, zone and reload commands.
- GitHub Actions build for Java 21; JAR uploaded as a workflow artifact.

## Migration scope and limitations

The supplied Forge JAR contains a large code/resource surface. Forge mods are not binary-compatible with Paper. This project is a clean-room native server plugin implementation; it does not bundle the original JAR, source code, or assets. The initial slice is not a claim that all original functionality has been ported.

Further work is needed for original blocks and block entities, machines/energy/wiring, items/equipment, entities/AI, world generation, structures, recipes/loot, faction/base systems, radios/audio and other gameplay systems. Client GUI, rendering, animations, mixins and client networking cannot be reproduced by a Paper-only plugin; those require a resource pack and/or client-side companion mod. Add third-party assets only when you have permission to redistribute them.

The input mod metadata identifies `Before the Fall`, version `0.1.3-fixed`, author `Orkemey`, with `All Rights Reserved`.

## Commands

```text
/bft
/bft status
/bft ashstorm [on|off]
/bft radiation <player> <0-100>
/bft zone
/bft reload
```

Permission: `beforethefall.admin` (OP by default).

Radiation zones are configured under `radiation.zones` in `config.yml`.
