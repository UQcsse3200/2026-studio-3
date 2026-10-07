package com.csse3200.game.components.chance;

/** Player-facing rules shown by contextual Event help. */
final class EventHelpContent {
  static final String DICE_TITLE = "Dice Game Rules";
  static final String DICE_RULES =
      "Predict Low (2-6) or High (8-12) on two dice. A correct first guess stakes 15 Gold; "
          + "a miss ends the wager. Rolling 7 lets you Take 10 Gold or Double Down. A Double "
          + "Down win stakes 30 Gold; another 7 offers Take 30 or one final prediction for "
          + "60 Gold. Even a 7 on that final roll wins 60 Gold.\n\n"
          + "Cash Out your stake, or risk it in round two for cards. A round-two win keeps "
          + "your Gold and awards one random card; Lucky Seven choices can raise the reward "
          + "to two or three cards. A losing prediction forfeits the stake and cards.";

  static final String FUSION_TITLE = "Card Fusion Rules";
  static final String FUSION_RULES =
      "Select three different Common card copies from your deck; copies with the same name "
          + "are allowed. Fusing replaces those three cards with one random Rare card. "
          + "You can successfully fuse once per run.\n\n"
          + "An invalid selection changes nothing and does not use your fusion chance. "
          + "If fusion is unavailable, you can still Leave. Leaving does not use your chance.";

  private EventHelpContent() {}
}
