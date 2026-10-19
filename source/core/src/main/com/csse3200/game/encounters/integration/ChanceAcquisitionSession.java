package com.csse3200.game.encounters.integration;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.encounters.results.AcquisitionPresentation;
import com.csse3200.game.encounters.results.AcquisitionResult;
import com.csse3200.game.encounters.results.AcquisitionView;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Optional acquisition presentation for one Chance session; never executes reward settlement. */
public final class ChanceAcquisitionSession implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(ChanceAcquisitionSession.class);
  private final ChanceEncounterSession session;
  private final CardService cards;
  private AcquisitionView view;
  private AcquisitionPresentation presentation;
  private boolean closed;

  ChanceAcquisitionSession(ChanceEncounterSession session, CardService cards) {
    this.session = session;
    this.cards = cards;
    closed = session.isCompleted();
  }

  /** Returns the session's cached committed data; a pending or failed choice has no result. */
  public Optional<AcquisitionResult> getResult() {
    return session.getAcquisitionResult(cards);
  }

  /**
   * Shows an optional view. Continue invokes the existing Event completion action; Close restores
   * its already-settled Outcome page. The UI owner supplies both actions and retains its old flow
   * when a view is missing or fails. Duplicate/busy requests do not dismiss an active view. The
   * same view is retained for the lifetime of this session.
   *
   * @param acquisitionView Guoqing's view, or null while unavailable
   * @param onContinue existing completion action, including any scene transition
   * @param onClose action restoring the old Outcome/Continue UI without completing the Event
   * @return true when the result was shown; false when absent, duplicate, busy, closed or failed
   */
  public boolean show(AcquisitionView acquisitionView, Runnable onContinue, Runnable onClose) {
    Objects.requireNonNull(onContinue, "onContinue");
    Objects.requireNonNull(onClose, "onClose");
    if (closed || session.isCompleted() || acquisitionView == null) return false;
    try {
      Optional<AcquisitionResult> result = getResult();
      if (result.isEmpty()) return false;
      if (presentation == null) {
        view = acquisitionView;
        presentation = new AcquisitionPresentation(view);
      } else if (view != acquisitionView) {
        return false;
      }
      return presentation.show(
          result.get(),
          dismissal -> {
            if (dismissal == AcquisitionPresentation.Dismissal.CONTINUE) onContinue.run();
            else onClose.run();
          });
    } catch (RuntimeException exception) {
      logger.warn("Chance acquisition display failed; retain the settled Outcome flow", exception);
      return false;
    }
  }

  /** Invalidates display callbacks without completing the Event or changing any reward. */
  @Override
  public void close() {
    if (closed) return;
    closed = true;
    if (presentation != null) {
      try {
        presentation.close();
      } catch (RuntimeException exception) {
        logger.warn("Chance acquisition view cleanup failed", exception);
      }
    }
  }
}
