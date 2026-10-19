package com.csse3200.game.encounters.results;

/** Presentation boundary for Guoqing's display; callbacks do not themselves grant rewards. */
public interface AcquisitionView {
  void show(AcquisitionResult result, Runnable onContinue, Runnable onClose);

  void hide();
}
