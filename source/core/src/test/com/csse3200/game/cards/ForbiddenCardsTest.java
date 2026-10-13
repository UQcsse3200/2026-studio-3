package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.CardUpgradeConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.cards.runtime.CardResolver;
import com.csse3200.game.cards.runtime.ResolvedCard;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Configuration and loading checks for the Round 2 forbidden card set. */
@ExtendWith(GameExtension.class)
class ForbiddenCardsTest {
  private static final Set<String> FORBIDDEN_CARD_IDS =
      Set.of("sealed_pact", "blood_price", "doom_sigil", "iron_oath");

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
        // Vulnerable is a fixed 1.5x multiplier; upgrade duration, not value.
        () -> assertEffects(upgrade.effects, EffectType.VULNERABLE, 1, 3, EffectType.POISON, 6, 3));
  }

  @Test
  void shouldConfigureIronOath() {
    CardConfig card = library.getCard("iron_oath").orElseThrow();
    CardUpgradeConfig upgrade = card.upgrade;

    assertAll(
        () -> assertEquals("Iron Oath", card.name),
        () -> assertEquals(1, card.cost),
        () -> assertEquals(CardType.SKILL, card.type),
        () -> assertEquals(Rarity.UNCOMMON, card.rarity),
        () -> assertEquals(TargetType.SELF, card.target),
        () -> assertEffect(card.effects, EffectType.FORTIFY, 4, 0),
        () -> assertEquals("images/cards/iron_oath.png", card.texturePath),
        () -> assertEquals("Iron Oath+", upgrade.name),
        () -> assertEquals(1, upgrade.cost),
        () -> assertEquals(Rarity.UNCOMMON, upgrade.rarity),
        () -> assertEffect(upgrade.effects, EffectType.FORTIFY, 6, 0));
  }

  @Test
  void forbiddenUpgradesResolveToUpgradedValues() {
    CardResolver resolver = new CardResolver();

    assertAll(
        () ->
            assertUpgraded(
                resolver, "sealed_pact", "Sealed Pact+", 3, List.of(effect(4, 0), effect(9, 0))),
        () -> assertUpgraded(resolver, "blood_price", "Blood Price+", 3, List.of(effect(28, 0))),
        () ->
            assertUpgraded(
                resolver, "doom_sigil", "Doom Sigil+", 2, List.of(effect(1, 3), effect(6, 3))),
        () -> assertUpgraded(resolver, "iron_oath", "Iron Oath+", 1, List.of(effect(6, 0))));
  }

  private void assertUpgraded(
      CardResolver resolver,
      String cardId,
      String expectedName,
      int expectedCost,
      List<int[]> expectedEffects) {
    CardConfig config = library.getCard(cardId).orElseThrow();
    ResolvedCard upgraded =
        resolver.resolve(
            config, new CardInstance("forbidden-" + cardId, cardId, CardInstance.UPGRADED_LEVEL));

    assertTrue(upgraded.upgraded(), cardId);
    assertEquals(expectedName, upgraded.name(), cardId);
    assertEquals(expectedCost, upgraded.cost(), cardId);
    assertEquals(expectedEffects.size(), upgraded.effects().size(), cardId);
    for (int i = 0; i < expectedEffects.size(); i++) {
      assertEquals(expectedEffects.get(i)[0], upgraded.effects().get(i).value, cardId + " value");
      assertEquals(
          expectedEffects.get(i)[1], upgraded.effects().get(i).duration, cardId + " duration");
    }
  }

  private static int[] effect(int value, int duration) {
    return new int[] {value, duration};
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
