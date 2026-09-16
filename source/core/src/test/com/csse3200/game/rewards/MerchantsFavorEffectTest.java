package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class MerchantsFavorEffectTest {

  private Entity player;
  private InventoryComponent inventory;
  private MerchantsFavorEffect effect;

  @BeforeEach
  void setUp() {
    inventory = new InventoryComponent(0);
    player = new Entity().addComponent(inventory);
    player.create();
    effect = new MerchantsFavorEffect();
  }

  @Test
  void appliesFivePercentDiscountOnFirstUse() {
    effect.apply(player);

    assertEquals(0.05f, inventory.getShopDiscount(), 0.0001f);
  }

  @Test
  void stacksDiscountAcrossMultipleApplications() {
    effect.apply(player);
    effect.apply(player);

    assertEquals(0.10f, inventory.getShopDiscount(), 0.0001f);
  }

  @Test
  void discountIsCappedAtFiftyPercent() {
    // 20 applications * 0.05 = 1.0, well past the cap, to confirm it holds at 0.5.
    for (int i = 0; i < 20; i++) {
      effect.apply(player);
    }

    assertEquals(0.50f, inventory.getShopDiscount(), 0.0001f);
  }
}
