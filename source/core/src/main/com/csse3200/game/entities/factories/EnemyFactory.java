package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.enemy.EnemyAnimationController;
import com.csse3200.game.components.enemy.EnemyBehaviourComponent;
import com.csse3200.game.components.enemy.EnemyStatsComponent;
import com.csse3200.game.components.enemy.IntentIcons;
import com.csse3200.game.components.spritedisplay.reactive.EnemyDropTargetComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.EnemyConfig;
import com.csse3200.game.entities.configs.EnemyConfigs;
import com.csse3200.game.entities.configs.EnemyScaling;
import com.csse3200.game.entities.configs.EnemyTier;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.DragNDropService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates enemy entities from configuration loaded from {@code configs/enemies.json}.
 *
 * <p>Unknown enemy ids fall back to a default enemy configuration.
 */
public class EnemyFactory {
  private static final Logger logger = LoggerFactory.getLogger(EnemyFactory.class);
  private static final EnemyConfigs roster = loadRoster();

  private static final String SPRITE_DIR = "images/enemies/";
  private static final String DEFAULT_ATLAS = SPRITE_DIR + "default.atlas";
  private static final float IDLE_FRAME_DURATION = 0.5f;
  private static final float HURT_FRAME_DURATION = 0.15f;

  private static EnemyConfigs loadRoster() {
    EnemyConfigs configs = FileLoader.readClass(EnemyConfigs.class, "configs/enemies.json");

    if (configs == null) {
      logger.warn("Failed to load enemy configs, using empty roster");
      return new EnemyConfigs();
    }

    return configs;
  }

  /**
   * Creates an enemy by id, falling back to a default enemy when the id is unknown.
   *
   * @param id enemy id
   * @return the assembled enemy entity
   */
  public static Entity create(String id) {
    return create(resolve(id));
  }

  /**
   * Creates an enemy by id with stats scaled for the given floor.
   *
   * <p>Deeper floors produce tougher enemies through {@link EnemyScaling}. A floor of {@code 0}, or
   * a negative floor, yields the enemy's base stats.
   *
   * @param id enemy id
   * @param floor the current run floor, used as the scaling progression
   * @return the assembled enemy entity
   */
  public static Entity create(String id, int floor) {
    return create(EnemyScaling.scale(resolve(id), floor));
  }

  /**
   * Creates an enemy from the given configuration.
   *
   * @param config enemy configuration
   * @return the assembled enemy entity
   */
  public static Entity create(EnemyConfig config) {
    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService().getAsset(atlasPath(config), TextureAtlas.class));
    animator.addAnimation("idle", IDLE_FRAME_DURATION, Animation.PlayMode.LOOP);
    animator.addAnimation("hurt", HURT_FRAME_DURATION, Animation.PlayMode.NORMAL);

    CombatStatsComponent stats = new CombatStatsComponent(config.health, config.baseAttack);
    stats.setArmor(config.armour);

    Entity enemy =
        new Entity()
            .addComponent(stats)
            .addComponent(new EnemyStatsComponent(config.name))
            .addComponent(new EnemyBehaviourComponent(config.behaviour))
            .addComponent(animator)
            .addComponent(new EnemyAnimationController());

    // The drop target lets the player drag a card onto the enemy. It needs the drag-and-drop UI
    // service and a camera, which are only registered when the battle screen is running, so it is
    // skipped when they are absent (e.g. headless tests) rather than failing enemy creation.
    DragNDropService dragAndDrop = ServiceLocator.getDragAndDropService();
    if (dragAndDrop != null && ServiceLocator.getCamera() != null) {
      enemy.addComponent(
          new EnemyDropTargetComponent(
              dragAndDrop.getDragAndDrop(), ServiceLocator.getCamera(), config.id));
    }

    return enemy;
  }

  /**
   * Resolves the texture atlas path for an enemy.
   *
   * <p>An explicit {@link EnemyConfig#sprite} wins. Otherwise the path is {@code
   * images/enemies/<id>.atlas} by convention, falling back to {@link #DEFAULT_ATLAS} when the id is
   * missing.
   *
   * @param config enemy configuration
   * @return an internal texture atlas path
   */
  private static String atlasPath(EnemyConfig config) {
    if (config.sprite != null && !config.sprite.isBlank()) {
      return config.sprite;
    }
    if (config.id != null && !config.id.isBlank() && !"unknown".equals(config.id)) {
      return SPRITE_DIR + config.id + ".atlas";
    }
    return DEFAULT_ATLAS;
  }

  /**
   * @return the ids of every enemy in the roster
   */
  public static List<String> availableEnemies() {
    return roster.ids();
  }

  /**
   * Returns every texture atlas path the factory may render, so a game area can queue them for
   * loading.
   *
   * <p>Covers the shared default atlas and every roster enemy atlas. The result has no duplicates.
   *
   * @return the internal atlas paths used by enemies
   */
  public static String[] getAtlasPaths() {
    Set<String> paths = new LinkedHashSet<>();
    paths.add(DEFAULT_ATLAS);

    for (String id : roster.ids()) {
      paths.add(atlasPath(roster.get(id)));
    }

    return paths.toArray(new String[0]);
  }

  /**
   * Queues every enemy atlas and intent icon with the resource service. The caller is responsible
   * for pumping the load (e.g. {@code ResourceService.loadForMillis}) and for calling {@link
   * #unloadAssets()} on teardown.
   */
  public static void loadAssets() {
    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.loadTextureAtlases(getAtlasPaths());
    resourceService.loadTextures(IntentIcons.all());
  }

  /** Releases every enemy atlas and intent icon previously queued by {@link #loadAssets()}. */
  public static void unloadAssets() {
    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.unloadAssets(getAtlasPaths());
    resourceService.unloadAssets(IntentIcons.all());
  }

  /**
   * Returns the ids of every roster enemy belonging to the given tier.
   *
   * <p>Used by level design to pick enemies of a particular difficulty. Returns an empty list when
   * nothing matches or the roster failed to load.
   *
   * @param tier the tier to filter by
   * @return a new list of matching enemy ids
   */
  public static List<String> getIdsByTier(EnemyTier tier) {
    List<String> matches = new ArrayList<>();

    for (String id : roster.ids()) {
      EnemyConfig config = roster.get(id);
      if (config != null && config.tier == tier) {
        matches.add(id);
      }
    }

    return matches;
  }

  /**
   * Looks up a config by id, returning a fresh default config (and logging a warning) when the id
   * is unknown.
   *
   * @param id enemy id
   * @return the matching config, or a default {@link EnemyConfig}
   */
  private static EnemyConfig resolve(String id) {
    EnemyConfig config = roster.get(id);

    if (config == null) {
      logger.warn("Unknown enemy id: {}", id);
      return new EnemyConfig();
    }

    return config;
  }

  private EnemyFactory() {
    throw new IllegalStateException("Instantiating utility class");
  }
}
