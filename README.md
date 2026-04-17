# Hallow

`Hallow` is a client-side Fabric cheat mod for Minecraft `1.21.11`.

This is a full cheat client mod with vision, movement, awareness, camera, inventory, protection, bundled resource-pack features, and a redesigned clickable `F7` control deck built around quick in-game controls.

## Current Feature Set

### Vision

- `Fullbright` using the same render path as night vision
- `X-Ray` ships with a bundled managed resource pack, supports an optional override zip, and keeps `ESP`, `Spectator View`, and `Ore Mode` as fallback behavior if pack activation fails
- `NoRender` for fog, overlays, bobbing, and damage tilt removal
- `Player ESP` with tracers and player boxes

### Movement

- `Fly`
- `AutoSprint`
- `StepAssist`
- `SwimAssist`
- `SafeWalk`
- `NoSlow`
- `NoPush`
- `NoWeb`
- `AutoTool`

### Awareness

- `Loot Compass`
- `Threat Radar`
- `Projectile Predict`
- `Minimap`

### Camera / Utility

- Searchable multi-section `F7` control deck with richer cards, badges, and profile context
- Saved camera points
- Player camera browse / lock / follow
- Live target hand copy
- `Anchor Pulse`
- `HallowInv`
- `Chest Stealer`

### Protection

- Invulnerability toggle support
- Fall damage blocking / no-fall spoofing
- Fire, freeze, drowning, and PvP damage blocking
- Keep-inventory support

## Controls

### Cheat Toggles

Hold `F6` and press. `F6` is shortcut-only now and does not open the menu:

- `1`: `Fullbright`
- `2`: `Fly`
- `3`: `X-Ray`
- `4`: `Loot Compass`
- `5`: `Threat Radar`
- `6`: `AutoSprint`
- `7`: `StepAssist`
- `8`: `SwimAssist`
- `9`: `HallowInv`
- `0`: `Anchor Pulse`
- `-`: `Projectile Predict`
- `=`: `NoRender`
- `[`: `SafeWalk`
- `]`: `NoSlow`
- `\`: `NoPush`
- `P`: `Player ESP`
- `T`: `AutoTool`
- `C`: `Chest Stealer`
- `W`: `NoWeb`

### Other Shortcuts

- `F7`: open or close the Hallow menu
- `V`: open `HallowInv`
- `,`: toggle minimap

### Camera Controls

- `B`: save the current camera point
- `Shift + B`: clear saved camera points for the current dimension
- `N`: cycle saved camera points
- `M`: browse player cameras
- Mouse wheel while browsing: change selected player
- `L`: lock the camera to the selected player point / return from locked view
- `K`: live follow the selected player / return from follow view
- `H`: copy the selected live target's main hand into your held slot
- `J`: copy the selected live target's offhand into your offhand slot

## Config

If `Mod Menu` is installed, `Hallow` exposes a config screen from the mod list.

The config surface now uses wider card lanes and clearer helper panels so navigation is faster on both compact and wide layouts.

Runtime config is stored in:

- `.hallow/config.json`

Per-world / per-server state is stored in:

- `.hallow/profiles/`

That includes enabled module state, saved camera points, minimap visibility, and anchor state.

Hallow now bundles `hallow_xray_pack.zip` directly inside the mod jar. When `X-Ray` is enabled, Hallow copies that built-in pack into the active game's `resourcepacks` directory and toggles it on automatically.

If you want to swap in a different pack, the Vision page still exposes an optional override path. When that override is blank, invalid, or missing, Hallow falls back to the built-in pack instead of depending on a file in `Downloads`.

## Notes

- Most features are client-side cheats. Some inventory and protection behavior has better sync in singleplayer or on Hallow-enabled servers.
- Holding `F6` captures keyboard and mouse input so the shortcut layer can use arbitrary keys without triggering normal controls.
- The `F7` menu is a real clickable screen, so it releases the mouse, blocks player controls while open, and keeps its wrapped sections inside the viewport with scrolling when needed.
- `HallowInv` supports live client inventory editing, with extra sync support where the matching server payload exists.
- `Projectile Predict` renders a translucent arc with landing marker instead of a simple point trail.
- Player camera browsing is detached from your movement entity, so you can still move while looking through another player's view.

## Build

Use the Gradle wrapper:

```powershell
.\gradlew.bat build
```

The built jars are written to `build\libs`.

## Releases

The repository includes a GitHub Actions workflow that builds the mod and publishes release assets from pushes to `main`.

## Missing / Limits

- No automated gameplay test coverage yet.
- This is still a Fabric mod, not an external injector or packet spam client.
- Behavior on strict anticheat servers will depend on what the server actually accepts.
