package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class CardKeywordTest {
  @Test
  void shouldRecogniseAllSupportedKeywordsRegardlessOfCase() {
    assertEquals(
        List.of(
            CardKeyword.BLOCK,
            CardKeyword.STRENGTH,
            CardKeyword.VULNERABLE,
            CardKeyword.POISON,
            CardKeyword.FEEBLE,
            CardKeyword.ARMOUR),
        CardKeyword.findIn("block STRENGTH vulnerable PoIsOn feeble ArMoUr"));
  }

  @Test
  void shouldIgnoreKeywordsInsideLongerWords() {
    assertEquals(
        List.of(),
        CardKeyword.findIn(
            "blocker strengthening invulnerable poisoned feebleness armoured armorplate"));
  }

  @Test
  void shouldReturnUniqueKeywordsInDescriptionOrder() {
    assertEquals(
        List.of(CardKeyword.STRENGTH, CardKeyword.BLOCK, CardKeyword.POISON),
        CardKeyword.findIn(
            "Gain Strength and Block. Apply Poison, then gain more BLOCK and Strength."));
  }

  @Test
  void shouldRecogniseKeywordsMentionedAsDefences() {
    assertEquals(
        List.of(CardKeyword.BLOCK, CardKeyword.ARMOUR, CardKeyword.POISON),
        CardKeyword.findIn(
            "Deal 10 piercing damage, ignoring Block and Armour. Apply 4 Poison for 2 turns."));
  }

  @Test
  void shouldRecogniseArmourInIronOathDescription() {
    assertEquals(
        List.of(CardKeyword.ARMOUR),
        CardKeyword.findIn(
            "You swear upon iron that the fallen shall be released. Gain 4 Armour."));
  }

  @Test
  void shouldTreatArmorAndArmourAsTheSameKeyword() {
    assertEquals(
        List.of(CardKeyword.ARMOUR), CardKeyword.findIn("Gain Armor, armour and more ARMOR."));
  }

  @Test
  void shouldRecogniseKeywordsBesidePunctuationAndAcrossLines() {
    assertEquals(
        List.of(
            CardKeyword.BLOCK,
            CardKeyword.STRENGTH,
            CardKeyword.VULNERABLE,
            CardKeyword.POISON,
            CardKeyword.FEEBLE),
        CardKeyword.findIn("Block.\nStrength; Vulnerable, Poison: Feeble!"));
  }

  @Test
  void shouldReturnNoKeywordsForOrdinaryOrEmptyDescriptions() {
    assertEquals(List.of(), CardKeyword.findIn("Deal 6 damage."));
    assertEquals(List.of(), CardKeyword.findIn(""));
  }
}
