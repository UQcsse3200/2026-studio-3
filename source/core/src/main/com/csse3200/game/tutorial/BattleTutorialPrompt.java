package com.csse3200.game.tutorial;

import java.util.Objects;

/** UI-independent instruction and highlight request for one teaching step. */
public record BattleTutorialPrompt(
    BattleTutorialController.Step step,
    String text,
    HighlightTarget highlight,
    boolean canContinue) {
  public enum HighlightTarget {
    HAND,
    CARD_COST,
    ENERGY,
    HEALTH,
    STATUS_EFFECTS,
    DRAW_PILE,
    END_TURN,
    ENEMIES,
    NONE
  }

  /** Uses the shared teaching content without choosing UI coordinates or changing battle rules. */
  public static BattleTutorialPrompt forStep(BattleTutorialController.Step step) {
    Objects.requireNonNull(step, "step cannot be null");
    return switch (step) {
      case HAND ->
          information(step, BattleTutorialPromptContent.OPENING_HAND, HighlightTarget.HAND);
      case CARD_COST ->
          information(step, BattleTutorialPromptContent.CARD_COST, HighlightTarget.CARD_COST);
      case ENERGY -> information(step, BattleTutorialPromptContent.ENERGY, HighlightTarget.ENERGY);
      case HEALTH -> information(step, BattleTutorialPromptContent.HEALTH, HighlightTarget.HEALTH);
      case BUFFS ->
          information(
              step, BattleTutorialPromptContent.BUFFS_AND_STATUS, HighlightTarget.STATUS_EFFECTS);
      case CARD_DRAW ->
          information(step, BattleTutorialPromptContent.CARD_DRAW, HighlightTarget.DRAW_PILE);
      case BATTLE_OUTCOME_RULES ->
          information(step, BattleTutorialPromptContent.WIN_OR_LOSE, HighlightTarget.ENEMIES);
      case PLAY_A_CARD ->
          new BattleTutorialPrompt(
              step, BattleTutorialPromptContent.PLAY_A_CARD, HighlightTarget.HAND, false);
      case END_TURN ->
          new BattleTutorialPrompt(
              step, BattleTutorialPromptContent.END_TURN, HighlightTarget.END_TURN, false);
      case FREE_PLAY ->
          new BattleTutorialPrompt(
              step, BattleTutorialPromptContent.WIN_OR_LOSE, HighlightTarget.NONE, false);
      case BATTLE_ENDED, CANCELLED ->
          new BattleTutorialPrompt(step, "", HighlightTarget.NONE, false);
    };
  }

  private static BattleTutorialPrompt information(
      BattleTutorialController.Step step, String text, HighlightTarget highlight) {
    return new BattleTutorialPrompt(step, text, highlight, true);
  }
}
