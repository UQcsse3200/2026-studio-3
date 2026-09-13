package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

class EffectConfigTest {
  private CardConfig validCardWith(EffectConfig effect) {
    CardConfig card = new CardConfig();
    card.id = "strike";
    card.name = "Strike";
    card.description = "Deal 6 damage.";
    card.cost = 1;
    card.type = CardType.ATTACK;
    card.rarity = Rarity.COMMON;
    card.target = TargetType.SINGLE_ENEMY;
    card.effects = new EffectConfig[] {effect};
    card.texturePath = "images/cards/strike.png";
    return card;
  }

  @Test
  void shouldDefaultEffectFields() {
    EffectConfig effect = new EffectConfig();

    assertEquals(EffectType.DAMAGE, effect.type);
    assertEquals(0, effect.value);
    assertEquals(0, effect.duration);
  }

  @Test
  void shouldRejectDefaultEffectForZeroValue() {
    EffectConfig effect = new EffectConfig();
    CardConfig card = validCardWith(effect);

    // The default type is DAMAGE; the sole validation failure is its non-positive value.
    assertEquals(
        List.of("effect 0: DAMAGE value must be positive, was 0"), CardValidator.validate(card));
  }

  @Test
  void shouldAssignThreeArgumentConstructorFields() {
    EffectConfig effect = new EffectConfig(EffectType.POISON, 3, 2);

    assertEquals(EffectType.POISON, effect.type);
    assertEquals(3, effect.value);
    assertEquals(2, effect.duration);
  }

  @Test
  void shouldDefaultDurationInTwoArgumentConstructor() {
    EffectConfig effect = new EffectConfig(EffectType.BLOCK, 5);

    assertEquals(EffectType.BLOCK, effect.type);
    assertEquals(5, effect.value);
    assertEquals(0, effect.duration);
  }

  @Test
  void shouldRejectOngoingEffectCreatedWithoutDuration() {
    EffectConfig effect = new EffectConfig(EffectType.POISON, 3);
    CardConfig card = validCardWith(effect);

    // Ongoing effects must use the three-argument constructor to set a positive duration.
    assertFalse(CardValidator.isValid(card));
    assertEquals(
        List.of("effect 0: POISON requires a positive duration, was 0"),
        CardValidator.validate(card));
  }
}
