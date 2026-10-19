package com.csse3200.game.shop;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ShopRefreshPolicyTest {
  @Test
  void standardMapShopPolicyUsesTheApprovedFees() {
    ShopRefreshPolicy policy = ShopRefreshPolicy.standard();
    int[] expected = {0, 10, 20, 40, 80, 160};
    for (int completed = 0; completed < expected.length; completed++) {
      assertEquals(expected[completed], policy.costAfter(completed).orElseThrow());
    }
  }

  @Test
  void firstRefreshIsFreeAndSubsequentFeesDouble() {
    ShopRefreshPolicy policy = new ShopRefreshPolicy(10);
    assertEquals(0, policy.costAfter(0).orElseThrow());
    assertEquals(10, policy.costAfter(1).orElseThrow());
    assertEquals(20, policy.costAfter(2).orElseThrow());
    assertEquals(40, policy.costAfter(3).orElseThrow());
  }

  @Test
  void missingTeamDecisionOnlyAllowsTheFreeRefresh() {
    assertEquals(0, ShopRefreshPolicy.pendingFee().costAfter(0).orElseThrow());
    assertTrue(ShopRefreshPolicy.pendingFee().costAfter(1).isEmpty());
  }

  @Test
  void rejectsInvalidSettingsAndNeverWrapsToFreeOrNegativeFees() {
    assertThrows(IllegalArgumentException.class, () -> new ShopRefreshPolicy(0));
    assertThrows(IllegalArgumentException.class, () -> new ShopRefreshPolicy(-1));
    assertThrows(IllegalArgumentException.class, () -> new ShopRefreshPolicy(1).costAfter(-1));
    assertEquals(
        Integer.MAX_VALUE, new ShopRefreshPolicy(Integer.MAX_VALUE).costAfter(1).orElseThrow());
    assertTrue(new ShopRefreshPolicy(Integer.MAX_VALUE).costAfter(2).isEmpty());
    assertTrue(new ShopRefreshPolicy(1).costAfter(32).isEmpty());
    assertTrue(new ShopRefreshPolicy(1).costAfter(Integer.MAX_VALUE).isEmpty());
  }
}
