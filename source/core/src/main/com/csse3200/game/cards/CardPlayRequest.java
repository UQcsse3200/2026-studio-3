package com.csse3200.game.cards;

public record CardPlayRequest(String cardID, String targetID) {
  public CardPlayRequest {
    if (cardID == null || cardID.isBlank()) {
      throw new IllegalArgumentException("Card id cant be blank bro chacho");
    }
    if (targetID == null || targetID.isBlank()) {
      throw new IllegalArgumentException("target id cant be blank brochacho");
    }
  }
}
