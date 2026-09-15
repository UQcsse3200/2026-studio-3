package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.cards.play.CardPlayRequest;
import org.junit.jupiter.api.Test;

// Use this class
public class CardPlayRequestTest {
  @Test
  void shouldRejectBlankCardID() {
    assertThrows(
        IllegalArgumentException.class, () -> CardPlayRequest.singleEnemy("", "test_enemy"));
  }

  @Test
  void shouldRejectBlankTargetId() {
    assertThrows(
        IllegalArgumentException.class, () -> CardPlayRequest.singleEnemy("strike-instance", ""));
  }
}
