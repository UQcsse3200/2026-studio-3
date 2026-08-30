// DisplayingFactory.java
package com.csse3200.game.components.spritedisplay.displaying;

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

public class DisplayingFactory extends UIComponent {

  private static final String DEFAULT_VARIANT = "Displaying";

  private static final Map<String, DisplayingSupplier> STATIC_VARIANTS = new HashMap<>();

  static {
    registerVariant(DEFAULT_VARIANT, record -> new Displaying(record) {});
    registerVariant("health", HealthDisplay::new);
    registerVariant("cardDisplay", CardDisplay::new);
    // Add more variants here as needed
  }

  public static void registerVariant(String name, DisplayingSupplier supplier) {
    STATIC_VARIANTS.put(name, supplier);
  }

  private final List<DisplayingRecord> records = new ArrayList<>();
  private final Map<String, Skin> skinCache = new HashMap<>();
  private final Map<String, DisplayingSupplier> instanceVariants = new HashMap<>();

  public DisplayingFactory(Path file) {
    JsonValue root = new JsonReader().parse(Gdx.files.internal(file.toString()));
    JsonValue displayingArray = root.get("Displaying");

    for (JsonValue entry : displayingArray) {
      Skin skin = getOrLoadSkin(entry.getString("skinFile", null));

      DisplayingRecord.Builder b =
          DisplayingRecord.builder(entry.getString("text"))
              .trigger(entry.getString("trigger", null))
              .skin(skin)
              .position(entry.getFloat("x"), entry.getFloat("y"))
              .fontName(entry.getString("fontName", null))
              .colour(entry.getString("colour", null))
              .scale(entry.getFloat("scale", 1f))
              .variant(entry.getString("variant", DEFAULT_VARIANT));

      JsonValue sizeArray = entry.get("size");
      if (sizeArray != null) {
        b.size(sizeArray.getFloat(0), sizeArray.getFloat(1));
      }

      records.add(b.build());
    }
  }

  public DisplayingFactory(List<DisplayingRecord> records) {
    this.records.addAll(records);
  }

  public void registerInstanceVariant(String name, DisplayingSupplier supplier) {
    instanceVariants.put(name, supplier);
  }

  private DisplayingSupplier resolveVariant(String name) {
    DisplayingSupplier supplier = instanceVariants.get(name);
    if (supplier != null) {
      return supplier;
    }
    return STATIC_VARIANTS.get(name);
  }

  private Skin getOrLoadSkin(String skinFile) {
    if (skinFile == null) {
      return null;
    }
    return skinCache.computeIfAbsent(
        skinFile, key -> new Skin(new TextureAtlas(Gdx.files.internal(skinFile))));
  }

  @Override
  public void create() {
    super.create();

    for (DisplayingRecord record : records) {
      DisplayingSupplier supplier = resolveVariant(record.variant());
      if (supplier == null) {
        Gdx.app.error(
            "DisplayingFactory",
            "Unknown displaying variant \"" + record.variant() + "\", falling back to default");
        supplier = STATIC_VARIANTS.get(DEFAULT_VARIANT);
      }

      Displaying displaying = supplier.create(record);
      this.entity.addComponent(displaying);
    }
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // All drawing is now handled by each individual Displaying component.
    // Nothing to do here!
  }

  @Override
  public void dispose() {
    super.dispose();
    // Components are disposed by the entity system automatically.
  }
}
