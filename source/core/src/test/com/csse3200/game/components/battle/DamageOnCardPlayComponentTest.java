package com.csse3200.game.components.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class DamageOnCardPlayComponentTest {
  private CombatStatsComponent stats;
  private Entity battleUi;

  @BeforeEach
  void setUp() {
    stats = new CombatStatsComponent(20, 0);

    battleUi = new Entity().addComponent(new DamageOnCardPlayComponent(stats));
    battleUi.create();
  }

  @Test
  void shouldNotDamageWithoutEffect() {
    battleUi.getEvents().trigger("cardPlayed", "strike", "enemy");

    assertEquals(20, stats.getHealth());
  }

  @Test
  void shouldDamageOnceForEachSuccessfulPlay() {
    stats.applyStatusEffect("DAMAGE_ON_CARD_PLAY", 3, 2);

    battleUi.getEvents().trigger("cardPlayed", "strike", "enemy");
    assertEquals(17, stats.getHealth());

    battleUi.getEvents().trigger("cardPlayed", "defend", "player");
    assertEquals(14, stats.getHealth());

    // Playing cards must not tick the effect's duration.
    assertEquals(2, stats.getStatusEffect("DAMAGE_ON_CARD_PLAY").getDuration());
  }

  @Test
  void shouldNotDamageForAPlayRequestAlone() {
    stats.applyStatusEffect("DAMAGE_ON_CARD_PLAY", 3, 2);

    battleUi.getEvents().trigger("playCard", "strike", "enemy");

    assertEquals(20, stats.getHealth());
  }

  @Test
  void shouldStopDamagingAfterEffectExpires() {
    stats.applyStatusEffect("DAMAGE_ON_CARD_PLAY", 3, 1);

    battleUi.getEvents().trigger("cardPlayed", "strike", "enemy");
    assertEquals(17, stats.getHealth());

    stats.updateStatusEffects();

    battleUi.getEvents().trigger("cardPlayed", "strike", "enemy");
    assertEquals(17, stats.getHealth());
  }

  @Test
  void shouldAllowBlockToAbsorbDamage() {
    stats.applyStatusEffect("DAMAGE_ON_CARD_PLAY", 3, 2);
    stats.setBlock(2);

    battleUi.getEvents().trigger("cardPlayed", "strike", "enemy");

    assertEquals(19, stats.getHealth());
    assertEquals(0, stats.getBlock());
  }
}
