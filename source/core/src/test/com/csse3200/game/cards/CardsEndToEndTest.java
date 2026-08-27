package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * End-to-end coverage for the Sprint 1 initial card set: load real {@code configs/cards.json},
 * validate, register into {@link CardLibrary}, and retrieve by ID.
 *
 * <p>Sprint grouping (docs) maps onto CardType as:
 *
 * <ul>
 *   <li>Attack ×2 → {@code ATTACK}: strike, poison_dagger
 *   <li>Defence ×2 → {@code SKILL} with BLOCK/HEAL: defend, bandage
 *   <li>Utility ×2 → {@code SKILL}/{@code POWER}: expose, inner_focus
 * </ul>
 */
@ExtendWith(GameExtension.class)
class CardsEndToEndTest {
  private static final Set<String> EXPECTED_IDS =
      Set.of("strike", "defend", "poison_dagger", "expose", "inner_focus", "bandage");

  private CardLibrary library;

  @BeforeEach
  void setUp() {
    List<CardConfig> cards = CardConfigLoader.loadCards();
    library = new CardLibrary(cards);
  }

  @Test
  void shouldLoadValidateRegisterAndRetrieveAllSixCards() {
    assertEquals(6, library.getAllCards().size());

    Set<String> loadedIds =
        library.getAllCards().stream().map(card -> card.id).collect(Collectors.toSet());
    assertEquals(EXPECTED_IDS, loadedIds);

    for (String id : EXPECTED_IDS) {
      assertTrue(library.getCard(id).isPresent(), "missing card: " + id);
    }
  }

  @Test
  void shouldReturnEmptyOptionalForUnknownId() {
    Optional<CardConfig> missing = library.getCard("nonexistent");
    assertTrue(missing.isEmpty());
  }

  @Test
  void shouldExposeCorrectFieldsForAttackCards() {
    CardConfig strike = library.getCard("strike").orElseThrow();
    CardConfig poisonDagger = library.getCard("poison_dagger").orElseThrow();

    assertAll(
        "strike (Attack → ATTACK)",
        () -> assertEquals("Strike", strike.name),
        () -> assertEquals(1, strike.cost),
        () -> assertEquals(CardType.ATTACK, strike.type),
        () -> assertEquals(Rarity.COMMON, strike.rarity),
        () -> assertEquals(TargetType.SINGLE_ENEMY, strike.target),
        () -> assertEquals(1, strike.effects.length),
        () -> assertEffect(strike.effects[0], EffectType.DAMAGE, 6, 0),
        () -> assertEquals("images/cards/strike.png", strike.texturePath));

    assertAll(
        "poison_dagger (Attack → ATTACK)",
        () -> assertEquals("Poison Dagger", poisonDagger.name),
        () -> assertEquals(1, poisonDagger.cost),
        () -> assertEquals(CardType.ATTACK, poisonDagger.type),
        () -> assertEquals(Rarity.UNCOMMON, poisonDagger.rarity),
        () -> assertEquals(TargetType.SINGLE_ENEMY, poisonDagger.target),
        () -> assertEquals(2, poisonDagger.effects.length),
        () -> assertEffect(poisonDagger.effects[0], EffectType.DAMAGE, 4, 0),
        () -> assertEffect(poisonDagger.effects[1], EffectType.POISON, 3, 3),
        () -> assertEquals("images/cards/poison_dagger.png", poisonDagger.texturePath));
  }

  @Test
  void shouldExposeCorrectFieldsForDefenceCards() {
    CardConfig defend = library.getCard("defend").orElseThrow();
    CardConfig bandage = library.getCard("bandage").orElseThrow();

    assertAll(
        "defend (Defence → SKILL + BLOCK)",
        () -> assertEquals("Defend", defend.name),
        () -> assertEquals(1, defend.cost),
        () -> assertEquals(CardType.SKILL, defend.type),
        () -> assertEquals(Rarity.COMMON, defend.rarity),
        () -> assertEquals(TargetType.SELF, defend.target),
        () -> assertEquals(1, defend.effects.length),
        () -> assertEffect(defend.effects[0], EffectType.BLOCK, 5, 0),
        () -> assertEquals("images/cards/defend.png", defend.texturePath));

    assertAll(
        "bandage (Defence → SKILL + HEAL)",
        () -> assertEquals("Bandage", bandage.name),
        () -> assertEquals(1, bandage.cost),
        () -> assertEquals(CardType.SKILL, bandage.type),
        () -> assertEquals(Rarity.COMMON, bandage.rarity),
        () -> assertEquals(TargetType.SELF, bandage.target),
        () -> assertEquals(1, bandage.effects.length),
        () -> assertEffect(bandage.effects[0], EffectType.HEAL, 6, 0),
        () -> assertEquals("images/cards/bandage.png", bandage.texturePath));
  }

  @Test
  void shouldExposeCorrectFieldsForUtilityCards() {
    CardConfig expose = library.getCard("expose").orElseThrow();
    CardConfig innerFocus = library.getCard("inner_focus").orElseThrow();

    assertAll(
        "expose (Utility → SKILL + VULNERABLE)",
        () -> assertEquals("Expose", expose.name),
        () -> assertEquals(1, expose.cost),
        () -> assertEquals(CardType.SKILL, expose.type),
        () -> assertEquals(Rarity.UNCOMMON, expose.rarity),
        () -> assertEquals(TargetType.ALL_ENEMIES, expose.target),
        () -> assertEquals(1, expose.effects.length),
        () -> assertEffect(expose.effects[0], EffectType.VULNERABLE, 2, 2),
        () -> assertEquals("images/cards/expose.png", expose.texturePath));

    assertAll(
        "inner_focus (Utility → POWER + STRENGTH)",
        () -> assertEquals("Inner Focus", innerFocus.name),
        () -> assertEquals(2, innerFocus.cost),
        () -> assertEquals(CardType.POWER, innerFocus.type),
        () -> assertEquals(Rarity.RARE, innerFocus.rarity),
        () -> assertEquals(TargetType.SELF, innerFocus.target),
        () -> assertEquals(1, innerFocus.effects.length),
        () -> assertEffect(innerFocus.effects[0], EffectType.STRENGTH, 2, 0),
        () -> assertEquals("images/cards/inner_focus.png", innerFocus.texturePath));
  }

  private static void assertEffect(EffectConfig effect, EffectType type, int value, int duration) {
    assertEquals(type, effect.type);
    assertEquals(value, effect.value);
    assertEquals(duration, effect.duration);
  }
}
