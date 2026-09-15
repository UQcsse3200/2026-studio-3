package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.CardUpgradeConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.extensions.GameExtension;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Configuration and loading checks for the Round 2 forbidden card set. */
@ExtendWith(GameExtension.class)
class ForbiddenCardsTest {
  private static final Set<String> FORBIDDEN_CARD_IDS =
      Set.of("sealed_pact", "blood_price", "doom_sigil", "eclipse_decree");

  private CardLibrary library;

  @BeforeEach
  void setUp() {
    library = new CardLibrary(CardConfigLoader.loadCards());
  }

  @Test
  void shouldLoadAndRegisterForbiddenCards() {
    Set<String> loadedIds =
        library.getAllCards().stream().map(card -> card.id).collect(Collectors.toSet());

    assertTrue(loadedIds.containsAll(FORBIDDEN_CARD_IDS));
    for (String id : FORBIDDEN_CARD_IDS) {
      assertTrue(library.getCard(id).isPresent(), "missing forbidden card: " + id);
    }
  }

  @Test
  void shouldConfigureSealedPact() {
    CardConfig card = library.getCard("sealed_pact").orElseThrow();
    CardUpgradeConfig upgrade = card.upgrade;

    assertAll(
        () -> assertEquals("Sealed Pact", card.name),
        () -> assertEquals(3, card.cost),
        () -> assertEquals(CardType.POWER, card.type),
        () -> assertEquals(Rarity.RARE, card.rarity),
        () -> assertEquals(TargetType.SELF, card.target),
        () -> assertEffects(card.effects, EffectType.STRENGTH, 3, 0, EffectType.BLOCK, 6, 0),
        () -> assertEquals("images/cards/sealed_pact.png", card.texturePath),
        () -> assertEquals("Sealed Pact+", upgrade.name),
        () -> assertEquals(3, upgrade.cost),
        () -> assertEquals(Rarity.RARE, upgrade.rarity),
        () -> assertEffects(upgrade.effects, EffectType.STRENGTH, 4, 0, EffectType.BLOCK, 9, 0));
  }

  @Test
  void shouldConfigureBloodPrice() {
    CardConfig card = library.getCard("blood_price").orElseThrow();
    CardUpgradeConfig upgrade = card.upgrade;

    assertAll(
        () -> assertEquals("Blood Price", card.name),
        () -> assertEquals(3, card.cost),
        () -> assertEquals(CardType.ATTACK, card.type),
        () -> assertEquals(Rarity.UNCOMMON, card.rarity),
        () -> assertEquals(TargetType.SINGLE_ENEMY, card.target),
        () -> assertEffect(card.effects, EffectType.DAMAGE, 20, 0),
        () -> assertEquals("images/cards/blood_price.png", card.texturePath),
        () -> assertEquals("Blood Price+", upgrade.name),
        () -> assertEquals(3, upgrade.cost),
        () -> assertEquals(Rarity.UNCOMMON, upgrade.rarity),
        () -> assertEffect(upgrade.effects, EffectType.DAMAGE, 28, 0));
  }

  @Test
  void shouldConfigureDoomSigil() {
    CardConfig card = library.getCard("doom_sigil").orElseThrow();
    CardUpgradeConfig upgrade = card.upgrade;

    assertAll(
        () -> assertEquals("Doom Sigil", card.name),
        () -> assertEquals(2, card.cost),
        () -> assertEquals(CardType.SKILL, card.type),
        () -> assertEquals(Rarity.RARE, card.rarity),
        () -> assertEquals(TargetType.SINGLE_ENEMY, card.target),
        () -> assertEffects(card.effects, EffectType.VULNERABLE, 1, 2, EffectType.POISON, 4, 3),
        () -> assertEquals("images/cards/doom_sigil.png", card.texturePath),
        () -> assertEquals("Doom Sigil+", upgrade.name),
        () -> assertEquals(2, upgrade.cost),
        () -> assertEquals(Rarity.RARE, upgrade.rarity),
        () -> assertEffects(upgrade.effects, EffectType.VULNERABLE, 2, 2, EffectType.POISON, 6, 3));
  }

  @Test
  void shouldConfigureEclipseDecree() {
    CardConfig card = library.getCard("eclipse_decree").orElseThrow();
    CardUpgradeConfig upgrade = card.upgrade;

    assertAll(
        () -> assertEquals("Eclipse Decree", card.name),
        () -> assertEquals(3, card.cost),
        () -> assertEquals(CardType.SKILL, card.type),
        () -> assertEquals(Rarity.RARE, card.rarity),
        () -> assertEquals(TargetType.ALL_ENEMIES, card.target),
        () -> assertEffect(card.effects, EffectType.FEEBLE, 1, 2),
        () -> assertEquals("images/cards/eclipse_decree.png", card.texturePath),
        () -> assertEquals("Eclipse Decree+", upgrade.name),
        () -> assertEquals(3, upgrade.cost),
        () -> assertEquals(Rarity.RARE, upgrade.rarity),
        () -> assertEffect(upgrade.effects, EffectType.FEEBLE, 1, 3));
  }

  private static void assertEffect(
      EffectConfig[] effects, EffectType type, int value, int duration) {
    assertEquals(1, effects.length);
    assertEffect(effects[0], type, value, duration);
  }

  private static void assertEffects(
      EffectConfig[] effects,
      EffectType firstType,
      int firstValue,
      int firstDuration,
      EffectType secondType,
      int secondValue,
      int secondDuration) {
    assertEquals(2, effects.length);
    assertEffect(effects[0], firstType, firstValue, firstDuration);
    assertEffect(effects[1], secondType, secondValue, secondDuration);
  }

  private static void assertEffect(EffectConfig effect, EffectType type, int value, int duration) {
    assertEquals(type, effect.type);
    assertEquals(value, effect.value);
    assertEquals(duration, effect.duration);
  }
}
