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
    registerVariant(DEFAULT_VARIANT, rec -> new Displaying(rec) {});
    registerVariant("cardDisplay", CardDisplay::new);
    registerVariant("battleLog", BattleLogDisplay::new);
    registerVariant("endBattle", EndBattleDisplay::new);
    // Add more variants here as needed.
  }

  public static void registerVariant(String name, DisplayingSupplier supplier) {
    STATIC_VARIANTS.put(name, supplier);
  }

  private final List<DisplayingRecord> records = new ArrayList<>();
  private final List<Displaying> displayings = new ArrayList<>();
  private final Map<String, Skin> skinCache = new HashMap<>();
  private final Map<String, DisplayingSupplier> instanceVariants = new HashMap<>();

  public DisplayingFactory(Path file) {
    JsonValue root = new JsonReader().parse(Gdx.files.internal(file.toString()));
    JsonValue displayingArray = root.get(DEFAULT_VARIANT);

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

    for (DisplayingRecord rec : records) {
      DisplayingSupplier supplier = resolveVariant(rec.variant());
      if (supplier == null) {
        Gdx.app.error(
            "DisplayingFactory",
            "Unknown displaying variant \"" + rec.variant() + "\", falling back to default");
        supplier = STATIC_VARIANTS.get(DEFAULT_VARIANT);
      }

      Displaying displaying = supplier.create(rec);
      // Factory-managed, not registered as an entity component: it shares this factory's entity so
      // its event listeners still work, and this factory owns its create/dispose lifecycle (the
      // entity has already snapshotted its component list by the time this runs). Mirrors
      // ClickableFactory.
      displaying.setEntity(this.entity);
      displaying.create();
      displayings.add(displaying);
    }
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // Each Displaying is a UIComponent and draws itself via the render service.
  }

  @Override
  public void dispose() {
    super.dispose();
    for (Displaying displaying : displayings) {
      displaying.dispose();
    }
    displayings.clear();
  }
}
