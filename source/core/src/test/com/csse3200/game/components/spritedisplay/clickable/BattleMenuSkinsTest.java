package com.csse3200.game.components.spritedisplay.clickable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class BattleMenuSkinsTest {
  @Test
  void allThreeButtonsShareTheMutedFrameButKeepDistinctIcons() {
    Skin card = BattleMenuSkins.forIcon(BattleMenuSkins.Icon.CARD);
    Skin inventory = BattleMenuSkins.forIcon(BattleMenuSkins.Icon.INVENTORY);
    Skin endTurn = BattleMenuSkins.forIcon(BattleMenuSkins.Icon.END_TURN);
    ImageTextButton.ImageTextButtonStyle cardStyle =
        card.get("default", ImageTextButton.ImageTextButtonStyle.class);
    ImageTextButton.ImageTextButtonStyle inventoryStyle =
        inventory.get("default", ImageTextButton.ImageTextButtonStyle.class);
    ImageTextButton.ImageTextButtonStyle endTurnStyle =
        endTurn.get("default", ImageTextButton.ImageTextButtonStyle.class);

    assertSame(card, BattleMenuSkins.forIcon(BattleMenuSkins.Icon.CARD));
    assertSame(cardStyle.up, inventoryStyle.up);
    assertSame(cardStyle.up, endTurnStyle.up);
    assertNotSame(cardStyle.imageUp, inventoryStyle.imageUp);
    assertNotSame(inventoryStyle.imageUp, endTurnStyle.imageUp);
    assertNotNull(cardStyle.over);
    assertNotNull(cardStyle.down);
    assertEquals(48f, cardStyle.imageUp.getMinWidth(), 0.01f);
    assertEquals(48f, cardStyle.imageUp.getMinHeight(), 0.01f);
    assertEquals(-2f, cardStyle.pressedOffsetY, 0.01f);
  }
}
