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
| `intents/` | Smooth source files for the status effect intent icons (128×128); the game uses pixel-art versions |
| `enemy_descriptions.json` | Bestiary backstory copy, agreed with Team 6 against the *Sprint 3 Story Outline* |
| `tools/` | Scripts used to produce the frames and sheets |

## How the frames were made

Follows the wiki page *Enemy Art Asset Conventions*.

Smooth frames (`frames/`):

- `idle_0` and `attack_0` come from the source images: background, watermark and floor shadow removed,
  scaled, and placed so the feet sit on row 478 of the frame (the same ground line as `tomb_guardian`).
  Floating enemies sit above that line.
- `void_knight` was brightened with `tools/Brighten.java` (gamma 0.5, rim 0.55) so it reads against the
  dark dungeon background.
- `tomb_guardian` was redrawn (ivory stone armour, bronze trim, rune shield). Its source images have a
  light grey background and a floor shadow, so `tools/Cutout.java` was run with `--grey-sat` (drops grey
  shadow and haze) and, for the idle image only, `--floor-y` (treats darker grey below the feet as
  background while stopping at the boots' outline).

Pixel-art frames (`frames_pixel/`):

- `idle_0` and `attack_0` are pixelated from the smooth frames with `tools/Pixelate.java` on a 4 px grid
  with a 32-colour palette shared by both frames. Small bright details (eyes, orbs, glows) keep their
  colour, and a dark outline is drawn on the inside of the silhouette.
- `idle_1`, `hurt_0` and `death_0` are derived from the pixel `idle_0` with `tools/DerivePixel.java`,
  moving whole grid cells so the pixel grid is never broken:
  - `idle_1`: upper body pressed down one cell (standing) or lifted two cells (floating)
  - `hurt_0`: tinted red and shifted one cell away from the player
  - `death_0`: desaturated, darkened and squashed onto the ground line

## Rebuilding

The tools are plain Java that only use the JDK, so each one runs as a single source file with
`java <Tool>.java` (JDK 11+, no compile step). Run from `assets/enemies/`.

```bash
java tools/Pixelate.java 4 32 <enemy_id>/frames_pixel <enemy_id>/frames/<enemy_id>_idle_0.png <enemy_id>/frames/<enemy_id>_attack_0.png
java tools/DerivePixel.java 4 <enemy_id>/frames_pixel <enemy_id>
java tools/PackEnemy.java <enemy_id> <enemy_id>/frames_pixel ../../source/core/assets/images/enemies --nearest
```

Add `--floating` to `DerivePixel.java` for enemies that hover (`lesser_shade`).

The tools were ported from earlier Python/Pillow scripts and give pixel-identical output to them. Only
`tomb_guardian`'s committed `idle_0`/`attack_0` match what `Pixelate.java` makes from the current smooth
frames; for `bone_crawler`, `dark_acolyte`, `lesser_shade` and `void_knight`, re-running it changes
roughly 100–500 cells per frame, so check the result before replacing those frames.

Other tools:

- `java tools/Brighten.java <gamma> <rim_strength> <out_dir> <frame.png>...` lightens dark frames.
- `java tools/Cutout.java <in.png> <out.png> [--grey-sat F] [--floor-y N] [--watermark x0,y0,x1,y1|none]`
  removes the white or light grey background from a generated image. The JDK cannot read `.webp`, so
  save the source as PNG first.
- `java tools/Pixelate.java --flatten <pixel_size> <colors> <out_dir> <frame.png>...` merges small
  colour noise in already pixelated frames into flat colour blocks (experimental, not used in game).

`PackEnemy.java` writes `<enemy_id>.png` (2048×1024) and `<enemy_id>.atlas` with the regions `default`,
`idle` (×2), `attack`, `hurt` and `death`. `--nearest` sets the atlas filter to `Nearest` so pixels stay
sharp when the game scales the sprite. Region names must not change: `EnemyAnimationController` looks
animations up by name and fails silently on a mismatch.

Status effect icons (`silence`, `taunt`, `damage_on_card_play`) are pixelated straight into the game
folder with `tools/PixelateIcon.java`: 4 px grid (32×32 cells), a 5-colour palette per icon and a
uniform one-cell white outline. White separators between shapes inside an icon are kept. It only uses
the JDK, so it runs as a single source file (JDK 11+):

```bash
java tools/PixelateIcon.java 4 5 ../../source/core/assets/images/enemies/intents intents/silence.png intents/taunt.png intents/damage_on_card_play.png
```

`tools/AtlasCheck.java` parses an atlas with libGDX's own `TextureAtlasData` to confirm every region
is found. Compile and run it against the `gdx` jar from the Gradle cache.
