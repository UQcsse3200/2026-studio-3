package com.csse3200.game.components.battle;

import com.csse3200.game.components.spritedisplay.displaying.CardPreviewDisplay;
import com.csse3200.game.components.spritedisplay.displaying.Displaying;
import com.csse3200.game.components.spritedisplay.displaying.DisplayingFactory;
import com.csse3200.game.components.spritedisplay.displaying.DisplayingRecord;
import com.csse3200.game.components.spritedisplay.displaying.PopupTextDisplay;
import com.csse3200.game.ui.PopupDisplay;
import java.util.List;

/**
 * The deck editor's {@link DisplayingFactory}. Overrides the factory method so displays that are
 * positioned inside the editor's popup are given that popup; every other variant falls through to
 * the defaults.
 */
public class DeckEditorDisplayingFactory extends DisplayingFactory {

  /** Text laid out relative to the popup window. */
  public static final String POPUP_TEXT = "popupText";

  /** Enlarged preview of the card under the cursor. */
  public static final String CARD_PREVIEW = "cardPreview";

  private final PopupDisplay popup;

  public DeckEditorDisplayingFactory(List<DisplayingRecord> records, PopupDisplay popup) {
    super(records);
    this.popup = popup;
  }

  @Override
  protected Displaying createDisplaying(DisplayingRecord rec) {
    return switch (rec.variant()) {
      case POPUP_TEXT -> {
        PopupTextDisplay display = new PopupTextDisplay(rec);
        display.addPopup(popup);
        yield display;
      }
      case CARD_PREVIEW -> {
        CardPreviewDisplay display = new CardPreviewDisplay(rec);
        display.addPopup(popup);
        yield display;
      }
      default -> super.createDisplaying(rec);
    };
  }
}
