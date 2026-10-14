package com.csse3200.game.encounters.results;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Encounter-scoped, one-shot presentation lifecycle for results already committed by their owner.
 */
public final class AcquisitionPresentation implements AutoCloseable {
  public enum Dismissal {
    CONTINUE,
    CLOSE
  }

  private final AcquisitionView view;
  private final Set<String> shownResults = new HashSet<>();
  private Active active;
  private boolean disposed;
  private boolean dismissing;

  private record Active(String id, Consumer<Dismissal> callback) {}

  public AcquisitionPresentation(AcquisitionView view) {
    this.view = Objects.requireNonNull(view, "view");
  }

  /** Returns false for a duplicate, a busy view or a disposed owner, without calling the UI. */
  public boolean show(AcquisitionResult result, Consumer<Dismissal> onDismiss) {
    Objects.requireNonNull(result, "result");
    Objects.requireNonNull(onDismiss, "onDismiss");
    if (disposed || active != null || !shownResults.add(result.resultId())) {
      return false;
    }
    Active presentation = new Active(result.resultId(), onDismiss);
    active = presentation;
    try {
      view.show(
          result,
          () -> dismiss(presentation, Dismissal.CONTINUE),
          () -> dismiss(presentation, Dismissal.CLOSE));
      return true;
    } catch (RuntimeException exception) {
      if (active == presentation) {
        active = null;
        shownResults.remove(result.resultId());
        try {
          view.hide();
        } catch (RuntimeException cleanupException) {
          exception.addSuppressed(cleanupException);
        }
      }
      throw exception;
    }
  }

  private void dismiss(Active presentation, Dismissal reason) {
    if (disposed || dismissing || active != presentation) {
      return;
    }
    dismissing = true;
    try {
      view.hide();
    } finally {
      dismissing = false;
    }
    if (!disposed) {
      active = null;
      presentation.callback().accept(reason);
    }
  }

  /** Disposal hides the view and invalidates late callbacks without completing the encounter. */
  @Override
  public void close() {
    if (disposed) {
      return;
    }
    disposed = true;
    if (active != null) {
      active = null;
      view.hide();
    }
    shownResults.clear();
  }
}
