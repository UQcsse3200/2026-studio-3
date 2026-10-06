package com.csse3200.game.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.components.battle.EffectVisualRegistry;
import com.csse3200.game.components.battle.EffectVisualStyle;
import com.csse3200.game.components.battle.EnemyStatusEffectVisuals;
import com.csse3200.game.components.battle.OffensiveEffectVisuals;
import org.junit.jupiter.api.Test;

class BattleScreenEffectVisualRegistryTest {
  @Test
  void shouldRegisterOffensiveAndEnemyStatusVisualsInBattleSetup() {
    EffectVisualRegistry registry = BattleScreen.createEffectVisualRegistry();

    // Battle setup must register exactly the styles each visual group defines.
    EffectVisualRegistry offensive = new EffectVisualRegistry();
    OffensiveEffectVisuals.registerAll(offensive);
    for (EffectType type :
        new EffectType[] {EffectType.DAMAGE, EffectType.PIERCE, EffectType.SUNDER}) {
      assertEquals(offensive.lookup(type), registry.lookup(type), type.name());
    }

    EffectVisualRegistry enemyStatus = new EffectVisualRegistry();
    EnemyStatusEffectVisuals.registerAll(enemyStatus);
    for (EffectType type :
        new EffectType[] {EffectType.POISON, EffectType.VULNERABLE, EffectType.FEEBLE}) {
      assertEquals(enemyStatus.lookup(type), registry.lookup(type), type.name());
    }
  }

  @Test
  void shouldRetainGeneratedGlowStyleForUnregisteredEffects() {
    EffectVisualStyle style = BattleScreen.createEffectVisualRegistry().lookup(EffectType.HEAL);

    assertNull(style.iconPath());
    assertEquals(Color.WHITE, style.color());
  }
}
