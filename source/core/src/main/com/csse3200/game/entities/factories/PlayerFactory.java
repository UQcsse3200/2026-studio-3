// PlayerFactory.java
package com.csse3200.game.entities.factories;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.*;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.PlayerConfig;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsUtils;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Factory to create a player entity.
 *
 * <p>Predefined player properties are loaded from a config stored as a json file and should have
 * the properties stores in 'PlayerConfig'.
 */
public class PlayerFactory {

  public static int getDefaultHealth() {
    return stats.health;
  }

  public static int getDefaultMaxHealth() {
    return stats.maxHealth;
  }

  public static int getDefaultMaxEnergy() {
    return stats.maxEnergy;
  }

  private static final PlayerConfig stats =
      FileLoader.readClass(PlayerConfig.class, "configs/player.json");

  /**
   * Create a player entity.
   *
   * @return entity
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
            .addComponent(new EnergyComponent(stats.maxEnergy))
            .addComponent(new PlayerStatsDisplay())
            .addComponent(new PlayerStatsTopDisplay(runState));

    PhysicsUtils.setScaledCollider(player, 0.6f, 0.3f);
    player.getComponent(ColliderComponent.class).setDensity(1.5f);
    player.getComponent(TextureRenderComponent.class).scaleEntity();
    return player;
  }

  public static Entity createPlayer(int currentHealth, RunState runState) {
    InputComponent inputComponent =
        ServiceLocator.getInputService().getInputFactory().createForPlayer();

    Entity player =
        new Entity()
            .addComponent(new TextureRenderComponent("images/star_player.png"))
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(new PlayerActions())
            .addComponent(
                new CombatStatsComponent(currentHealth, stats.baseAttack, stats.maxHealth))
            .addComponent(new InventoryComponent(stats.gold))
            .addComponent(new PlayerBehaviourComponent())
            .addComponent(inputComponent)
            .addComponent(new EnergyComponent(stats.maxEnergy))
            .addComponent(new PlayerStatsDisplay())
            .addComponent(new PlayerStatsTopDisplay(runState));

    PhysicsUtils.setScaledCollider(player, 0.6f, 0.3f);
    player.getComponent(ColliderComponent.class).setDensity(1.5f);
    player.getComponent(TextureRenderComponent.class).scaleEntity();
    return player;
  }

  private PlayerFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
