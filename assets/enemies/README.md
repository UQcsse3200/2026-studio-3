# Enemy art working files (Sprint 3, issue #279)

Raw material behind the restyled enemy sprites. Nothing here is loaded by the game; the packed
sheets live in `source/core/assets/images/enemies/`.

## Layout

| Path | Contents |
|---|---|
| `<enemy>/source/` | Original generated images (`idle`, `attack`) and, where kept, a full-resolution transparent cutout |
| `<enemy>/frames/` | Final 512×512 frames: `idle_0`, `idle_1`, `attack_0`, `hurt_0`, `death_0` |
| `intents/` | Source files for the status effect intent icons (128×128) |
| `enemy_descriptions.json` | Bestiary backstory copy, agreed with Team 6 against the *Sprint 3 Story Outline* |
| `tools/` | Scripts used to produce the frames and sheets |

## How the frames were made

Follows the wiki page *Enemy Art Asset Conventions*.

- `idle_0` and `attack_0` come from the source images: background, watermark and floor shadow removed,
  scaled, and placed so the feet sit on row 478 of the frame (the same ground line as `tomb_guardian`).
  Floating enemies sit above that line.
- `idle_1` is derived from `idle_0`: a small stretch from the ground line (standing enemies) or an
  8 px lift (floating enemies).
- `hurt_0` is `idle_0` tinted red and shifted 4 px away from the player.
- `death_0` is `idle_0` desaturated, darkened and collapsed towards the ground line.

## Rebuilding a sheet

Requires Python 3 with Pillow.

```bash
python tools/pack_enemy.py <enemy_id> <enemy_id>/frames ../../source/core/assets/images/enemies
```

This writes `<enemy_id>.png` (2048×1024) and `<enemy_id>.atlas` with the regions `default`, `idle`
(×2), `attack`, `hurt` and `death`. Region names must not change: `EnemyAnimationController` looks
animations up by name and fails silently on a mismatch.

`tools/AtlasCheck.java` parses an atlas with libGDX's own `TextureAtlasData` to confirm every region
is found. Compile and run it against the `gdx` jar from the Gradle cache.
