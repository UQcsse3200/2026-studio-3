package com.csse3200.game.components.battle;

/**
 * Event names shared by {@link DeckEditorComponent} and the deck editor's {@code Displaying}
 * variants ({@link PopupTextDisplay}, {@link CardPreviewDisplay}, {@link CardBadgesDisplay}). All
 * of them live on the same entity, so they talk through its event handler.
 *
 * <p>SUMMARY, ERROR, PREVIEW and BADGES are also the {@code "trigger"} values in {@code
 * sprites/DeckEditorUi.json} — if you rename one here, rename it there too.
 */
public final class DeckEditorEvents {
  /** The popup was shown. No payload. */
  public static final String OPENED = "deckEditorOpened";

  /** The popup was hidden. No payload. */
  public static final String CLOSED = "deckEditorClosed";

  /** Re-assert stacking order: the popup window pulls itself to the front when clicked. */
  public static final String TO_FRONT = "deckEditorToFront";

  /** Payload: String — the selection summary line. */
  public static final String SUMMARY = "deckSummary";

  /** Payload: String — the error line ("" clears it). */
  public static final String ERROR = "deckError";

  /** Payload: {@link CardPreviewDisplay.Content} (use {@code Content.NONE} to clear). */
  public static final String PREVIEW = "deckPreview";

  /** Payload: {@code List<CardBadgesDisplay.Badge>} (empty list clears). */
  public static final String BADGES = "deckBadges";

  private DeckEditorEvents() {}
}
