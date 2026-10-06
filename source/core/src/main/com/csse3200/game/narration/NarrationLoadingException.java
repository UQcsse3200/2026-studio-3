package com.csse3200.game.narration;

/** Indicates a missing, malformed or invalid narration configuration. */
public class NarrationLoadingException extends RuntimeException {
  public NarrationLoadingException(String message) {
    super(message);
  }

  public NarrationLoadingException(String message, Throwable cause) {
    super(message, cause);
  }
}
