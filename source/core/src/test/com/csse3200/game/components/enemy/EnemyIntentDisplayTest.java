package com.csse3200.game.components.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class EnemyIntentDisplayTest {

  private Entity entity;
  private EnemyIntentDisplay intentDisplay;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerRenderService(new RenderService());

    entity = new Entity();
    intentDisplay = new EnemyIntentDisplay();

    entity.addComponent(intentDisplay);
    intentDisplay.create();
  }

  @AfterEach
  void tearDown() {
    intentDisplay.dispose();
  }

  @Test
  void shouldSetIntentWhenIntentChanged() {
    EnemyIntent intent = new EnemyIntent(IntentType.ATTACK, 2);

    entity.getEvents().trigger("intentChanged", intent);

    assertEquals(intent, intentDisplay.getCurrentIntent());
  }

  @Test
  void shouldClearIntentWhenEnemyDefeated() {
    EnemyIntent intent = new EnemyIntent(IntentType.ATTACK, 5);

    // First give the display an intent
    entity.getEvents().trigger("intentChanged", intent);
    assertEquals(intent, intentDisplay.getCurrentIntent());

    // Then defeat the enemy
    entity.getEvents().trigger("enemyDefeated");

    assertNull(intentDisplay.getCurrentIntent());
  }

  @Test
  void shouldDisplayPositiveDuration() {
    EnemyIntent intent = EnemyIntent.debuff(IntentEffectType.SILENCE, 1, 2);

    assertEquals("2", EnemyIntentDisplay.durationTextFor(intent));
  }

  @Test
  void shouldDisplayMultiDigitDuration() {
    EnemyIntent intent = EnemyIntent.debuff(IntentEffectType.DAMAGE_ON_CARD_PLAY, 1, 12);

    assertEquals("12", EnemyIntentDisplay.durationTextFor(intent));
  }

  @Test
  void shouldNotDisplayZeroDuration() {
    EnemyIntent intent = EnemyIntent.debuff(IntentEffectType.SILENCE, 1, 0);

    assertEquals("", EnemyIntentDisplay.durationTextFor(intent));
  }

  @Test
  void shouldNotDisplayNegativeDuration() {
    EnemyIntent intent = EnemyIntent.debuff(IntentEffectType.SILENCE, 1, -1);

    assertEquals("", EnemyIntentDisplay.durationTextFor(intent));
  }

  @Test
  void shouldNotDisplayDurationForNullIntent() {
    assertEquals("", EnemyIntentDisplay.durationTextFor(null));
  }

  @Test
  void shouldNotDisplayDurationForNormalAttack() {
    EnemyIntent intent = EnemyIntent.attack(6);

    assertEquals("", EnemyIntentDisplay.durationTextFor(intent));
  }

  @Test
  void shouldShowDamageForAttackIntents() {
    assertEquals("16", EnemyIntentDisplay.labelTextFor(EnemyIntent.attack(16)));
  }

  @Test
  void shouldShowArmourForDefendIntents() {
    assertEquals("5", EnemyIntentDisplay.labelTextFor(EnemyIntent.defend(5)));
  }

  @Test
  void shouldShowDurationForStatusIntents() {
    assertEquals(
        "2", EnemyIntentDisplay.labelTextFor(EnemyIntent.debuff(IntentEffectType.TAUNT, 0, 2)));
  }

  @Test
  void shouldShowNothingForNullIntent() {
    assertEquals("", EnemyIntentDisplay.labelTextFor(null));
  }
}
