// ClickableFactory.java
package com.csse3200.game.components.spritedisplay.clickable;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.csse3200.game.ui.UIComponent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClickableFactory extends UIComponent {

  private static final String DEFAULT_VARIANT = "Clickable";
  private static final Map<String, ClickableSupplier> STATIC_VARIANTS = new HashMap<>();

  static {
    registerVariant(DEFAULT_VARIANT, record -> new Clickable(record) {});
    registerVariant("inout", InOutOnTrigger::new);
    // add future built-in variants here, e.g.:
    // registerVariant("toggle", ToggleClickable::new);
  }

  public static void registerVariant(String name, ClickableSupplier supplier) {
    STATIC_VARIANTS.put(name, supplier);
  }

  private final List<ClickableRecord> records = new ArrayList<>();
  private final List<Clickable> clickables = new ArrayList<>();
  private final Map<String, Skin> skinCache = new HashMap<>();
  private final Map<String, ClickableSupplier> instanceVariants = new HashMap<>();

  public ClickableFactory(Path file) {
    JsonValue root = new JsonReader().parse(Gdx.files.internal(file.toString()));
    JsonValue clickableArray = root.get("Clickable");

    for (JsonValue entry : clickableArray) {
      Skin skin =
          getOrLoadSkin(entry.getString("skinFile", null), entry.getString("skinAtlas", null));

      ClickableRecord.Builder b =
          ClickableRecord.builder(entry.getString("trigger"))
              .text(entry.getString("text", null))
              .skin(skin)
              .position(entry.getFloat("x"), entry.getFloat("y"))
              .styleName(entry.getString("styleName", null))
              .variant(entry.getString("variant", DEFAULT_VARIANT));

      JsonValue sizeArray = entry.get("size");
      if (sizeArray != null) {
        b.size(sizeArray.getFloat(0), sizeArray.getFloat(1));
      }

      records.add(b.build());
    }
  }

  public ClickableFactory(List<ClickableRecord> records) {
    this.records.addAll(records);
  }

  /**
   * Registers a variant for THIS factory instance only, overriding a static variant of the same
   * name if present. Optional — most variants should go in the static block above instead.
   */
  public void registerInstanceVariant(String name, ClickableSupplier supplier) {
    instanceVariants.put(name, supplier);
  }

  private ClickableSupplier resolveVariant(String name) {
    ClickableSupplier supplier = instanceVariants.get(name);
    if (supplier != null) {
      return supplier;
    }
    return STATIC_VARIANTS.get(name);
  }

  private Skin getOrLoadSkin(String skinFile, String skinAtlas) {
    if (skinFile == null && skinAtlas == null) {
      return null;
    }

    String cacheKey = skinFile + "|" + skinAtlas;
    return skinCache.computeIfAbsent(
        cacheKey,
        key -> {
          if (skinFile != null && skinAtlas != null) {
            TextureAtlas atlas = new TextureAtlas(Gdx.files.internal(skinAtlas));
            return new Skin(Gdx.files.internal(skinFile), atlas);
          } else if (skinFile != null) {
            return new Skin(Gdx.files.internal(skinFile));
          } else {
            return new Skin(new TextureAtlas(Gdx.files.internal(skinAtlas)));
          }
        });
  }

  @Override
  public void create() {
    super.create();
    for (ClickableRecord record : records) {
      ClickableSupplier supplier = resolveVariant(record.variant());
      if (supplier == null) {
        Gdx.app.error(
            "ClickableFactory",
            "Unknown clickable variant \"" + record.variant() + "\", falling back to default");
        supplier = STATIC_VARIANTS.get(DEFAULT_VARIANT);
      }

      Clickable clickable = supplier.create(record);
      clickable.setEntity(this.entity);

      clickable.create();

      clickables.add(clickable);
      stage.addActor(clickable.getBtn());
    }
  }

  @Override
  protected void draw(SpriteBatch batch) {
    for (Clickable clickable : clickables) {
      clickable.draw();
    }
  }

  @Override
  public void dispose() {
    super.dispose();
    for (Clickable clickable : clickables) {
      clickable.getBtn().remove();
    }
  }
}
