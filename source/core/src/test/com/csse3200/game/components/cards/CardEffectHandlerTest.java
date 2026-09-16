package com.csse3200.game.components.cards;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.effects.ResolvedCardEffect;
import com.csse3200.game.cards.play.CardPlayRequest;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CardEffectHandlerTest {

  private CardEffectHandler handler;

  @BeforeEach
  void setUp() {
    handler = new CardEffectHandler();
  }

  @Test
  void shouldApplyDamageToEnemy() {
    Entity enemy = new Entity().addComponent(new CombatStatsComponent(10, 2));

    ResolvedCardEffect damage =
        new ResolvedCardEffect("strike", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 6, 0, 0);

    handler.applyEnemyEffects(List.of(enemy), List.of(damage));

    assertEquals(4, enemy.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldApplyPoisonToEnemy() {
    Entity enemy = new Entity().addComponent(new CombatStatsComponent(10, 2));

    ResolvedCardEffect poison =
        new ResolvedCardEffect(
            "poison_dagger", EffectType.POISON, TargetType.SINGLE_ENEMY, 3, 2, 2);

    handler.applyEnemyEffects(List.of(enemy), List.of(poison));

    CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);

    assertTrue(stats.hasStatusEffect("POISON"));
  }

  @Test
  void shouldApplyVulnerableToEnemy() {
    Entity enemy = new Entity().addComponent(new CombatStatsComponent(10, 2));

    ResolvedCardEffect vulnerable =
        new ResolvedCardEffect("expose", EffectType.VULNERABLE, TargetType.ALL_ENEMIES, 2, 0, 2);

    handler.applyEnemyEffects(List.of(enemy), List.of(vulnerable));

    CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);

    assertFalse(stats.hasStatusEffect(vulnerable.cardId()));
  }

  @Test
  void shouldIgnoreEnemyEffectWhenEnemyHasNoCombatStats() {
    Entity enemy = new Entity();

    ResolvedCardEffect damage =
        new ResolvedCardEffect("strike", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 6, 0, 0);

    assertDoesNotThrow(() -> handler.applyEnemyEffects(List.of(enemy), List.of(damage)));
  }

  @Test
  void shouldIgnorePlayerEffectsThatAreNotPlayerFacing() {
    Entity player = new Entity().addComponent(new CombatStatsComponent(10, 2));

    ResolvedCardEffect damage =
        new ResolvedCardEffect("strike", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 6, 0, 0);

    handler.applyPlayerEffects(List.of(damage), player);

    assertEquals(10, player.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldApplyBlockToPlayer() {
    Entity player = new Entity().addComponent(new CombatStatsComponent(10, 2));

    ResolvedCardEffect block =
        new ResolvedCardEffect("defend", EffectType.BLOCK, TargetType.SELF, 5, 0, 0);

    handler.applyPlayerEffects(List.of(block), player);

    assertEquals(5, player.getComponent(CombatStatsComponent.class).getArmor());
  }

  @Test
  void shouldApplyImmediateHealToPlayer() {
    CombatStatsComponent stats = new CombatStatsComponent(10, 2);

    stats.takeDamage(4);

    Entity player = new Entity().addComponent(stats);

    ResolvedCardEffect heal =
        new ResolvedCardEffect("bandage", EffectType.HEAL, TargetType.SELF, 3, 0, 0);

    handler.applyPlayerEffects(List.of(heal), player);

    assertEquals(9, stats.getHealth());
  }

  @Test
  void shouldApplyHealAsStatusEffectWhenDurationIsPositive() {
    Entity player = new Entity().addComponent(new CombatStatsComponent(10, 2));

    ResolvedCardEffect heal =
        new ResolvedCardEffect("bandage", EffectType.HEAL, TargetType.SELF, 3, 0, 2);

    handler.applyPlayerEffects(List.of(heal), player);

    assertFalse(player.getComponent(CombatStatsComponent.class).hasStatusEffect(heal.cardId()));
  }

  @Test
  void shouldApplyStrengthToPlayer() {
    Entity player = new Entity().addComponent(new CombatStatsComponent(10, 2));

    ResolvedCardEffect strength =
        new ResolvedCardEffect("inner_focus", EffectType.STRENGTH, TargetType.SELF, 2, 0, 2);

    handler.applyPlayerEffects(List.of(strength), player);

    assertFalse(player.getComponent(CombatStatsComponent.class).hasStatusEffect(strength.cardId()));
  }

  @Test
  void shouldIgnorePlayerEffectsWhenPlayerHasNoCombatStats() {
    Entity player = new Entity();

    ResolvedCardEffect block =
        new ResolvedCardEffect("defend", EffectType.BLOCK, TargetType.SELF, 5, 0, 0);

    assertDoesNotThrow(() -> handler.applyPlayerEffects(List.of(block), player));
  }

  @Test
  void shouldReturnAllLivingEnemiesForEnemyTargetingCard() {
    Entity livingEnemy = new Entity().addComponent(new CombatStatsComponent(10, 2));

    Entity deadEnemy = new Entity().addComponent(new CombatStatsComponent(0, 2));

    List<Entity> enemies = List.of(livingEnemy, deadEnemy);

    CardPlayRequest request = CardPlayRequest.allEnemies("starfall");

    List<Entity> targets = handler.getLivingEnemyTargets(request, enemies);

    assertEquals(List.of(livingEnemy), targets);
  }

  @Test
  void shouldReturnNoEnemiesForPlayerTargetingCard() {
    Entity enemy = new Entity().addComponent(new CombatStatsComponent(10, 2));

    CardPlayRequest request = CardPlayRequest.self("defend");

    List<Entity> targets = handler.getLivingEnemyTargets(request, List.of(enemy));

    assertTrue(targets.isEmpty());
  }

  @Test
  void shouldReturnNoEnemiesWhenAllEnemiesAreDead() {
    Entity enemyOne = new Entity().addComponent(new CombatStatsComponent(0, 2));

    Entity enemyTwo = new Entity().addComponent(new CombatStatsComponent(0, 2));

    CardPlayRequest request = CardPlayRequest.allEnemies("expose");

    List<Entity> targets = handler.getLivingEnemyTargets(request, List.of(enemyOne, enemyTwo));

    assertTrue(targets.isEmpty());
  }
}
