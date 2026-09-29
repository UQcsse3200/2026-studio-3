package com.csse3200.game.components.enemy.Memory;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.combat.BattlePhase;
import java.util.Objects;

/**
 * Observes the player's combat actions and writes the resulting information into {@link
 * EnemyMemoryComponent}.
 *
 * <p>This component is attached to the player entity. It listens to successful card-play and
 * phase-change events, resolves played card instances and sends simplified player behaviour to the
 * enemy memory system.
 *
 * <p>The tracker does not store long-term combat behaviour. Persistent per-combat information
 * belongs to {@link EnemyMemoryComponent}.
 */
public class PlayerTrackerComponent extends Component {
  private EnemyMemoryComponent enemyMemory;

  private BattleController battleController;
  private CardService cardService;
  private BattleDeck battleDeck;

  private boolean connected;

  /** Finds the EnemyMemoryComponent attached to the same player entity. */
  @Override
  public void create() {
    super.create();

    enemyMemory = entity.getComponent(EnemyMemoryComponent.class);

    if (enemyMemory == null) {
      throw new IllegalStateException(
          "PlayerTrackerComponent requires EnemyMemoryComponent " + "on the same entity");
    }
  }

  /**
   * Connects this tracker to the current battle.
   *
   * <p>This should be called by BattleScreen after the player and BattleController have both been
   * created.
   *
   * @param battleController source of battle events
   * @param cardService source of card definitions
   * @param battleDeck source of runtime card instances
   */
  public void connect(
      BattleController battleController, CardService cardService, BattleDeck battleDeck) {
    if (connected) {
      throw new IllegalStateException("PlayerTrackerComponent is already connected");
    }

    BattleController checkedController =
        Objects.requireNonNull(battleController, "battleController cannot be null");

    CardService checkedCardService =
        Objects.requireNonNull(cardService, "cardService cannot be null");

    BattleDeck checkedBattleDeck = Objects.requireNonNull(battleDeck, "battleDeck cannot be null");

    this.battleController = checkedController;
    this.cardService = checkedCardService;
    this.battleDeck = checkedBattleDeck;

    this.battleController.addCardPlayedListener(this::onCardPlayed);

    this.battleController.addPhaseChangeListener(this::onPhaseChanged);

    connected = true;
  }

  /**
   * Handles one successfully played card.
   *
   * @param instanceId runtime ID of the played card
   * @param targetId selected target ID; not needed by player memory
   */
  private void onCardPlayed(String instanceId, String targetId) {
    CardConfig config = findCardConfig(instanceId);

    if (config == null || config.type == null) {
      return;
    }

    enemyMemory.recordCardPlayed(config.type, grantsPositiveBlock(config));
  }

  /**
   * Detects when the real player turn ends.
   *
   * <p>A temporary transition into CARD_RESOLVING does not end the turn.
   *
   * @param previousPhase phase being exited
   * @param nextPhase phase being entered
   */
  private void onPhaseChanged(BattlePhase previousPhase, BattlePhase nextPhase) {
    boolean leavingPlayerTurn =
        previousPhase == BattlePhase.PLAYER_TURN
            && nextPhase != BattlePhase.PLAYER_TURN
            && nextPhase != BattlePhase.CARD_RESOLVING;

    if (leavingPlayerTurn) {
      enemyMemory.settlePlayerTurn();
    }
  }

  /**
   * Resolves a runtime card instance into its card configuration.
   *
   * @param instanceId runtime card-instance ID
   * @return matching card configuration, or null if unresolved
   */
  private CardConfig findCardConfig(String instanceId) {
    if (instanceId == null || instanceId.isBlank()) {
      return null;
    }

    for (CardInstance instance : battleDeck.getAllInstances()) {
      if (instance.instanceId().equals(instanceId)) {
        return cardService.getCard(instance.cardId()).orElse(null);
      }
    }

    return null;
  }

  /**
   * Checks whether a card contains a positive block effect.
   *
   * @param config card configuration
   * @return true when at least one effect grants positive block
   */
  private boolean grantsPositiveBlock(CardConfig config) {
    if (config.effects == null) {
      return false;
    }

    for (EffectConfig effect : config.effects) {
      if (effect != null && effect.type == EffectType.BLOCK && effect.value > 0) {
        return true;
      }
    }

    return false;
  }
}
