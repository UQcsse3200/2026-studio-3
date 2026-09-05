package com.csse3200.game.components.spritedisplay.clickable;

import java.util.Arrays;
import java.util.Objects;

/**
 * Everything a drop target needs to fire the right event when something is dropped on it: the event
 * name to trigger, the arguments to trigger it with, and a human-readable label for UI feedback
 * (e.g. "Strike played").
 *
 * <p>This is intentionally generic — it has no knowledge of cards, health, or any other game
 * concept. {@link com.csse3200.game.components.spritedisplay.clickable.DragNDrop} packs one of
 * these into the drag payload from whatever {@link ClickableRecord} it was built from, and {@link
 * com.csse3200.game.components.spritedisplay.reactive.EnemyDropTargetComponent} supplies the target
 * ID. {@link DragNDrop} then fires {@code trigger(cardId, targetId)} on the source entity. Neither
 * class needs to know *what* trigger/args mean — that's entirely up to whoever defined the
 * card/button (e.g. via JSON) and whoever listens for the resulting event (e.g. PlayerActions).
 *
 * <p>{@code args} supports 0–3 elements, matching the arities EventHandler natively supports.
 */
public record TriggerPayload(String trigger, Object[] args, String label) {

  /** Convenience factory when you don't need a separate display label (defaults to trigger). */
  public static TriggerPayload of(String trigger, Object... args) {
    return new TriggerPayload(trigger, args, trigger);
  }

  public static TriggerPayload of(String trigger, String label, Object... args) {
    return new TriggerPayload(trigger, args, label);
  }

  // Explicit equals/hashCode/toString because the auto-generated record versions compare the
  // Object[] by reference, not content.
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return o instanceof TriggerPayload(String otherTrigger, Object[] otherArgs, String otherLabel)
        && Objects.equals(trigger, otherTrigger)
        && Arrays.equals(args, otherArgs)
        && Objects.equals(label, otherLabel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(trigger, Arrays.hashCode(args), label);
  }

  @Override
  public String toString() {
    return "TriggerPayload[trigger="
        + trigger
        + ", args="
        + Arrays.toString(args)
        + ", label="
        + label
        + "]";
  }
}
