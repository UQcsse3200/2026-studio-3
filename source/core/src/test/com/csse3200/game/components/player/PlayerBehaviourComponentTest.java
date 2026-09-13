package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PlayerBehaviourComponentTest {
  @Test
  void shouldDamageTargetOnAttack() {
    Entity playerEntity = new Entity();
    Entity enemyEntity = new Entity();
    CombatStatsComponent playerStats = new CombatStatsComponent(100, 5, 100);
    CombatStatsComponent enemyStats = new CombatStatsComponent(50, 5, 100);
    PlayerBehaviourComponent behaviour = new PlayerBehaviourComponent();
    playerEntity.addComponent(playerStats);
    playerEntity.addComponent(behaviour);
    enemyEntity.addComponent(enemyStats);
    playerEntity.create();
    enemyEntity.create();
    behaviour.attack(enemyEntity, 5);
    assertEquals(40, enemyStats.getHealth()); // 5 card damage + 5 base attack
  }

  @Test
  void shouldApplyVulnerableModifier() {
    Entity playerEntity = new Entity();
    Entity enemyEntity = new Entity();
    CombatStatsComponent playerStats = new CombatStatsComponent(100, 5, 100);
    CombatStatsComponent enemyStats = new CombatStatsComponent(50, 5, 100);
    PlayerBehaviourComponent behaviour = new PlayerBehaviourComponent();
    playerEntity.addComponent(playerStats);
    playerEntity.addComponent(behaviour);
    enemyEntity.addComponent(enemyStats);
    playerEntity.create();
    enemyEntity.create();
    enemyStats.applyStatusEffect("VULNERABLE", 1, 2);
    behaviour.attack(enemyEntity, 5);
    // raw = 5 + 5 baseAttack = 10, × 1.0 × 1.5 = 15
    assertEquals(35, enemyStats.getHealth()); // 50 - 15
  }

  @Test
  void shouldApplyFeebleModifier() {
    Entity playerEntity = new Entity();
    Entity enemyEntity = new Entity();
    CombatStatsComponent playerStats = new CombatStatsComponent(100, 5, 100);
    CombatStatsComponent enemyStats = new CombatStatsComponent(50, 5, 100);
    PlayerBehaviourComponent behaviour = new PlayerBehaviourComponent();
    playerEntity.addComponent(playerStats);
    playerEntity.addComponent(behaviour);
    enemyEntity.addComponent(enemyStats);
    playerEntity.create();
    enemyEntity.create();
    playerStats.applyStatusEffect("FEEBLE", 1, 2);
    behaviour.attack(enemyEntity, 5);
    // raw = 10, × 0.75 × 1.0 = 7.5 → Math.round → 8
    assertEquals(42, enemyStats.getHealth()); // 50 - 8
  }

  @Test
  void shouldAddArmorOnDefend() {
    Entity playerEntity = new Entity();
    CombatStatsComponent playerStats = new CombatStatsComponent(100, 5, 100);
    PlayerBehaviourComponent behaviour = new PlayerBehaviourComponent();
    playerEntity.addComponent(playerStats);
    playerEntity.addComponent(behaviour);
    playerEntity.create();
    behaviour.defend(5);
    assertEquals(5, playerStats.getArmor());
  }
}
