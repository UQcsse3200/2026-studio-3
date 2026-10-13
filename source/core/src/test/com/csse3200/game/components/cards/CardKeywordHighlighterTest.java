package com.csse3200.game.components.cards;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CardKeywordHighlighterTest {
  @Test
  void highlightsAllSixKeywordsAndPreservesCasingAndPunctuation() {
    assertEquals(
        "[YELLOW]Block[], [YELLOW]Armour[], [YELLOW]Strength[], [YELLOW]Vulnerable[], [YELLOW]Poison[], [YELLOW]Feeble[]. [YELLOW]poison[]!",
        CardKeywordHighlighter.highlight(
            "Block, Armour, Strength, Vulnerable, Poison, Feeble. poison!"));
  }

  @Test
  void leavesPartialWordsAndUnrelatedTextUncoloured() {
    assertEquals(
        "Blocked poisonous Strengthened Feebleness Armoury poison_mark. Deal 6 damage.",
        CardKeywordHighlighter.highlight(
            "Blocked poisonous Strengthened Feebleness Armoury poison_mark. Deal 6 damage."));
  }

  @Test
  void escapesLiteralMarkupAndRestoresNormalColourAfterEachKeyword() {
    assertEquals(
        "[[RED]Gain 6 [YELLOW]Block[]. [[] Then 2 [YELLOW]Strength[].",
        CardKeywordHighlighter.highlight("[RED]Gain 6 Block. [] Then 2 Strength."));
    assertEquals("", CardKeywordHighlighter.highlight(""));
  }
}
