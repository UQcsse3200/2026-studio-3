package com.csse3200.game.tutorial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BattleTutorialPromptContentTest {
  @Test
  void containsEightShortConceptPrompts() {
    List<String> prompts =
        List.of(
            BattleTutorialPromptContent.OPENING_HAND,
            BattleTutorialPromptContent.CARD_COST,
            BattleTutorialPromptContent.ENERGY,
            BattleTutorialPromptContent.HEALTH,
            BattleTutorialPromptContent.BUFFS_AND_STATUS,
            BattleTutorialPromptContent.CARD_DRAW,
            BattleTutorialPromptContent.END_TURN,
            BattleTutorialPromptContent.WIN_OR_LOSE);

    assertEquals(8, prompts.size());
    assertTrue(prompts.stream().allMatch(prompt -> !prompt.isBlank() && prompt.length() <= 150));
    assertTrue(BattleTutorialPromptContent.OPENING_HAND.contains("up to five"));
    assertTrue(BattleTutorialPromptContent.ENERGY.contains("start of each of your turns"));
    assertTrue(BattleTutorialPromptContent.END_TURN.contains("Surviving enemies act"));
    assertTrue(BattleTutorialPromptContent.WIN_OR_LOSE.contains("every enemy"));
  }
}
