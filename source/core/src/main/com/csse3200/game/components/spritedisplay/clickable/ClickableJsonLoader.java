// ClickableJsonLoader.java
package com.csse3200.game.components.spritedisplay.clickable;

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
 * Parses Clickable JSON files into {@link ClickableRecord}s. Pure data loading — no knowledge of
 * how those records get turned into live widgets (that's {@link ClickableFactory}'s job). Split out
 * so each class has one reason to change: this one changes if the JSON schema changes,
 * ClickableFactory changes if the runtime widget lifecycle changes.
 */
final class ClickableJsonLoader {

  private static final String DEFAULT_VARIANT = "Clickable";

  private ClickableJsonLoader() {}

  /**
   * Parses a Clickable JSON file into records without constructing any components. Useful when you
   * want to merge JSON-defined buttons with records built programmatically elsewhere (e.g.
   * CardService-driven cards) before handing the combined list to a single ClickableFactory.
   */
  static List<ClickableRecord> loadRecordsFromJson(Path file) {
    List<ClickableRecord> records = new ArrayList<>();
    Map<String, Skin> skinCache = new HashMap<>();

    JsonValue root = new JsonReader().parse(Gdx.files.internal(file.toString()));
    JsonValue clickableArray = root.get(DEFAULT_VARIANT);

    for (JsonValue entry : clickableArray) {
      Skin skin =
          getOrLoadSkin(
              skinCache, entry.getString("skinFile", null), entry.getString("skinAtlas", null));

      ClickableRecord.Builder b =
          ClickableRecord.builder(entry.getString("trigger"))
              .text(entry.getString("text", null))
              .skin(skin)
              .position(entry.getFloat("x"), entry.getFloat("y"))
              .styleName(entry.getString("styleName", null))
              .variant(entry.getString("variant", DEFAULT_VARIANT))
              .label(entry.getString("label", null));

      JsonValue sizeArray = entry.get("size");
      if (sizeArray != null) {
        b.size(sizeArray.getFloat(0), sizeArray.getFloat(1));
      }

      JsonValue argsArray = entry.get("args");
      if (argsArray != null) {
        b.args(jsonArrayToObjects(argsArray));
      }

      records.add(b.build());
    }

    return records;
  }

  /** Converts a JsonValue array of mixed primitives (number/string/boolean) into an Object[]. */
  private static Object[] jsonArrayToObjects(JsonValue array) {
    Object[] result = new Object[array.size];
    int i = 0;
    for (JsonValue child = array.child; child != null; child = child.next) {
      if (child.isNumber()) {
        // Whole numbers become Integer (most game values — damage, heal amounts — are ints);
        // anything with a fractional part becomes Double.
        double value = child.asDouble();
        result[i] = (value == Math.rint(value)) ? (Object) (int) value : (Object) value;
      } else if (child.isBoolean()) {
        result[i] = child.asBoolean();
      } else {
        result[i] = child.asString();
      }
      i++;
    }
    return result;
  }

  private static Skin getOrLoadSkin(
      Map<String, Skin> skinCache, String skinFile, String skinAtlas) {
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
}
