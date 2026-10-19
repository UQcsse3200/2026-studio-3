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

/**
 * Checks the real configs/enemies.json: every enemy has a bestiary description, and the
 * descriptions follow the lore writing guidelines.
 */
@ExtendWith(GameExtension.class)
class EnemyRosterContentTest {
  // Old (Archive-version) lore terms banned by the wiki's "The Fall of the Pantheon" guidelines
  private static final List<String> RETIRED_WORDS =
      List.of("archive", "catalogue", "shelf", "record");

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
