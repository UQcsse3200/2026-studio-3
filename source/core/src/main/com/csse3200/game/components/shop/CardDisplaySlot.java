package com.csse3200.game.components.shop;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.csse3200.game.components.cards.CardWidget;
import java.util.function.BiConsumer;

/** Live tabletop offer: readable card, slim price plaque and purchase button, no cabinet. */
final class CardDisplaySlot extends Group {
  static final float SLOT_HEIGHT = 458f;
  private static final float CARD_DISPLAY_WIDTH = 238f;
  private static final float CARD_DISPLAY_HEIGHT = 365f;
  private final Group cardLayer = new Group();
  private final Label priceLabel;
  private final TextButton purchaseButton;
  private String itemId;
  private long revision;

  CardDisplaySlot(
      Drawable pixel,
      Label.LabelStyle priceStyle,
      TextButton.TextButtonStyle buttonStyle,
      BiConsumer<String, Long> purchase) {
    setSize(270, SLOT_HEIGHT);
    setTouchable(Touchable.childrenOnly);
    // Account for transparent margins in the authored face, so visible pixels fill the wood
    // opening.
    cardLayer.setPosition(18, 94);
    cardLayer.setTouchable(Touchable.disabled);
    addActor(cardLayer);
    Table pricePanel = new Table();
    // The scenery's lower wood rail is the price plaque; only the live text overlays it.
    priceLabel = new Label("", priceStyle);
    priceLabel.setFontScale(1f);
    pricePanel.add(priceLabel);
    pricePanel.setBounds(8.5f, 78, 253, 24);
    addActor(pricePanel);
    TextButton.TextButtonStyle insetStyle = new TextButton.TextButtonStyle(buttonStyle);
    insetStyle.up = new MerchantShopFrame(pixel, new Color(0.14f, 0.043f, 0.027f, 1));
    insetStyle.down = new MerchantShopFrame(pixel, new Color(0.10f, 0.035f, 0.024f, 1));
    insetStyle.disabled = new MerchantShopFrame(pixel, new Color(0.08f, 0.064f, 0.056f, 1));
    insetStyle.over = new MerchantShopFrame(pixel, new Color(0.22f, 0.085f, 0.04f, 1));
    purchaseButton = new TextButton("Purchase", insetStyle);
    purchaseButton.getLabel().setFontScale(0.96f);
    purchaseButton.setBounds(3, 28, 264, 46);
    purchaseButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (!purchaseButton.isDisabled() && itemId != null) purchase.accept(itemId, revision);
          }
        });
    addActor(purchaseButton);
  }

  /** Painted openings are slightly closer together than the evenly spaced price plaques. */
  void alignWithFrame(int index) {
    cardLayer.setX(18f - index * 3f);
  }

  void bind(Actor card, int price, String offerId, long quoteRevision) {
    itemId = offerId;
    revision = quoteRevision;
    purchaseButton.setName("purchase-" + offerId);
    priceLabel.setText(price + " GOLD");
    cardLayer.clearChildren();
    if (card instanceof CardWidget) {
      card.setSize(CardWidget.CARD_WIDTH, CardWidget.CARD_HEIGHT);
      Group fitted = new Group();
      fitted.setScale(
          CARD_DISPLAY_WIDTH / CardWidget.CARD_WIDTH, CARD_DISPLAY_HEIGHT / CardWidget.CARD_HEIGHT);
      fitted.addActor(card);
      cardLayer.addActor(fitted);
    } else {
      card.setSize(CARD_DISPLAY_WIDTH, CARD_DISPLAY_HEIGHT);
      cardLayer.addActor(card);
    }
  }

  TextButton purchaseButton() {
    return purchaseButton;
  }
}
