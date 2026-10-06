package com.csse3200.game.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlayerSaveDataTest {
  @Test
  void shouldReadLegacyAliasOnlyWhenLevelIsAbsent() {
    Map<String, Integer> cases =
        Map.of(
            "{}", 0,
            "{\"piety\":8}", 8,
            "{\"piety\":0}", 0,
            "{\"level\":3}", 3,
            "{\"piety\":8,\"level\":3}", 3,
            "{\"level\":3,\"piety\":8}", 3,
            "{\"piety\":8,\"level\":0}", 0,
            "{\"level\":0,\"piety\":8}", 0);

    for (Map.Entry<String, Integer> entry : cases.entrySet()) {
      PlayerSaveData data = new Json().fromJson(PlayerSaveData.class, entry.getKey());

      assertEquals(entry.getValue().intValue(), data.level, entry.getKey());
    }
  }

  @Test
  void shouldWriteOnlyCurrentFieldsAndPreserveAllValues() {
    Json json = new Json();
    PlayerSaveData original =
        new PlayerSaveData(43, 60, 120, 8, List.of("LUCKY_COIN", "ENERGY_CRYSTAL"));

    String serialised = json.toJson(original);
    JsonValue fields = new JsonReader().parse(serialised);
    PlayerSaveData restored = json.fromJson(PlayerSaveData.class, serialised);

    assertEquals(5, fields.size);
    assertEquals(8, fields.getInt("level"));
    assertFalse(fields.has("piety"));
    assertEquals(original.currentHealth, restored.currentHealth);
    assertEquals(original.maxHealth, restored.maxHealth);
    assertEquals(original.gold, restored.gold);
    assertEquals(original.level, restored.level);
    assertEquals(original.ownedItems, restored.ownedItems);
  }
}
