package com.csse3200.game.components.battle;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.components.spritedisplay.displaying.BattleLogDisplay;
import com.csse3200.game.components.spritedisplay.displaying.CardPreviewDisplay;
import com.csse3200.game.components.spritedisplay.displaying.Displaying;
import com.csse3200.game.components.spritedisplay.displaying.DisplayingRecord;
import com.csse3200.game.components.spritedisplay.displaying.PopupTextDisplay;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.ui.PopupDisplay;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class DeckEditorDisplayingFactoryTest {

  private final PopupDisplay popup = mock(PopupDisplay.class);
  private final DeckEditorDisplayingFactory factory =
      new DeckEditorDisplayingFactory(List.of(), popup);

  @Test
  void popupTextIsGivenThePopup() {
    Displaying display =
        factory.createDisplaying(displayRecord(DeckEditorDisplayingFactory.POPUP_TEXT));

    assertInstanceOf(PopupTextDisplay.class, display);
    // Drawing positions the text inside the popup window, so it must read the injected popup.
    display.render(mock(SpriteBatch.class));
    verify(popup, atLeastOnce()).getWindowX();
  }

  @Test
  void cardPreviewIsCreated() {
    Displaying display =
        factory.createDisplaying(displayRecord(DeckEditorDisplayingFactory.CARD_PREVIEW));

    assertInstanceOf(CardPreviewDisplay.class, display);
  }

  @Test
  void otherVariantsFallBackToTheDefaults() {
    Displaying display = factory.createDisplaying(displayRecord("battleLog"));

    assertInstanceOf(BattleLogDisplay.class, display);
  }

  private static DisplayingRecord displayRecord(String variant) {
    return DisplayingRecord.builder("text").variant(variant).build();
  }
}
