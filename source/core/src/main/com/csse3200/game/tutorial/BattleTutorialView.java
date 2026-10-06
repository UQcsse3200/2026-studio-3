package com.csse3200.game.tutorial;

/** Implemented by the tutorial UI; no battle ownership or navigation is required. */
public interface BattleTutorialView {
  /** Connects the UI's Continue and Exit Tutorial controls. */
  void bindActions(Runnable continueAction, Runnable exitAction);

  /** Shows text and highlights the named live actor, enabling Continue only when allowed. */
  void show(BattleTutorialPrompt prompt);

  /**
   * Optional local damage-number presentation from a real HP difference, never simulated damage.
   */
  default void showResolvedDamage(int amount) {}

  default boolean areTransientEffectsFinished() {
    return true;
  }

  /** Removes tutorial text, highlights and input handlers. Safe to call more than once. */
  void clear();
}
