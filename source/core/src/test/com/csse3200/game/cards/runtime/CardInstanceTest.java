package com.csse3200.game.cards.runtime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CardInstanceTest {
  @Test
  void shouldCreateNormalAndUpgradedInstances() {
    CardInstance normal = new CardInstance("copy-1", "strike", CardInstance.BASE_LEVEL);
    CardInstance upgraded = new CardInstance("copy-2", "strike", CardInstance.UPGRADED_LEVEL);

    assertAll(
        () -> assertEquals("copy-1", normal.instanceId()),
        () -> assertEquals("strike", normal.cardId()),
        () -> assertFalse(normal.isUpgraded()),
        () -> assertTrue(upgraded.isUpgraded()));
  }

  @Test
  void shouldUpgradeImmutablyWhilePreservingIdentity() {
    CardInstance normal = new CardInstance("copy-1", "strike", CardInstance.BASE_LEVEL);

    CardInstance upgraded = normal.upgrade();

    assertNotSame(normal, upgraded);
    assertEquals(normal.instanceId(), upgraded.instanceId());
    assertEquals(normal.cardId(), upgraded.cardId());
    assertEquals(CardInstance.BASE_LEVEL, normal.upgradeLevel());
    assertEquals(CardInstance.UPGRADED_LEVEL, upgraded.upgradeLevel());
  }

  @Test
  void shouldRejectUpgradingAnAlreadyUpgradedInstance() {
    CardInstance upgraded = new CardInstance("copy-1", "strike", CardInstance.UPGRADED_LEVEL);

    assertThrows(IllegalStateException.class, upgraded::upgrade);
  }

  @Test
  void shouldRejectInvalidInstanceIds() {
    assertAll(
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () -> new CardInstance(null, "strike", CardInstance.BASE_LEVEL)),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () -> new CardInstance("", "strike", CardInstance.BASE_LEVEL)),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () -> new CardInstance("  ", "strike", CardInstance.BASE_LEVEL)));
  }

  @Test
  void shouldRejectInvalidCardIds() {
    assertAll(
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () -> new CardInstance("copy-1", null, CardInstance.BASE_LEVEL)),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () -> new CardInstance("copy-1", "", CardInstance.BASE_LEVEL)),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () -> new CardInstance("copy-1", "  ", CardInstance.BASE_LEVEL)));
  }

  @Test
  void shouldRejectUnsupportedUpgradeLevels() {
    assertAll(
        () ->
            assertThrows(
                IllegalArgumentException.class, () -> new CardInstance("copy-1", "strike", -1)),
        () ->
            assertThrows(
                IllegalArgumentException.class, () -> new CardInstance("copy-1", "strike", 2)));
  }
}
