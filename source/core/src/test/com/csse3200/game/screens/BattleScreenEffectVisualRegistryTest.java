package com.csse3200.game.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.components.battle.EffectVisualRegistry;
import com.csse3200.game.components.battle.EffectVisualStyle;
import com.csse3200.game.components.enemy.IntentIcons;
import org.junit.jupiter.api.Test;

class BattleScreenEffectVisualRegistryTest {
  @Test
  void shouldRegisterOffensiveAndEnemyStatusVisualsInBattleSetup() {
    EffectVisualRegistry registry = BattleScreen.createEffectVisualRegistry();

    assertEquals(IntentIcons.ATTACK, registry.lookup(EffectType.DAMAGE).iconPath());
    for (EffectType type :
        new EffectType[] {EffectType.POISON, EffectType.VULNERABLE, EffectType.FEEBLE}) {
      EffectVisualStyle style = registry.lookup(type);
      assertEquals(IntentIcons.DEBUFF, style.iconPath());
      assertNotEquals(Color.WHITE, style.color());
    }
    assertNotEquals(
        registry.lookup(EffectType.POISON).color(), registry.lookup(EffectType.VULNERABLE).color());
    assertNotEquals(
        registry.lookup(EffectType.VULNERABLE).color(), registry.lookup(EffectType.FEEBLE).color());
  }

  @Test
  void shouldRetainGeneratedGlowStyleForUnregisteredEffects() {
    EffectVisualStyle style = BattleScreen.createEffectVisualRegistry().lookup(EffectType.HEAL);

    assertNull(style.iconPath());
    assertEquals(Color.WHITE, style.color());
  }
}
