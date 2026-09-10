package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.CardUpgradeConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

class CardValidatorTest {
  private CardConfig validCard() {
    CardConfig card = new CardConfig();
    card.id = "strike";
    card.name = "Strike";
    card.description = "Deal 6 damage.";
    card.cost = 1;
    card.type = CardType.ATTACK;
    card.rarity = Rarity.COMMON;
    card.target = TargetType.SINGLE_ENEMY;
    card.effects = new EffectConfig[] {new EffectConfig(EffectType.DAMAGE, 6)};
    card.texturePath = "images/cards/strike.png";
    return card;
  }

  private CardUpgradeConfig validUpgrade() {
    CardUpgradeConfig upgrade = new CardUpgradeConfig();
    upgrade.name = "Strike+";
    upgrade.description = "Deal 12 damage.";
    upgrade.cost = 1;
    upgrade.rarity = Rarity.COMMON;
    upgrade.effects = new EffectConfig[] {new EffectConfig(EffectType.DAMAGE, 12)};
    return upgrade;
  }

  @Test
  void shouldAcceptAValidCard() {
    assertTrue(CardValidator.isValid(validCard()));
  }

  @Test
  void shouldAcceptImmediateAndTimedHealingButRejectNegativeDuration() {
    CardConfig card = validCard();
    card.target = TargetType.SELF;
    card.effects = new EffectConfig[] {new EffectConfig(EffectType.HEAL, 6, 3)};
    assertTrue(CardValidator.isValid(card));
    card.effects[0].duration = 0;
    assertTrue(CardValidator.isValid(card));
    card.effects[0].duration = -1;
    assertFalse(CardValidator.isValid(card));
  }

  @Test
  void shouldAcceptZeroCost() {
    CardConfig card = validCard();
    card.cost = 0;
    assertTrue(CardValidator.isValid(card));
  }

  @Test
  void shouldAcceptMultipleEffectsOnOneCard() {
    CardConfig card = validCard();
    card.effects =
        new EffectConfig[] {
          new EffectConfig(EffectType.DAMAGE, 4), new EffectConfig(EffectType.POISON, 3, 3)
        };
    assertTrue(CardValidator.isValid(card));
  }

  @Test
  void shouldAcceptStrengthWithoutDuration() {
    CardConfig card = validCard();
    card.type = CardType.POWER;
    card.target = TargetType.SELF;
    card.effects = new EffectConfig[] {new EffectConfig(EffectType.STRENGTH, 2)};
    assertTrue(CardValidator.isValid(card));
  }

  @Test
  void shouldRejectNullCard() {
    assertFalse(CardValidator.isValid(null));
  }

  @Test
  void shouldRejectEmptyIdAndName() {
    CardConfig card = validCard();
    card.id = "";
    card.name = "";
    assertEquals(2, CardValidator.validate(card).size());
  }

  @Test
  void shouldRejectBlankId() {
    CardConfig card = validCard();
    card.id = "  ";
    assertFalse(CardValidator.isValid(card));
  }

  @Test
  void shouldRejectBlankName() {
    CardConfig card = validCard();
    card.name = "  ";
    assertFalse(CardValidator.isValid(card));
  }

  @Test
  void shouldRejectNegativeCost() {
    CardConfig card = validCard();
    card.cost = -1;
    assertFalse(CardValidator.isValid(card));
  }

  @Test
  void shouldRejectBlankTexturePath() {
    CardConfig card = validCard();
    card.texturePath = "";
    assertFalse(CardValidator.isValid(card));
  }

  @Test
  void shouldRejectCardWithNoEffects() {
    CardConfig card = validCard();
    card.effects = new EffectConfig[0];
    assertFalse(CardValidator.isValid(card));
  }

  @Test
  void shouldRejectNullEffectEntry() {
    CardConfig card = validCard();
    card.effects = new EffectConfig[] {null};
    assertFalse(CardValidator.isValid(card));
  }

  @Test
  void shouldRejectNonPositiveEffectValue() {
    CardConfig card = validCard();
    card.effects = new EffectConfig[] {new EffectConfig(EffectType.DAMAGE, 0)};
    assertFalse(CardValidator.isValid(card));
  }

  @Test
  void shouldRejectNegativeEffectValue() {
    CardConfig card = validCard();
    card.effects = new EffectConfig[] {new EffectConfig(EffectType.DAMAGE, -1)};
    assertFalse(CardValidator.isValid(card));
  }

  @Test
  void shouldRejectOngoingEffectWithoutDuration() {
    CardConfig card = validCard();
    card.effects = new EffectConfig[] {new EffectConfig(EffectType.POISON, 3, 0)};
    assertFalse(CardValidator.isValid(card));
  }

