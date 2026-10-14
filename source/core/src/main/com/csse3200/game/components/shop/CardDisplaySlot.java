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
    setSize(270, 410);
    setTouchable(Touchable.childrenOnly);
    // Inset the live face inside the painted uprights instead of covering the wood.
    cardLayer.setPosition(14, 94);
    cardLayer.setTouchable(Touchable.disabled);
    addActor(cardLayer);
    Table pricePanel = new Table();
    pricePanel.setBackground(new MerchantShopFrame(pixel, new Color(0.12f, 0.06f, 0.03f, 1)));
    priceLabel = new Label("", priceStyle);
    priceLabel.setFontScale(1f);
    pricePanel.add(priceLabel);
    pricePanel.setBounds(8.5f, 47, 253, 28);
    addActor(pricePanel);
    TextButton.TextButtonStyle insetStyle = new TextButton.TextButtonStyle(buttonStyle);
    insetStyle.up = new MerchantShopFrame(pixel, new Color(0.14f, 0.043f, 0.027f, 1));
    insetStyle.down = new MerchantShopFrame(pixel, new Color(0.10f, 0.035f, 0.024f, 1));
    insetStyle.disabled = new MerchantShopFrame(pixel, new Color(0.08f, 0.064f, 0.056f, 1));
    insetStyle.over = new MerchantShopFrame(pixel, new Color(0.22f, 0.085f, 0.04f, 1));
    purchaseButton = new TextButton("Purchase", insetStyle);
    purchaseButton.getLabel().setFontScale(0.9f);
    purchaseButton.setBounds(8.5f, 6, 253, 40);
    purchaseButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (!purchaseButton.isDisabled() && itemId != null) purchase.accept(itemId, revision);
          }
        });
    addActor(purchaseButton);
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
      fitted.setScale(242f / CardWidget.CARD_WIDTH, 322f / CardWidget.CARD_HEIGHT);
      fitted.addActor(card);
      cardLayer.addActor(fitted);
    } else {
      card.setSize(242, 322);
      cardLayer.addActor(card);
    }
  }

  TextButton purchaseButton() {
    return purchaseButton;
  }
}
