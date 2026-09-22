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
  private final int progression;
  private final List<String> enemyIds;
  private final Map<String, Entity> enemyTargets = new LinkedHashMap<>();
  private String[] additionalAtlases = new String[0];
  /**
   * Creates a battle area with the default line-up.
   *
   * @param terrainFactory factory used to build the terrain
   * @param progression map progression of the node, used to scale enemy stats
   * @param runState state of the current run
   * @param backgroundId id of the background to show
   */
  public BattleGameArea(
          TerrainFactory terrainFactory, int progression, RunState runState, String backgroundId) {
    this(terrainFactory, progression, runState, backgroundId, EncounterComposer.DEFAULT_ENEMIES);
  }

  /**
   * Creates a battle area for the given line-up.
   *
   * @param terrainFactory factory used to build the terrain
   * @param progression map progression of the node, used to scale enemy stats
   * @param runState state of the current run
   * @param backgroundId id of the background to show
   * @param enemyIds enemy ids to spawn, from left to right
   */
  public BattleGameArea(
          TerrainFactory terrainFactory,
          int progression,
          RunState runState,
          String backgroundId,
          List<String> enemyIds) {
    super(terrainFactory, progression, runState, backgroundId);
    this.progression = progression;
    this.enemyIds = List.copyOf(enemyIds);
  }
  /** This area spawns every enemy itself, so the shared area's default enemy is not wanted. */
  @Override
  protected boolean spawnsDefaultEnemy() {
    return false;
  }
  @Override
  public void create() {
    super.create();

    EnemyConfigs roster = FileLoader.readClass(EnemyConfigs.class, "configs/enemies.json");
    List<EnemyConfig> configs =
            enemyIds.stream()
            .map(id -> EnemyScaling.scale(roster.get(id), progression))
            .toList();
    for (int index = 0; index < configs.size(); index++) {
      EnemyConfig config = configs.get(index);
      if (config.sprite == null || config.sprite.isBlank()) {
        config.sprite = "images/enemies/" + config.id + ".atlas";
      }
    }
    additionalAtlases =
        configs.stream().map(config -> config.sprite).distinct().toArray(String[]::new);
    ResourceService resources = ServiceLocator.getResourceService();
    resources.loadTextureAtlases(additionalAtlases);
    resources.loadAll();
    for (int index = 0; index < configs.size(); index++) {
      EnemyConfig config = configs.get(index);
      Entity enemy = EnemyFactory.create(config);
      spawnEntityAt(enemy, new GridPoint2(20 + index * 7, 20), true, true);
      // Keyed by the entity's numeric ID to match the drop-target ID EnemyFactory assigns it.
      enemyTargets.put(Integer.toString(enemy.getId()), enemy);
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
