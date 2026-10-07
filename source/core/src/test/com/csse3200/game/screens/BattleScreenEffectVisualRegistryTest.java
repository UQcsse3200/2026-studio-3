package com.csse3200.game.screens;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.components.battle.EffectVisualRegistry;
import com.csse3200.game.components.battle.EffectVisualStyle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BattleScreenEffectVisualRegistryTest {
  @Test
  void shouldRegisterCombinedBattleVisualsInBattleSetup() {
    EffectVisualRegistry registry = BattleScreen.createEffectVisualRegistry();

    // Damage uses the generated pixel burst from Team 7's animation coordinator.
    assertNull(registry.lookup(EffectType.DAMAGE).iconPath());
    for (EffectType type :
        new EffectType[] {EffectType.POISON, EffectType.VULNERABLE, EffectType.FEEBLE}) {
      EffectVisualStyle style = registry.lookup(type);
      assertEquals(
          "images/effects/enemy-status/" + type.name().toLowerCase() + ".png", style.iconPath());
    }
    assertNotEquals(
        registry.lookup(EffectType.POISON).iconPath(),
        registry.lookup(EffectType.VULNERABLE).iconPath());
  }

  @Test
  void shouldRetainPlayerEffectVisualsFromAnimationBranch() {
    EffectVisualStyle style = BattleScreen.createEffectVisualRegistry().lookup(EffectType.HEAL);

    assertEquals("images/effects/heal.png", style.iconPath());
    assertEquals(Color.WHITE, style.color());
  }
}
