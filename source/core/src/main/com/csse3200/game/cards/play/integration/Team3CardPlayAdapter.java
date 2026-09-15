package com.csse3200.game.cards.play.integration;

import com.csse3200.game.cards.play.CardPlayRequest;
import com.csse3200.game.cards.play.CardPlayService;
import com.csse3200.game.components.Component;

/**
 * Connects Team 3's existing {@code playCard(instanceId, targetId)} event to Team 5's unified API.
 *
 * <p>Attach this component to the same battle-flow entity that receives Team 3's card UI events.
 * The component emits one {@code cardPlayResult} event after every attempt. Team 3's battle flow
 * owns distribution of the returned enemy and player effects to Team 1 and Team 7.
 */
public final class Team3CardPlayAdapter extends Component {
  public static final String PLAY_CARD_EVENT = "playCard";
  public static final String CARD_PLAY_RESULT_EVENT = "cardPlayResult";

  private final CardPlayService cardPlayService;

  /** Creates an adapter that returns results to Team 3 without applying external state changes. */
  public Team3CardPlayAdapter(CardPlayService cardPlayService) {
    if (cardPlayService == null) {
      throw new IllegalArgumentException("Card play service cannot be null");
    }
    this.cardPlayService = cardPlayService;
  }

  @Override
  public void create() {
    entity.getEvents().addListener(PLAY_CARD_EVENT, this::onCardPlayed);
  }

  private void onCardPlayed(String instanceId, String targetId) {
    if (targetId == null || targetId.isBlank()) return;
    var selected = cardPlayService.resolveInHand(instanceId);
    CardPlayRequest request =
        selected
            .map(card -> CardPlayRequest.fromUi(instanceId, card.target(), targetId))
            .orElseGet(() -> CardPlayRequest.self(instanceId));
    entity.getEvents().trigger(CARD_PLAY_RESULT_EVENT, cardPlayService.playCard(request));
  }
}
