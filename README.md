# Hallow

`Hallow` is a client-side Fabric mod scaffold for permissive servers or private testing worlds. It ships with a small keybind-driven cheat module system and a first batch of features:

- Classic cheats:
  - `Fullbright`
  - `Fly`
  - `X-Ray`
- Original utility cheats:
  - `Loot Compass`
  - `Threat Radar`
  - `Anchor Pulse`

## Mod Menu Config

If you install Mod Menu, `Hallow` exposes a full config screen from the mod's `Configure` button. Settings are also persisted in `config/hallow.json`.

Configurable options include:

- `Fullbright`: auto-enable and gamma value
- `Fly`: auto-enable and flight speed
- `X-Ray`: auto-enable, mode, scan radii, target cap, spectator peek distance, spectator push depth
- `Loot Compass`: auto-enable, search radius, scan interval
- `Threat Radar`: auto-enable, search radius, scan interval, blindside threshold
- `Anchor Pulse`: anchor persistence, exact coordinate HUD output
- HUD offsets for the overlay itself

### X-Ray Modes

- `ESP`: scans nearby ore blocks and draws colored boxes through terrain
- `Spectator View`: pushes the camera into the block you're looking at to give the inside-a-block spectator peek effect
- `Ore Mode`: hides normal block geometry and leaves ores plus water/lava visible

## Default Binds

- `R`: Fullbright
- `G`: Fly
- `X`: X-Ray
- `C`: Loot Compass
- `V`: Threat Radar
- `H`: Anchor Pulse
- `Shift + H`: Clear Anchor Pulse

All binds can be changed in `Options -> Controls -> Key Binds -> Hallow Cheats`.

## Notes

- `Fly` is implemented as forced client-side creative flight. It works best where the server/plugin explicitly allows it.
- `X-Ray` now supports `ESP`, `Spectator View`, and `Ore Mode`.
- `Loot Compass`, `Threat Radar`, and `Anchor Pulse` are HUD-focused helpers, so they remain useful on strict servers too.

## Build

Use the Gradle wrapper:

```powershell
.\gradlew.bat build
```

The built jar will be placed in `build\libs`.

## Still Missing

- No automated tests or sandbox build verification yet.
- The mod does not include packet-level cheats; everything here is client-side only.
- `X-Ray` currently targets common ores and ancient debris; if you want custom block lists, add them in `XRayModule`.
