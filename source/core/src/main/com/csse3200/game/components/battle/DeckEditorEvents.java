package com.csse3200.game.components.battle;

/**
 * Event names shared by {@link DeckEditorComponent} and the deck editor's {@code Displaying}
 * variants ({@code PopupTextDisplay}, {@code PopupPanelDisplay}, {@code CardPreviewDisplay}, {@code
 * CardBadgesDisplay}, {@code CardFramesDisplay}). All of them live on the same entity, so they talk
 * through its event handler.
 */
public final class DeckEditorEvents {
  /** The popup was shown. No payload. */
  public static final String OPENED = "deckEditorOpened";

  /** The popup was hidden. No payload. */
  public static final String CLOSED = "deckEditorClosed";

  /**
   * Re-assert stacking order for the displays that sit IN FRONT of the card buttons (preview,
   * badges, text...). The popup window pulls itself to the front when clicked. No payload.
   */
  public static final String TO_FRONT = "deckEditorToFront";

  /**
   * Re-assert stacking order for the displays that sit BEHIND the card buttons (card frames). Fired
   * just before the card buttons are brought forward. No payload.
   */
  public static final String TO_FRONT_BEHIND_CARDS = "deckEditorToFrontBehindCards";

  /** Payload: String — the selection summary line. */
  public static final String SUMMARY = "deckSummary";

  /** Payload: String — the error line ("" clears it). */
  public static final String ERROR = "deckError";

  /** Payload: {@code CardPreviewDisplay.Content} (use {@code Content.NONE} to clear). */
  public static final String PREVIEW = "deckPreview";

  /** Payload: {@code List<CardBadgesDisplay.Badge>} (empty list clears). */
  public static final String BADGES = "deckBadges";

  /** Payload: {@code List<CardFramesDisplay.Frame>} (empty list clears). */
  public static final String FRAMES = "deckFrames";

  private DeckEditorEvents() {}
}
