package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Loads Sprint 2 cards from the production configuration and checks their library contracts. */
@ExtendWith(GameExtension.class)
class SprintTwoCardsIntegrationTest {
  private CardLibrary library;

  @BeforeEach
  void setUp() {
    library = new CardLibrary(CardConfigLoader.loadCards());
  }

  @Test
  void shouldLoadWardingSweepWithExpectedFields() {
    CardConfig card = library.getCard("warding_sweep").orElseThrow();

    assertAll(
        () -> assertEquals("warding_sweep", card.id),
        () -> assertEquals("Warding Sweep", card.name),
        () -> assertEquals("Deal 4 damage to all enemies.", card.description),
        () -> assertEquals(2, card.cost),
        () -> assertEquals(CardType.ATTACK, card.type),
        () -> assertEquals(Rarity.COMMON, card.rarity),
        () -> assertEquals(TargetType.ALL_ENEMIES, card.target),
        () -> assertEquals("images/cards/warding_sweep.png", card.texturePath));
    assertEquals(1, card.effects.length);
    assertAll(
        () -> assertEquals(EffectType.DAMAGE, card.effects[0].type),
        () -> assertEquals(4, card.effects[0].value),
        () -> assertEquals(0, card.effects[0].duration));
  }

  @Test
  void shouldValidateLoadedWardingSweep() {
    CardConfig card = library.getCard("warding_sweep").orElseThrow();

    assertTrue(CardValidator.validate(card).isEmpty());
  }

  @Test
  void shouldLoadSentinelsRebukeWithExpectedFields() {
    CardConfig card = library.getCard("sentinels_rebuke").orElseThrow();

    assertAll(
        () -> assertEquals("sentinels_rebuke", card.id),
        () -> assertEquals("Sentinel's Rebuke", card.name),
        () -> assertEquals("Deal 4 damage. Apply 1 Feeble for 2 turns.", card.description),
        () -> assertEquals(1, card.cost),
        () -> assertEquals(CardType.ATTACK, card.type),
        () -> assertEquals(Rarity.UNCOMMON, card.rarity),
        () -> assertEquals(TargetType.SINGLE_ENEMY, card.target),
        () -> assertEquals("images/cards/sentinels_rebuke.png", card.texturePath));
    assertEquals(2, card.effects.length);
    assertAll(
        () -> assertEquals(EffectType.DAMAGE, card.effects[0].type),
        () -> assertEquals(4, card.effects[0].value),
        () -> assertEquals(0, card.effects[0].duration),
        () -> assertEquals(EffectType.FEEBLE, card.effects[1].type),
        () -> assertEquals(1, card.effects[1].value),
        () -> assertEquals(2, card.effects[1].duration));
  }

  @Test
  void shouldValidateLoadedSentinelsRebuke() {
    CardConfig card = library.getCard("sentinels_rebuke").orElseThrow();

    assertTrue(CardValidator.validate(card).isEmpty());
  }

  @Test
  void shouldLoadWardensJudgementWithExpectedFields() {
    CardConfig card = library.getCard("wardens_judgement").orElseThrow();

    assertAll(
        () -> assertEquals("wardens_judgement", card.id),
        () -> assertEquals("Warden's Judgement", card.name),
        () -> assertEquals("Deal 9 damage.", card.description),
        () -> assertEquals(2, card.cost),
        () -> assertEquals(CardType.ATTACK, card.type),
        () -> assertEquals(Rarity.UNCOMMON, card.rarity),
        () -> assertEquals(TargetType.SINGLE_ENEMY, card.target),
        () -> assertEquals("images/cards/wardens_judgement.png", card.texturePath));
    assertEquals(1, card.effects.length);
    assertAll(
        () -> assertEquals(EffectType.DAMAGE, card.effects[0].type),
        () -> assertEquals(9, card.effects[0].value),
        () -> assertEquals(0, card.effects[0].duration));
  }

  @Test
  void shouldValidateLoadedWardensJudgement() {
    CardConfig card = library.getCard("wardens_judgement").orElseThrow();

    assertTrue(CardValidator.validate(card).isEmpty());
  }

  @Test
  void shouldLoadUnsealTheBreachWithExpectedFields() {
    CardConfig card = library.getCard("unseal_the_breach").orElseThrow();

    assertAll(
        () -> assertEquals("unseal_the_breach", card.id),
        () -> assertEquals("Unseal the Breach", card.name),
        () -> assertEquals("Deal 2 damage. Reduce the enemy's armour by 3.", card.description),
        () -> assertEquals(1, card.cost),
        () -> assertEquals(CardType.ATTACK, card.type),
        () -> assertEquals(Rarity.UNCOMMON, card.rarity),
        () -> assertEquals(TargetType.SINGLE_ENEMY, card.target),
        () -> assertEquals("images/cards/unseal_the_breach.png", card.texturePath));
    assertEquals(2, card.effects.length);
    assertAll(
        () -> assertEquals(EffectType.DAMAGE, card.effects[0].type),
        () -> assertEquals(2, card.effects[0].value),
        () -> assertEquals(0, card.effects[0].duration),
        () -> assertEquals(EffectType.SUNDER, card.effects[1].type),
        () -> assertEquals(3, card.effects[1].value),
        () -> assertEquals(0, card.effects[1].duration));
  }

  @Test
  void shouldValidateLoadedUnsealTheBreach() {
    CardConfig card = library.getCard("unseal_the_breach").orElseThrow();

    assertTrue(CardValidator.validate(card).isEmpty());
  }
}
