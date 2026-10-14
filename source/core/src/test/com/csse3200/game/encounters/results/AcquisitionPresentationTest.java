package com.csse3200.game.encounters.results;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AcquisitionPresentationTest {
  private final TestView view = new TestView();
  private final AcquisitionPresentation presentation = new AcquisitionPresentation(view);

  @Test
  void hideFailureCanBeRetriedWithoutDroppingTheCompletionCallback() {
    AtomicInteger completed = new AtomicInteger();
    presentation.show(result("first"), reason -> completed.incrementAndGet());
    view.failHide = true;
    assertThrows(IllegalStateException.class, view.onContinue::run);
    assertEquals(0, completed.get());
    view.failHide = false;
    view.onContinue.run();
    view.onContinue.run();
    assertEquals(1, completed.get());
  }

  @Test
  void hideCannotReenterDismissOrReplaceTheActiveDisplay() {
    AtomicInteger completed = new AtomicInteger();
    presentation.show(result("first"), reason -> completed.incrementAndGet());
    view.duringHide =
        () -> {
          view.onClose.run();
          assertFalse(presentation.show(result("second"), reason -> fail("Still dismissing")));
        };
    view.onContinue.run();
    assertEquals(1, completed.get());
    assertEquals(1, view.hidden);
  }

  private AcquisitionResult result(String id) {
    return new AcquisitionResult(
        id,
        List.of(
            new AcquisitionResult.Entry(
                AcquisitionResult.Kind.CARD, "strike", 1, "Strike", "Deal damage", null)));
  }

  @Test
  void showsCommittedDataAndContinuesOnlyOnceWithoutApplyingAnything() {
    AtomicInteger completions = new AtomicInteger();
    AcquisitionResult result = result("purchase-1");
    assertTrue(
        presentation.show(
            result,
            reason -> {
              assertEquals(AcquisitionPresentation.Dismissal.CONTINUE, reason);
              completions.incrementAndGet();
            }));
    assertSame(result, view.result);
    view.onContinue.run();
    view.onContinue.run();
    view.onClose.run();
    assertEquals(1, completions.get());
    assertEquals(1, view.hidden);
    assertFalse(presentation.show(result, reason -> fail("Repeated result")));
    assertEquals(1, view.shown);
  }

  @Test
  void preservesDistinctCloseReasonForTheEventOwnerToDecideNavigation() {
    List<AcquisitionPresentation.Dismissal> reasons = new ArrayList<>();
    presentation.show(result("event-1"), reasons::add);
    view.onClose.run();
    view.onContinue.run();
    assertEquals(List.of(AcquisitionPresentation.Dismissal.CLOSE), reasons);
  }

  @Test
  void busyOrOldCallbacksCannotReplaceOrDismissTheCurrentResult() {
    presentation.show(result("first"), reason -> {});
    Runnable oldContinue = view.onContinue;
    assertFalse(presentation.show(result("second"), reason -> fail("Busy")));
    oldContinue.run();
    AtomicInteger next = new AtomicInteger();
    assertTrue(presentation.show(result("second"), reason -> next.incrementAndGet()));
    oldContinue.run();
    assertEquals(1, view.hidden);
    assertEquals(0, next.get());
    view.onContinue.run();
    assertEquals(1, next.get());
  }

  @Test
  void disposingAnOwnerInvalidatesLateCallbacksWithoutCompletingItsEncounter() {
    presentation.show(result("first"), reason -> fail("Disposed owner must not navigate"));
    presentation.close();
    presentation.close();
    view.onContinue.run();
    view.onClose.run();
    assertEquals(1, view.hidden);
    assertFalse(presentation.show(result("later"), reason -> fail("Disposed")));
  }

  @Test
  void failedOpeningCanBeRetriedWithoutDuplicatingBusinessEffects() {
    view.failShow = true;
    assertThrows(
        IllegalStateException.class, () -> presentation.show(result("first"), reason -> {}));
    Runnable failedCallback = view.onContinue;
    view.failShow = false;
    AtomicInteger completed = new AtomicInteger();
    assertTrue(presentation.show(result("first"), reason -> completed.incrementAndGet()));
    failedCallback.run();
    assertEquals(0, completed.get());
    view.onContinue.run();
    assertEquals(1, completed.get());
  }

  @Test
  void completionCallbackFailureCannotCauseASecondCompletion() {
    AtomicInteger called = new AtomicInteger();
    presentation.show(
        result("first"),
        reason -> {
          called.incrementAndGet();
          throw new IllegalStateException("Owner failed");
        });
    assertThrows(IllegalStateException.class, view.onContinue::run);
    view.onContinue.run();
    assertEquals(1, called.get());
  }

  @Test
  void resultSupportsMixedGoodsAndSignedEffectsWithImmutableData() {
    List<AcquisitionResult.Entry> entries =
        new ArrayList<>(
            List.of(
                new AcquisitionResult.Entry(
                    AcquisitionResult.Kind.CARD, "strike", 1, "Strike", "Card", null),
                new AcquisitionResult.Entry(
                    AcquisitionResult.Kind.ITEM, "MERCHANTS_FAVOR", 2, "Relic", "Discount", null),
                new AcquisitionResult.Entry(
                    AcquisitionResult.Kind.POSITIVE_EFFECT,
                    "heal",
                    10,
                    "Healing",
                    "Restore 10 HP",
                    null),
                new AcquisitionResult.Entry(
                    AcquisitionResult.Kind.NEGATIVE_EFFECT,
                    "poison",
                    3,
                    "Poison",
                    "Apply 3 Poison",
                    2)));
    AcquisitionResult result = new AcquisitionResult("event", entries);
    entries.clear();
    assertEquals(4, result.entries().size());
    assertEquals(2, result.entries().getLast().durationTurns());
    assertThrows(UnsupportedOperationException.class, () -> result.entries().clear());
  }

  @Test
  void mixedResourceAndItemResultsUseOneDisplayAndOneCompletion() {
    AcquisitionResult result =
        new AcquisitionResult(
            "event-choice-1",
            List.of(
                AcquisitionResult.Entry.gold(50),
                new AcquisitionResult.Entry(
                    AcquisitionResult.Kind.ITEM,
                    "test-curse-item",
                    1,
                    "Curse Item",
                    "Test item",
                    null),
                AcquisitionResult.Entry.hp(-10)));
    AtomicInteger completed = new AtomicInteger();
    assertTrue(presentation.show(result, reason -> completed.incrementAndGet()));
    assertSame(result, view.result);
    assertEquals(3, view.result.entries().size());
    assertEquals(50, view.result.entries().getFirst().delta());
    assertEquals(-10, view.result.entries().getLast().delta());
    view.onContinue.run();
    view.onClose.run();
    assertEquals(1, completed.get());
    assertEquals(1, view.hidden);
    assertFalse(presentation.show(result, reason -> fail("Repeated settled result")));
    assertEquals(1, view.shown);
  }

  @Test
  void rejectsMalformedPresentationDataBeforeShowingAnything() {
    assertThrows(IllegalArgumentException.class, () -> result(" "));
    assertThrows(IllegalArgumentException.class, () -> new AcquisitionResult("empty", List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AcquisitionResult.Entry(AcquisitionResult.Kind.ITEM, "item", 0, "Item", "", null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new AcquisitionResult.Entry(AcquisitionResult.Kind.ITEM, "item", 1, "Item", "", 1));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AcquisitionResult.Entry(
                AcquisitionResult.Kind.NEGATIVE_EFFECT, "poison", 1, "Poison", "", -1));
  }

  private static final class TestView implements AcquisitionView {
    private AcquisitionResult result;
    private Runnable onContinue;
    private Runnable onClose;
    private int shown;
    private int hidden;
    private boolean failShow;
    private boolean failHide;
    private Runnable duringHide;

    @Override
    public void show(AcquisitionResult result, Runnable onContinue, Runnable onClose) {
      this.result = result;
      this.onContinue = onContinue;
      this.onClose = onClose;
      shown++;
      if (failShow) {
        throw new IllegalStateException("Display construction failed");
      }
    }

    @Override
    public void hide() {
      hidden++;
      if (failHide) {
        throw new IllegalStateException("Display cleanup failed");
      }
      if (duringHide != null) {
        duringHide.run();
      }
    }
  }
}
