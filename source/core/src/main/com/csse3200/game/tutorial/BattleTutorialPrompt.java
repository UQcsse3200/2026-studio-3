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
    NONE,
    CARD_INVENTORY,
    ITEM_INVENTORY,
    USED_CARD,
    ENEMY_STATS,
    ENEMY_ARMOUR
  }

  /** Uses the shared teaching content without choosing UI coordinates or changing battle rules. */
  public static BattleTutorialPrompt forStep(BattleTutorialController.Step step) {
    Objects.requireNonNull(step, "step cannot be null");
    return switch (step) {
      case INTRO -> information(step, "Let's learn the basics of battle.", HighlightTarget.NONE);
      case CARD_INVENTORY ->
          information(
              step,
              "Use Card Inventory to view and manage your battle cards.",
              HighlightTarget.CARD_INVENTORY);
      case ITEM_INVENTORY ->
          information(
              step,
              "Use Item Inventory to view\nand use the items you have collected.",
              HighlightTarget.ITEM_INVENTORY);
      case USED_CARD ->
          information(
              step,
              "This card has been used and is now inactive. It returns when its cooldown expires.",
              HighlightTarget.USED_CARD);
      case ENEMY_STATS ->
          information(
              step,
              "This is the enemy's Health. Armour reduces incoming damage.",
              HighlightTarget.ENEMY_STATS);
      case HAND ->
          information(step, BattleTutorialPromptContent.OPENING_HAND, HighlightTarget.HAND);
      case ENEMY_ARMOUR ->
          information(step, "Armour reduces incoming damage.", HighlightTarget.ENEMY_ARMOUR);
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
      case CARD_ANIMATION, ENEMY_ANIMATION, BATTLE_ENDED, CANCELLED ->
          new BattleTutorialPrompt(step, "", HighlightTarget.NONE, false);
    };
  }

  /** The reference flow has its own concise local prompts; legacy shared content is unchanged. */
  public static BattleTutorialPrompt referenceStep(BattleTutorialController.Step step) {
    BattleTutorialPrompt original = forStep(step);
    String text =
        switch (step) {
          case HAND -> "Welcome to battle.\nThese are your cards.\nChoose one to play.";
          case CARD_INVENTORY ->
              "Use Card Inventory to view\nand choose the cards you want to use.";
          case ENERGY -> "Energy is used to play cards.";
          case CARD_COST -> "This number is the Energy\ncost of the card.";
          case PLAY_A_CARD -> "Drag this Strike onto the enemy\nto play it.";
          case ENEMY_STATS -> "This is the enemy's Health.\nReduce it to zero.";
          case USED_CARD ->
              "This card has been used.\n\nIt cannot be used next turn.\n\nIt will be available again\non the following turn.";
          case END_TURN -> "End your turn when you're\ndone playing cards.";
          case HEALTH -> "Enemy attacks reduce your Health.\nDon't let it reach zero.";
          default -> original.text();
        };
    return new BattleTutorialPrompt(
        step,
        text,
        original.highlight(),
        original.canContinue() || step == BattleTutorialController.Step.END_TURN);
  }

  private static BattleTutorialPrompt information(
      BattleTutorialController.Step step, String text, HighlightTarget highlight) {
    return new BattleTutorialPrompt(step, text, highlight, true);
  }
}
