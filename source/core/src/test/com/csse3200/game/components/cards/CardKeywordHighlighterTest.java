package com.csse3200.game.components.cards;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CardKeywordHighlighterTest {
  @Test
  void highlightsAllSixKeywordsAndPreservesCasingAndPunctuation() {
    assertEquals(
        "[#8B4513]Block[], [#8B4513]Armour[], [#8B4513]Strength[], [#8B4513]Vulnerable[], [#8B4513]Poison[], [#8B4513]Feeble[]. [#8B4513]poison[]!",
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
        "[[RED]Gain 6 [#8B4513]Block[]. [[] Then 2 [#8B4513]Strength[].",
        CardKeywordHighlighter.highlight("[RED]Gain 6 Block. [] Then 2 Strength."));
    assertEquals("", CardKeywordHighlighter.highlight(""));
  }
}