  @Test
  void shouldRejectInstantEffectWithDuration() {
    CardConfig card = validCard();
    card.effects = new EffectConfig[] {new EffectConfig(EffectType.DAMAGE, 6, 2)};
    assertFalse(CardValidator.isValid(card));
  }

  @Test
  void shouldReportEveryProblemAtOnce() {
    CardConfig card = validCard();
    card.id = "";
    card.name = "";
    card.cost = -5;
    assertEquals(3, CardValidator.validate(card).size());
  }

  @Test
  void shouldAcceptValidUpgradeWithoutMutatingConfiguration() {
    CardConfig card = validCard();
    CardUpgradeConfig upgrade = validUpgrade();
    EffectConfig effect = upgrade.effects[0];
    card.upgrade = upgrade;

    assertTrue(CardValidator.isValid(card));
    assertSame(upgrade, card.upgrade);
    assertSame(effect, card.upgrade.effects[0]);
    assertEquals(12, effect.value);
  }

  @Test
  void shouldRejectInvalidUpgradeFieldsWithPrecisePaths() {
    CardConfig card = validCard();
    card.upgrade = validUpgrade();
    card.upgrade.name = null;
    card.upgrade.description = " ";
    card.upgrade.cost = -1;
    card.upgrade.rarity = null;
    card.upgrade.effects = null;

    List<String> errors = CardValidator.validate(card);

    assertTrue(errors.contains("upgrade.name must not be blank"));
    assertTrue(errors.contains("upgrade.description must not be blank"));
    assertTrue(errors.contains("upgrade.cost must not be negative, was -1"));
    assertTrue(errors.contains("upgrade.rarity must not be null"));
    assertTrue(errors.contains("upgrade.effects must define at least one effect"));
  }

  @Test
  void shouldRejectEmptyOrNullUpgradeEffectEntries() {
    CardConfig card = validCard();
    card.upgrade = validUpgrade();
    card.upgrade.effects = new EffectConfig[0];
    assertTrue(
        CardValidator.validate(card).contains("upgrade.effects must define at least one effect"));

    card.upgrade.effects = new EffectConfig[] {null};
    assertTrue(CardValidator.validate(card).contains("upgrade.effects[0] must not be null"));
  }

  @Test
  void shouldValidateUpgradeEffectTypeValueAndDuration() {
    CardConfig card = validCard();
    card.upgrade = validUpgrade();
    card.upgrade.effects = new EffectConfig[] {new EffectConfig(null, 1, 0)};
    assertTrue(CardValidator.validate(card).contains("upgrade.effects[0].type must not be null"));

    card.upgrade.effects = new EffectConfig[] {new EffectConfig(EffectType.DAMAGE, 0, 2)};
    List<String> damageErrors = CardValidator.validate(card);
    assertTrue(damageErrors.contains("upgrade.effects[0].value must be positive, was 0"));
    assertTrue(damageErrors.contains("upgrade.effects[0].duration must be zero for DAMAGE, was 2"));

    card.upgrade.effects = new EffectConfig[] {new EffectConfig(EffectType.POISON, 3, 0)};
    assertTrue(
        CardValidator.validate(card)
            .contains("upgrade.effects[0].duration must be positive for POISON, was 0"));
  }

  @Test
  void shouldValidateUpgradeEffectsAgainstInheritedTarget() {
    CardConfig card = validCard();
    card.target = TargetType.SELF;
    card.effects = new EffectConfig[] {new EffectConfig(EffectType.BLOCK, 5)};
    card.upgrade = validUpgrade();

    assertTrue(
        CardValidator.validate(card)
            .contains(
                "upgrade.effects[0].type DAMAGE is not compatible with inherited target SELF"));

    card.target = TargetType.SINGLE_ENEMY;
    card.effects = new EffectConfig[] {new EffectConfig(EffectType.DAMAGE, 6)};
    card.upgrade.effects = new EffectConfig[] {new EffectConfig(EffectType.BLOCK, 8)};
    assertTrue(
        CardValidator.validate(card)
            .contains(
                "upgrade.effects[0].type BLOCK is not compatible with inherited target"
                    + " SINGLE_ENEMY"));
  }

  @Test
  void shouldAllowImmediateOrTimedHealingInSelfTargetedUpgrade() {
    CardConfig card = validCard();
    card.target = TargetType.SELF;
    card.effects = new EffectConfig[] {new EffectConfig(EffectType.HEAL, 6)};
    card.upgrade = validUpgrade();
    card.upgrade.effects = new EffectConfig[] {new EffectConfig(EffectType.HEAL, 8, 3)};
    assertTrue(CardValidator.isValid(card));

    card.upgrade.effects[0].duration = 0;
    assertTrue(CardValidator.isValid(card));

    card.upgrade.effects[0].duration = -1;
    assertTrue(
        CardValidator.validate(card)
            .contains("upgrade.effects[0].duration must not be negative for HEAL"));
  }
}
