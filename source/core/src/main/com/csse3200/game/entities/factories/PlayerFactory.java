package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatsDisplay;
import com.csse3200.game.components.battle.FloatingDamageDisplay;
import com.csse3200.game.components.enemy.memory.EnemyMemoryComponent;
import com.csse3200.game.components.enemy.memory.PlayerTrackerComponent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.components.player.PlayerAnimationController;
import com.csse3200.game.components.player.PlayerBehaviourComponent;
import com.csse3200.game.components.player.PlayerEnergyDisplay;
import com.csse3200.game.components.player.PlayerStatsDisplay;
import com.csse3200.game.components.player.PlayerStatsTopDisplay;
import com.csse3200.game.components.status.StatusIconDisplay;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.PlayerConfig;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsUtils;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Predefined player properties are loaded from the player configuration file. */
public class PlayerFactory {
  private static final PlayerConfig stats =
      FileLoader.readClass(PlayerConfig.class, "configs/player.json");

  private static final String PLAYER_ATLAS = "images/player/player.atlas";

  private static final float IDLE_FRAME_DURATION = 0.12f;
  private static final float HURT_FRAME_DURATION = 0.08f;
  private static final float ATTACK_FRAME_DURATION = 0.06f;
  private static final float PLAYER_RENDER_SCALE = 2f;

  private PlayerFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }

  public static int getDefaultHealth() {
    return stats.health;
  }

  public static int getDefaultMaxHealth() {
    return stats.maxHealth;
  }

  public static int getDefaultMaxEnergy() {
    return stats.maxEnergy;
  }

  /**
   * Creates a player entity using the standard texture renderer.
   *
   * @param runState current run state
   * @return the assembled player entity
   */
  public static Entity createPlayer(RunState runState) {
    InputComponent inputComponent =
        ServiceLocator.getInputService().getInputFactory().createForPlayer();

    Entity player =
        new Entity()
            .addComponent(new TextureRenderComponent("images/star_player.png"))
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(new PlayerActions())
            .addComponent(new CombatStatsComponent(stats.health, stats.baseAttack, stats.maxHealth))
            .addComponent(new InventoryComponent(stats.gold))
            .addComponent(new PlayerBehaviourComponent())
            .addComponent(inputComponent)
            .addComponent(new EnergyComponent(resolveMaxEnergy(runState)))
            .addComponent(new StatsDisplay())
            .addComponent(new PlayerStatsTopDisplay(runState))
            .addComponent(new PlayerEnergyDisplay())
            .addComponent(new EnemyMemoryComponent())
            .addComponent(new PlayerTrackerComponent());

    if (ServiceLocator.getRenderService() != null) {
      player.addComponent(new FloatingDamageDisplay());
    }

    runState.getOrCreatePlayerState().applyTo(player);

    PhysicsUtils.setScaledCollider(player, 0.6f, 0.3f);
    player.getComponent(ColliderComponent.class).setDensity(1.5f);
    player.getComponent(TextureRenderComponent.class).scaleEntity();
    return player;
  }

  /**
   * Creates an animated player entity with the specified current health.
   *
   * @param currentHealth current player health
   * @param runState current run state
   * @return the assembled player entity
   */
  public static Entity createPlayer(int currentHealth, RunState runState) {
    InputComponent inputComponent =
        ServiceLocator.getInputService().getInputFactory().createForPlayer();

    TextureAtlas atlas =
        ServiceLocator.getResourceService().getAsset(PLAYER_ATLAS, TextureAtlas.class);

    AnimationRenderComponent animator = new AnimationRenderComponent(atlas);
    animator.addAnimation("idle", IDLE_FRAME_DURATION, Animation.PlayMode.LOOP);
    animator.addAnimation("hurt", HURT_FRAME_DURATION, Animation.PlayMode.NORMAL);
    animator.addAnimation("attack", ATTACK_FRAME_DURATION, Animation.PlayMode.NORMAL);

    Entity player =
        new Entity()
            .addComponent(animator)
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(new PlayerActions())
            .addComponent(
                new CombatStatsComponent(currentHealth, stats.baseAttack, stats.maxHealth))
            .addComponent(new InventoryComponent(stats.gold))
            .addComponent(new PlayerBehaviourComponent())
            .addComponent(inputComponent)
            .addComponent(new EnergyComponent(resolveMaxEnergy(runState)))
            .addComponent(new StatsDisplay())
            // Status icons sit just under the health bar, which is anchored 1.25 units below.
            .addComponent(new StatusIconDisplay(-1.25f, false))
            .addComponent(new PlayerStatsTopDisplay(runState))
            .addComponent(new PlayerEnergyDisplay())
            .addComponent(new EnemyMemoryComponent())
            .addComponent(new PlayerTrackerComponent())
            .addComponent(new PlayerAnimationController());

    if (ServiceLocator.getRenderService() != null) {
      player.addComponent(new FloatingDamageDisplay());
    }

    runState.getOrCreatePlayerState().applyTo(player);

    PhysicsUtils.setScaledCollider(player, 0.6f, 0.3f);
    player.getComponent(ColliderComponent.class).setDensity(1.5f);
    animator.scaleEntity();
    player.setScale(player.getScale().scl(PLAYER_RENDER_SCALE));
    return player;
  }

  /**
   * Creates the initial durable player state from the same config used by player creation.
   *
   * @return default player state for a new run
   */
  public static PlayerRunState createInitialRunState() {
    return new PlayerRunState(stats.health, stats.maxHealth, stats.gold);
  }

  private static int resolveMaxEnergy(RunState runState) {
    if (runState == null || runState.getPlayerMaxEnergy() <= 0) {
      return stats.maxEnergy;
    }

    return runState.getPlayerMaxEnergy();
  }
}
