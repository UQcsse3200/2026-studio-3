package com.csse3200.game.cards.play.integration;

import static com.csse3200.game.components.battle.BattleActions.BATTLE_LOG_EVENT;

import com.csse3200.game.cards.play.CardPlayRequest;
import com.csse3200.game.cards.play.CardPlayService;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.enemy.IntentEffectType;

/**
 * Connects Team 3's existing {@code playCard(instanceId, targetId)} event to Team 5's unified API.
 *
 * <p>Attach this component to the same battle-flow entity that receives Team 3's card UI events.
 * The component emits one {@code cardPlayResult} event after every attempt. Team 3's battle flow
 * owns distribution of the returned enemy and player effects to Team 1 and Team 7.
 */
public final class Team3CardPlayAdapter extends Component {
  public static final String PLAY_CARD_EVENT = "playCard";
  public static final String CARD_PLAY_RESULT_EVENT = "cardPlayed";

  private final CardPlayService cardPlayService;
  private final BattleController battleController;

  /** Creates an adapter that returns results to Team 3 without applying external state changes. */
  public Team3CardPlayAdapter(CardPlayService cardPlayService, BattleController battleController) {
    if (cardPlayService == null) {
      throw new IllegalArgumentException("Card play service cannot be null");
    }
    if (battleController == null) {
      throw new IllegalArgumentException("Battle controller cannot be null");
    }
    this.cardPlayService = cardPlayService;
    this.battleController = battleController;
  }

  @Override
  public void create() {
    entity.getEvents().addListener(PLAY_CARD_EVENT, this::onCardPlayed);
    battleController.addCardPlayedListener(
        (instanceId, targetId) -> {
          entity.getEvents().trigger(CARD_PLAY_RESULT_EVENT, instanceId, targetId);
        });
  }

  private void onCardPlayed(String instanceId, String targetId) {
    if (playerIsBlockedFromPlayingCards()) {
      return;
    }
    CardPlayRequest request = toRequest(instanceId, targetId);
    battleController.submitCardPlayRequest(request);
  }

  private CardPlayRequest toRequest(String instanceId, String targetId) {
    return cardPlayService
        .resolveInHand(instanceId)
        .map(card -> CardPlayRequest.fromUi(instanceId, card.target(), targetId))
        .orElseGet(() -> CardPlayRequest.self(instanceId));
  }

  /**
   * Whether a status effect currently prevents the player from playing cards.
   *
   * <p>Hook point for Team 1's boss mechanics. Rejecting here reuses the existing rejection path:
   * "cardPlayed" is not fired, energy is not spent and the card stays in hand, exactly as when the
   * controller declines the request.
   *
   * @return true if the play should be rejected before reaching the controller
   */
  private boolean playerIsBlockedFromPlayingCards() {
    if (!battleController.playerHasStatusEffect(IntentEffectType.SILENCE.name())) {
      return false;
    }

    entity.getEvents().trigger(BATTLE_LOG_EVENT, "You are silenced and cannot play cards.");
    return true;
  }
}
