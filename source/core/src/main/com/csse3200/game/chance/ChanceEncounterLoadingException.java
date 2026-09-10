package com.csse3200.game.chance;

/** Thrown when Chance Encounter definitions cannot be loaded from configuration. */
public class ChanceEncounterLoadingException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /**
   * Creates a Chance Encounter loading exception.
   *
   * @param message explanation of the loading failure
   */
  public ChanceEncounterLoadingException(String message) {
    super(message);
  }

  /**
   * Creates a Chance Encounter loading exception caused by another exception.
   *
   * @param message explanation of the loading failure
   * @param cause original cause of the failure
   */
  public ChanceEncounterLoadingException(String message, Throwable cause) {
    super(message, cause);
  }
}
