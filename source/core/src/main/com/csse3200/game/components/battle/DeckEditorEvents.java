package com.csse3200.game.components.battle;

public final class DeckEditorEvents {

  /** Fired when the popup opens. No payload. */
  public static final String OPENED = "deckEditorOpened";

  /** Fired when the popup closes. Displays clear their content on this. No payload. */
  public static final String CLOSED = "deckEditorClosed";

  /** Status/validation message for the editor's error strip. Payload: {@code String}. */
  public static final String ERROR = "deckError";

  /** Text summarising the current selection. Payload: {@code String}. */
  public static final String SUMMARY = "deckSummary";

  /** Card to show enlarged in the preview panel. Payload: {@code CardPreviewDisplay.Content}. */
  public static final String PREVIEW = "deckPreview";

  /** Numbered selection badges. Payload: {@code List<CardBadgesDisplay.Badge>}. */
  public static final String BADGES = "deckEditorBadges";

  /** Re-assert draw order: card widgets behind the interactive layer. No payload. */
  public static final String TO_FRONT_BEHIND_CARDS = "deckEditorToFrontBehindCards";

  /** Re-assert draw order: overlay layer to the front. No payload. */
  public static final String TO_FRONT = "deckEditorToFront";

  private DeckEditorEvents() {}
}
