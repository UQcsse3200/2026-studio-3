package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses the {@code "Displaying"} array of a sprite JSON file into {@link DisplayingRecord}s. Pure
 * data loading, the counterpart of {@code ClickableJsonLoader}: this class changes if the JSON
 * schema changes, {@link DisplayingFactory} changes if the runtime widget lifecycle changes.
 */
final class DisplayingJsonLoader {

  private static final String DEFAULT_VARIANT = "Displaying";

  private DisplayingJsonLoader() {}

  /**
   * Parses a JSON file into records without constructing any components, loading any {@code
   * skinFile} atlases the entries name. Returns an empty list if the file has no {@code
   * "Displaying"} array.
   */
  static List<DisplayingRecord> loadRecordsFromJson(Path file) {
    List<DisplayingRecord> records = new ArrayList<>();
    Map<String, Skin> skinCache = new HashMap<>();

    // libGDX expects '/' separators; Path.toString() gives '\' on Windows.
    JsonValue root = new JsonReader().parse(Gdx.files.internal(file.toString().replace('\\', '/')));
    JsonValue displayingArray = root.get(DEFAULT_VARIANT);
    if (displayingArray == null) {
      return records;
    }

    for (JsonValue entry : displayingArray) {
      DisplayingRecord.Builder b =
          DisplayingRecord.builder(entry.getString("text"))
              .trigger(entry.getString("trigger", null))
              .skin(getOrLoadSkin(skinCache, entry.getString("skinFile", null)))
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
    return records;
  }

  private static Skin getOrLoadSkin(Map<String, Skin> skinCache, String skinFile) {
    if (skinFile == null) {
      return null;
    }
    return skinCache.computeIfAbsent(
        skinFile, key -> new Skin(new TextureAtlas(Gdx.files.internal(skinFile))));
  }
}
