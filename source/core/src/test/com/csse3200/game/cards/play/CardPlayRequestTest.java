package com.csse3200.game.cards.play;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CardPlayRequestTest {

  @Test
  void shouldCreateRequestWithValidInstanceIdAndTarget() {
    CardPlayTarget target = CardPlayTarget.self();

    CardPlayRequest request = new CardPlayRequest("strike", target);

    assertEquals("strike", request.instanceId());
    assertEquals(target, request.target());
  }

  @Test
  void shouldRejectNullInstanceId() {
    CardPlayTarget cardPlayTarget = CardPlayTarget.self();

    assertThrows(IllegalArgumentException.class, () -> new CardPlayRequest(null, cardPlayTarget));
  }

  @Test
  void shouldRejectBlankInstanceId() {
    CardPlayTarget cardPlayTarget = CardPlayTarget.self();

    assertThrows(IllegalArgumentException.class, () -> new CardPlayRequest("", cardPlayTarget));

    assertThrows(IllegalArgumentException.class, () -> new CardPlayRequest("   ", cardPlayTarget));
  }

  @Test
  void shouldRejectInstanceIdWithLeadingWhitespace() {
    CardPlayTarget cardPlayTarget = CardPlayTarget.self();

    assertThrows(
        IllegalArgumentException.class, () -> new CardPlayRequest(" strike", cardPlayTarget));
  }

  @Test
  void shouldRejectInstanceIdWithTrailingWhitespace() {
    CardPlayTarget cardPlayTarget = CardPlayTarget.self();

    assertThrows(
        IllegalArgumentException.class, () -> new CardPlayRequest("strike ", cardPlayTarget));
  }

  @Test
  void shouldRejectInstanceIdWithLeadingAndTrailingWhitespace() {
    CardPlayTarget cardPlayTarget = CardPlayTarget.self();

    assertThrows(
        IllegalArgumentException.class, () -> new CardPlayRequest(" strike ", cardPlayTarget));
  }

  @Test
  void shouldRejectNullTarget() {
    assertThrows(IllegalArgumentException.class, () -> new CardPlayRequest("strike", null));
  }

  @Test
  void shouldCreateSelfTargetingRequest() {
    CardPlayRequest request = CardPlayRequest.self("defend");

    assertEquals("defend", request.instanceId());
    assertEquals(CardPlayTarget.self(), request.target());
  }

  @Test
  void shouldCreateSingleEnemyTargetingRequest() {
    CardPlayRequest request = CardPlayRequest.singleEnemy("strike", "enemy-1");

    assertEquals("strike", request.instanceId());
    assertEquals(CardPlayTarget.singleEnemy("enemy-1"), request.target());
  }

  @Test
  void shouldRejectBlankTargetIdForSingleEnemy() {
    assertThrows(IllegalArgumentException.class, () -> CardPlayRequest.singleEnemy("strike", ""));
  }

  @Test
  void shouldCreateAllEnemiesTargetingRequest() {
    CardPlayRequest request = CardPlayRequest.allEnemies("whirlwind");

    assertEquals("whirlwind", request.instanceId());
    assertEquals(CardPlayTarget.allEnemies(), request.target());
  }

  @Test
  void shouldPreserveInstanceIdExactlyWhenValid() {
    CardPlayRequest request = new CardPlayRequest("Strike_01", CardPlayTarget.self());

    assertEquals("Strike_01", request.instanceId());
  }

  @Test
  void shouldCreateEqualRequestsWithSameValues() {
    CardPlayRequest first = CardPlayRequest.singleEnemy("strike", "enemy-1");

    CardPlayRequest second = CardPlayRequest.singleEnemy("strike", "enemy-1");

    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
  }

  @Test
  void shouldNotCreateEqualRequestsWithDifferentInstanceIds() {
    CardPlayRequest first = CardPlayRequest.self("strike");

    CardPlayRequest second = CardPlayRequest.self("defend");

    assertNotEquals(first, second);
  }

  @Test
  void shouldNotCreateEqualRequestsWithDifferentTargets() {
    CardPlayRequest first = CardPlayRequest.singleEnemy("strike", "enemy-1");

    CardPlayRequest second = CardPlayRequest.singleEnemy("strike", "enemy-2");

    assertNotEquals(first, second);
  }
}
