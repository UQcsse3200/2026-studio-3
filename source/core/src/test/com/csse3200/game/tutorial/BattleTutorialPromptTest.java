package com.csse3200.game.tutorial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BattleTutorialPromptTest {
  @Test
  void explanatoryStepsUseTheSharedContentAndHaveStableHighlightKeys() {
    List<BattleTutorialController.Step> steps =
        List.of(
            BattleTutorialController.Step.HAND,
            BattleTutorialController.Step.CARD_COST,
            BattleTutorialController.Step.ENERGY,
            BattleTutorialController.Step.HEALTH,
            BattleTutorialController.Step.BUFFS,
            BattleTutorialController.Step.CARD_DRAW,
            BattleTutorialController.Step.BATTLE_OUTCOME_RULES);
    List<String> texts =
        List.of(
            BattleTutorialPromptContent.OPENING_HAND,
            BattleTutorialPromptContent.CARD_COST,
            BattleTutorialPromptContent.ENERGY,
            BattleTutorialPromptContent.HEALTH,
            BattleTutorialPromptContent.BUFFS_AND_STATUS,
            BattleTutorialPromptContent.CARD_DRAW,
            BattleTutorialPromptContent.WIN_OR_LOSE);
    List<BattleTutorialPrompt.HighlightTarget> targets =
        List.of(
            BattleTutorialPrompt.HighlightTarget.HAND,
            BattleTutorialPrompt.HighlightTarget.CARD_COST,
            BattleTutorialPrompt.HighlightTarget.ENERGY,
            BattleTutorialPrompt.HighlightTarget.HEALTH,
            BattleTutorialPrompt.HighlightTarget.STATUS_EFFECTS,
            BattleTutorialPrompt.HighlightTarget.DRAW_PILE,
            BattleTutorialPrompt.HighlightTarget.ENEMIES);
    for (int i = 0; i < steps.size(); i++) {
      BattleTutorialPrompt prompt = BattleTutorialPrompt.forStep(steps.get(i));
      assertEquals(steps.get(i), prompt.step());
      assertEquals(texts.get(i), prompt.text());
      assertEquals(targets.get(i), prompt.highlight());
      assertTrue(prompt.canContinue());
    }
  }

  @Test
  void realActionStepsCannotBeSkippedWithContinue() {
    BattleTutorialPrompt play =
        BattleTutorialPrompt.forStep(BattleTutorialController.Step.PLAY_A_CARD);
    BattleTutorialPrompt end = BattleTutorialPrompt.forStep(BattleTutorialController.Step.END_TURN);
    assertFalse(play.canContinue());
    assertFalse(end.canContinue());
    assertEquals(BattleTutorialPromptContent.PLAY_A_CARD, play.text());
    assertNotEquals(BattleTutorialPromptContent.CARD_COST, play.text());
    assertEquals(BattleTutorialPrompt.HighlightTarget.HAND, play.highlight());
    assertEquals(BattleTutorialPromptContent.END_TURN, end.text());
    assertEquals(BattleTutorialPrompt.HighlightTarget.END_TURN, end.highlight());
  }

  @Test
  void everyStepHasAMappingAndTerminalStepsHaveNoHighlight() {
    for (BattleTutorialController.Step step : BattleTutorialController.Step.values()) {
      assertEquals(step, BattleTutorialPrompt.forStep(step).step());
    }
    for (BattleTutorialController.Step step :
        List.of(
            BattleTutorialController.Step.FREE_PLAY,
            BattleTutorialController.Step.BATTLE_ENDED,
            BattleTutorialController.Step.CANCELLED)) {
      assertEquals(
          BattleTutorialPrompt.HighlightTarget.NONE,
          BattleTutorialPrompt.forStep(step).highlight());
      assertFalse(BattleTutorialPrompt.forStep(step).canContinue());
    }
    assertThrows(NullPointerException.class, () -> BattleTutorialPrompt.forStep(null));
  }
}
