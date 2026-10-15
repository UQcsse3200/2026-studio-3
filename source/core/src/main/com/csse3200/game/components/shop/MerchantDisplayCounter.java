package com.csse3200.game.components.shop;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;

/** Live goods row in front of the scenery board; it owns no shared furniture or cabinet art. */
final class MerchantDisplayCounter extends Group {
  MerchantDisplayCounter() {
    setName("merchant-products");
    setBounds(170, 98, 940, CardDisplaySlot.SLOT_HEIGHT);
    setTouchable(Touchable.childrenOnly);
  }

  void addSlot(CardDisplaySlot slot, int index) {
    slot.setBounds(45 + index * 290, 0, 270, CardDisplaySlot.SLOT_HEIGHT);
    slot.alignWithFrame(index);
    addActor(slot);
  }
}
