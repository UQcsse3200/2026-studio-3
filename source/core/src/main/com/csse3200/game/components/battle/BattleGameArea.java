package com.csse3200.game.components.battle;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.ForestGameArea;
import com.csse3200.game.areas.terrain.TerrainFactory;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.EnemyConfig;
import com.csse3200.game.entities.configs.EnemyConfigs;
import com.csse3200.game.entities.configs.EnemyScaling;
import com.csse3200.game.entities.factories.EnemyFactory;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Adds the battle encounter roster using the shared area's and enemy factory's existing APIs. */
public class BattleGameArea extends ForestGameArea {
  private static final List<String> ADDITIONAL_ENEMIES = List.of("lesser_shade");
  private final int progression;
  private final Map<String, Entity> enemyTargets = new LinkedHashMap<>();
  private String[] additionalAtlases = new String[0];

  public BattleGameArea(
      TerrainFactory terrainFactory, int progression, RunState runState, String backgroundId) {
    super(terrainFactory, progression, runState, backgroundId);
    this.progression = progression;
  }

  @Override
  public void create() {
    super.create();
    // The shared forest area owns its original Bone Crawler and its existing drop-target ID.
    enemyTargets.put("bone_crawler", super.getEnemies().getFirst());

    EnemyConfigs roster = FileLoader.readClass(EnemyConfigs.class, "configs/enemies.json");
    List<EnemyConfig> configs =
        ADDITIONAL_ENEMIES.stream()
            .map(id -> EnemyScaling.scale(roster.get(id), progression))
            .toList();
    for (int index = 0; index < configs.size(); index++) {
      EnemyConfig config = configs.get(index);
      if (config.sprite == null || config.sprite.isBlank()) {
        config.sprite = "images/enemies/" + config.id + ".atlas";
      }
      // The factory accepts an explicit sprite and target ID. Keep instances distinct even when
      // the encounter contains several copies of the same enemy type, without changing the roster.
      config.id = config.id + "_" + (index + 1);
    }
    additionalAtlases =
        configs.stream().map(config -> config.sprite).distinct().toArray(String[]::new);
    ResourceService resources = ServiceLocator.getResourceService();
    resources.loadTextureAtlases(additionalAtlases);
    resources.loadAll();
    for (int index = 0; index < configs.size(); index++) {
      EnemyConfig config = configs.get(index);
      Entity enemy = EnemyFactory.create(config);
      spawnEntityAt(enemy, new GridPoint2(27 + index * 7, 20), true, true);
      enemyTargets.put(config.id, enemy);
    }
  }

  @Override
  public List<Entity> getEnemies() {
    return List.copyOf(enemyTargets.values());
  }

  /** IDs supplied by the existing enemy drop targets, mapped to their encounter instances. */
  public Map<String, Entity> getEnemyTargets() {
    return Map.copyOf(enemyTargets);
  }

  @Override
  public void dispose() {
    super.dispose();
    ServiceLocator.getResourceService().unloadAssets(additionalAtlases);
  }
}
