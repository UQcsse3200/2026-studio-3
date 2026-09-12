package com.csse3200.game.cards;

/** Thrown when card definitions cannot be loaded or validated. */
public class CardLoadingException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /**
   * Creates a card loading exception.
   *
   * @param message explanation of the loading failure
   */
  public CardLoadingException(String message) {
    super(message);
  }

  /**
   * Creates a card loading exception caused by another exception.
   *
   * @param message explanation of the loading failure
   * @param cause original cause of the failure
   */
  public CardLoadingException(String message, Throwable cause) {
    super(message, cause);
  }
}
