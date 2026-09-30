# Enemy art working files (Sprint 3, issue #279)

Raw material behind the restyled enemy sprites. Nothing here is loaded by the game; the packed
sheets live in `source/core/assets/images/enemies/`.

The sheets in the game are the **pixel-art** version (`frames_pixel/`). The smooth version (`frames/`)
is kept as the source the pixel art is generated from.

## Layout

| Path | Contents |
|---|---|
| `<enemy>/source/` | Original generated images (`idle`, `attack`) and, where kept, a full-resolution transparent cutout |
| `<enemy>/frames/` | Smooth 512×512 frames: `idle_0`, `idle_1`, `attack_0`, `hurt_0`, `death_0` |
| `<enemy>/frames_pixel/` | Pixel-art 512×512 frames used in game (4 px grid) |
| `intents/` | Source files for the status effect intent icons (128×128) |
| `enemy_descriptions.json` | Bestiary backstory copy, agreed with Team 6 against the *Sprint 3 Story Outline* |
| `tools/` | Scripts used to produce the frames and sheets |

## How the frames were made

Follows the wiki page *Enemy Art Asset Conventions*.

Smooth frames (`frames/`):

- `idle_0` and `attack_0` come from the source images: background, watermark and floor shadow removed,
  scaled, and placed so the feet sit on row 478 of the frame (the same ground line as `tomb_guardian`).
  Floating enemies sit above that line.
- `void_knight` was brightened with `tools/brighten.py` (gamma 0.5, rim 0.55) so it reads against the
  dark dungeon background.

Pixel-art frames (`frames_pixel/`):

- `idle_0` and `attack_0` are pixelated from the smooth frames with `tools/pixelate.py` on a 4 px grid
  with a 32-colour palette shared by both frames. Small bright details (eyes, orbs, glows) keep their
  colour, and a dark outline is drawn on the inside of the silhouette.
- `idle_1`, `hurt_0` and `death_0` are derived from the pixel `idle_0` with `tools/derive_pixel.py`,
  moving whole grid cells so the pixel grid is never broken:
  - `idle_1`: upper body pressed down one cell (standing) or lifted two cells (floating)
  - `hurt_0`: tinted red and shifted one cell away from the player
  - `death_0`: desaturated, darkened and squashed onto the ground line

## Rebuilding

Requires Python 3 with Pillow. Run from `assets/enemies/`.

```bash
python tools/pixelate.py 4 32 <enemy_id>/frames_pixel <enemy_id>/frames/<enemy_id>_idle_0.png <enemy_id>/frames/<enemy_id>_attack_0.png
python tools/derive_pixel.py 4 <enemy_id>/frames_pixel <enemy_id>
python tools/pack_enemy.py <enemy_id> <enemy_id>/frames_pixel ../../source/core/assets/images/enemies --nearest
```

Add `--floating` to `derive_pixel.py` for enemies that hover (`lesser_shade`).

`pack_enemy.py` writes `<enemy_id>.png` (2048×1024) and `<enemy_id>.atlas` with the regions `default`,
`idle` (×2), `attack`, `hurt` and `death`. `--nearest` sets the atlas filter to `Nearest` so pixels stay
sharp when the game scales the sprite. Region names must not change: `EnemyAnimationController` looks
animations up by name and fails silently on a mismatch.

`tools/AtlasCheck.java` parses an atlas with libGDX's own `TextureAtlasData` to confirm every region
is found. Compile and run it against the `gdx` jar from the Gradle cache.
