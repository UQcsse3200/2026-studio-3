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
 * 守住 #438 的动画区域命名：Tianyu 的形态切换代码（#436）按名字读这些区域，图鉴读 default 区域。
 *
 * <p>只解析 .atlas 文本，不加载贴图，所以不需要 GPU。
 */
@ExtendWith(GameExtension.class)
class EnemyAtlasRegionsTest {

  private static TextureAtlasData atlas(String id) {
    FileHandle file = Gdx.files.internal("images/enemies/" + id + ".atlas");
    return new TextureAtlasData(file, file.parent(), false);
  }

  /** 每个区域名在图集里出现的次数（同名多帧算多次）。 */
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
  void defaultRegionIsStillTheFirstIdleFrame() {
    // 图鉴显示 default 区域；新动画不能挪动它，它要和 idle 第 0 帧是同一块
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
