package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData;
import com.csse3200.game.extensions.GameExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Guards the animation region names from #438: Tianyu's form-switching code (#436) reads these
 * regions by name, and the bestiary reads the default region.
 *
 * <p>Only parses the .atlas text and loads no textures, so no GPU is needed.
 */
@ExtendWith(GameExtension.class)
class EnemyAtlasRegionsTest {

  private static TextureAtlasData atlas(String id) {
    FileHandle file = Gdx.files.internal("images/enemies/" + id + ".atlas");
    return new TextureAtlasData(file, file.parent(), false);
  }

  /**
   * How many times each region name appears in the atlas (a name with several frames counts once
   * per frame).
   */
  private static Map<String, Integer> regionCounts(String id) {
    Map<String, Integer> counts = new HashMap<>();
    for (TextureAtlasData.Region region : atlas(id).getRegions()) {
      counts.merge(region.name, 1, Integer::sum);
    }
    return counts;
  }

  @Test
  void normalEnemiesHaveADefendPose() {
    for (String id : List.of("lesser_shade", "bone_crawler", "dark_acolyte")) {
      assertEquals(1, regionCounts(id).get("defend"), id);
    }
  }

  @Test
  void tombGuardianHasDefendCastAndTauntingForm() {
    Map<String, Integer> counts = regionCounts("tomb_guardian");

    assertEquals(1, counts.get("defend"));
    assertEquals(1, counts.get("cast"));
    assertEquals(2, counts.get("idle_taunt"));
    assertEquals(1, counts.get("hurt_taunt"));
  }

  @Test
  void rivenKnightHasEnragedForm() {
    Map<String, Integer> counts = regionCounts("void_knight");

    assertEquals(2, counts.get("idle_enraged"));
    assertEquals(1, counts.get("attack_enraged"));
    assertEquals(1, counts.get("hurt_enraged"));
  }

  @Test
  void bossHasSilenceForm() {
    assertEquals(2, regionCounts("boss_knight").get("idle_silence"));
  }

  @Test
  void defaultRegionIsStillTheFirstIdleFrame() {
    // The bestiary shows the default region; new animations must not move it, and it must be the
    // same region as idle frame 0
    for (String id :
        List.of("lesser_shade", "bone_crawler", "dark_acolyte", "tomb_guardian", "void_knight")) {
      TextureAtlasData.Region defaultRegion = null;
      TextureAtlasData.Region firstIdle = null;
      for (TextureAtlasData.Region region : atlas(id).getRegions()) {
        if (region.name.equals("default")) {
          defaultRegion = region;
        } else if (region.name.equals("idle") && region.index == 0) {
          firstIdle = region;
        }
      }
      assertNotNull(defaultRegion, id);
      assertNotNull(firstIdle, id);
      assertEquals(0, defaultRegion.left, id);
      assertEquals(0, defaultRegion.top, id);
      assertEquals(firstIdle.left, defaultRegion.left, id);
      assertEquals(firstIdle.top, defaultRegion.top, id);
    }
  }
}
