package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
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

  @Test
  void shouldAcceptAValidCard() {
    assertTrue(CardValidator.isValid(validCard()));
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
  void shouldAcceptFortifyOnSelf() {
    CardConfig card = validCard();
    card.id = "iron_oath";
    card.name = "Iron Oath";
    card.type = CardType.SKILL;
    card.rarity = Rarity.UNCOMMON;
    card.target = TargetType.SELF;
    card.effects = new EffectConfig[] {new EffectConfig(EffectType.FORTIFY, 4)};
    card.texturePath = "images/cards/iron_oath.png";
    assertTrue(CardValidator.isValid(card));
    assertTrue(CardValidator.isCompatibleWithTarget(EffectType.FORTIFY, TargetType.SELF));
  }

  @Test
  void shouldRejectFortifyInsideEnemyUpgradeBlock() {
    CardConfig card = validCard();
    com.csse3200.game.cards.configs.CardUpgradeConfig upgrade =
        new com.csse3200.game.cards.configs.CardUpgradeConfig();
    upgrade.name = "Bad+";
    upgrade.description = "Invalid fortify on enemy.";
    upgrade.cost = 1;
    upgrade.rarity = Rarity.COMMON;
    upgrade.effects = new EffectConfig[] {new EffectConfig(EffectType.FORTIFY, 4)};
    card.upgrade = upgrade;

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
}
