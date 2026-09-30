package com.csse3200.game.entities.configs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** 检查真实的 configs/enemies.json：每只敌人都有图鉴描述，且描述符合剧情写作规范。 */
@ExtendWith(GameExtension.class)
class EnemyRosterContentTest {
  // wiki《The Fall of the Pantheon》写作规范禁止的旧设定（档案馆版）用词
  private static final List<String> RETIRED_WORDS = List.of("archive", "catalogue", "shelf", "record");

  private EnemyConfigs roster;

  @BeforeEach
  void loadRoster() {
    roster = FileLoader.readClass(EnemyConfigs.class, "configs/enemies.json");
    assertNotNull(roster, "configs/enemies.json failed to parse");
  }

  @Test
  void everyEnemyHasADescription() {
    for (String id : roster.ids()) {
      String description = roster.get(id).description;
      assertFalse(
          description == null || description.isBlank(), "Enemy '" + id + "' has no description");
    }
  }

  @Test
  void descriptionsAvoidRetiredStoryWording() {
    for (String id : roster.ids()) {
      String description = roster.get(id).description.toLowerCase(Locale.ROOT);
      for (String word : RETIRED_WORDS) {
        assertFalse(
            description.contains(word),
            "Enemy '" + id + "' description uses retired story wording '" + word + "'");
      }
    }
  }
}
