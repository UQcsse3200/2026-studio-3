package com.csse3200.game.cards.play.integration;

import static com.csse3200.game.components.battle.BattleActions.BATTLE_LOG_EVENT;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.play.CardPlayRequest;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.enemy.IntentEffectType;

/**
 * Connects Team 3's existing {@code playCard(cardId, targetId)} event to Team 5's unified API.
 *
 * <p>Attach this component to the same battle-flow entity that receives Team 3's card UI events.
 * The component emits one {@code cardPlayResult} event after every attempt. Team 3's battle flow
 * owns distribution of the returned enemy and player effects to Team 1 and Team 7.
 */
public final class Team3CardPlayAdapter extends Component {
  public static final String PLAY_CARD_EVENT = "playCard";
  public static final String CARD_PLAY_RESULT_EVENT = "cardPlayed";

  private final CardService cardService;
  private final BattleController battleController;

  /** Creates an adapter that returns results to Team 3 without applying external state changes. */
  public Team3CardPlayAdapter(CardService cardService, BattleController battleController) {
    if (cardService == null) {
      throw new IllegalArgumentException("Card service cannot be null");
    }
    if (battleController == null) {
      throw new IllegalArgumentException("Card play service cannot be null");
    }
    this.cardService = cardService;
    this.battleController = battleController;
  }

  @Override
  public void create() {
    entity.getEvents().addListener(PLAY_CARD_EVENT, this::onCardPlayed);
    battleController.addCardPlayedListener(
        (cardId, targetId) -> {
          entity.getEvents().trigger(CARD_PLAY_RESULT_EVENT, cardId, targetId);
        });
  }

  private void onCardPlayed(String cardId, String targetId) {
    if (playerIsBlockedFromPlayingCards()) {
      return;
    }
    CardPlayRequest request = toRequest(cardId, targetId);
    battleController.submitCardPlayRequest(request);
  }

  // test that card is played
  private void logCardPlayed(String cardName, String targetID) {
    System.out.println("Card played: " + cardName + " on target: " + targetID);
  }

  private CardPlayRequest toRequest(String cardId, String targetId) {
    CardConfig card = cardService.getCard(cardId).orElse(null);
    if (card == null) {
      return CardPlayRequest.self(cardId);
    }
    if (card.target == TargetType.SELF) {
      return CardPlayRequest.self(cardId);
    }
    if (card.target == TargetType.ALL_ENEMIES) {
      return CardPlayRequest.allEnemies(cardId);
    }
    return CardPlayRequest.singleEnemy(cardId, targetId);
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
