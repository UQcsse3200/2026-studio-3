package com.csse3200.game.components.battle;

import com.badlogic.gdx.Gdx;
import com.csse3200.game.GdxGame;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardPlayRequest;
import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.effects.ResolvedCardEffect;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.combat.BattleEvent;
import com.csse3200.game.components.player.PlayerIntent;
import java.util.List;

/** Connects battle UI events to valid transitions in the battle controller. */
public class BattleActions extends Component {
  static final String ATTACK_SELECTED_EVENT = "attackCardSelected";
  static final String DEFEND_SELECTED_EVENT = "defendCardSelected";
  static final String END_TURN_SELECTED_EVENT = "endTurnSelected";
  static final String PHASE_CHANGED_EVENT = "phaseChange";
  static final String PLAY_CARD_EVENT = "playCard";

  /** Fired on the battle UI entity with the latest one-line action description. */
  public static final String BATTLE_LOG_EVENT = "battleLog";

  /** Fired on the battle UI entity with the resolved enemy-facing effects, for Team 1. */
  public static final String ENEMY_EFFECTS_EVENT = "enemyEffects";

  /** Fired on the battle UI entity with the resolved player-facing effects, for Team 7. */
  public static final String PLAYER_EFFECTS_EVENT = "playerEffects";

  /** Fired on the battle UI entity when the player wins the battle. */
  public static final String BATTLE_WON_EVENT = "battleWon";

  /** Fired on the battle UI entity when the player loses the battle. */
  public static final String BATTLE_LOST_EVENT = "battleLost";

  /** Fired on the battle UI entity with the player's hand after it changes (e.g. a card played). */
  public static final String HAND_CHANGED_EVENT = "handChanged";

  private final BattleController controller;
  private final GdxGame game;
  private final CardLibrary library;

  public BattleActions(BattleController controller, GdxGame game, CardLibrary library) {
    this.controller = controller;
    this.game = game;
    this.library = library;
  }

  @Override
  public void create() {
    entity.getEvents().addListener("battle", this::onStart);
    entity.getEvents().addListener("exit", this::onExit);
    entity.getEvents().addListener(ATTACK_SELECTED_EVENT, controller::selectAttack);

    entity.getEvents().addListener(DEFEND_SELECTED_EVENT, controller::selectDefend);

    entity.getEvents().addListener(END_TURN_SELECTED_EVENT, controller::endPlayerTurn);
    entity.getEvents().addListener(PLAY_CARD_EVENT, this::onCardPlayed);
    controller.addPhaseChangeListener(
        (previousPhase, nextPhase) -> entity.getEvents().trigger(PHASE_CHANGED_EVENT, nextPhase));
    entity.getEvents().addListener("cardPlayed", this::logCardPlayed);
    entity.getEvents().addListener("endturn", this::triggerEndTurn);

    // Re-broadcast the controller's battle-loop signals as plain entity events so the battle-log
    // UI, Team 1 (enemy effects) and Team 7 (player effects) can all subscribe in one place.
    controller.addBattleLogListener(
        message -> entity.getEvents().trigger(BATTLE_LOG_EVENT, message));
    controller.addEnemyEffectsListener(this::onEnemyEffects);
    controller.addPlayerEffectsListener(this::onPlayerEffects);
    controller.addBattleEndListener(this::onBattleEnd);
    controller.addHandChangedListener(hand -> entity.getEvents().trigger(HAND_CHANGED_EVENT, hand));
  }

  private void onEnemyEffects(List<ResolvedCardEffect> effects) {
    entity.getEvents().trigger(ENEMY_EFFECTS_EVENT, effects);
  }

  private void onPlayerEffects(List<ResolvedCardEffect> effects) {
    entity.getEvents().trigger(PLAYER_EFFECTS_EVENT, effects);
  }

  /**
   * The battle finished. Announce it, then switch to the matching end screen. The screen swap is
   * deferred to the next frame so it does not run while we are still inside the battle's own
   * input/update cascade.
   */
  private void onBattleEnd(Boolean won) {
    boolean win = Boolean.TRUE.equals(won);
    entity.getEvents().trigger(win ? BATTLE_WON_EVENT : BATTLE_LOST_EVENT);

    // Report the result to the run so the map node is marked done (win) or the run stays put
    // (loss). The end screen reads this to decide whether to go back to the map or the menu.
    if (game.getRunState() != null) {
      game.getRunState().completeEncounter(win);
    }

    GdxGame.ScreenType target = win ? GdxGame.ScreenType.VICTORY : GdxGame.ScreenType.DEFEAT;
    if (Gdx.app != null) {
      Gdx.app.postRunnable(() -> game.setScreen(target));
    } else {
      game.setScreen(target);
    }
  }

  // test is card is played
  private void logCardPlayed(String cardName, String targetID) {
    System.out.println("Card played: " + cardName + " on target: " + targetID);
  }

  /**
   * A card was played (self-target on click, or dropped on a target) — see Clickable/DragNDrop and
   * EnemyDropTargetComponent for how "playCard" ends up firing with (cardId, targetId). Translates
   * the raw cardId into its display name and re-fires as "cardPlayed" for UI feedback.
   */
  private void onCardPlayed(String cardID, String targetID) {
    var optionalCard = library.getCard(cardID);
    if (optionalCard.isEmpty()) {
      return;
    }
    CardConfig cardConfig = optionalCard.get();
    CardPlayRequest request = new CardPlayRequest(cardID, targetID);
    PlayerIntent intent = classifyCard(cardConfig);

    if (controller.submitCardPlayRequest(request, intent)) {
      entity.getEvents().trigger("cardPlayed", cardConfig.name, targetID);
    }
  }

  //  private void selectAttack() {
  //    handleIfAllowed(BattleEvent.PLAYER_ATTACK_SELECTED);
  //  }
  //
  //  private void selectDefend() {
  //    handleIfAllowed(BattleEvent.PLAYER_DEFEND_SELECTED);
  //  }
  //
  private void triggerEndTurn() {
    controller.endPlayerTurn();
  }

  private void selectEndTurn() {
    if (controller.canHandle(BattleEvent.PLAYER_END_REQUESTED)) {}
  }

  private PlayerIntent classifyCard(CardConfig card) {
    if (card.type == CardType.ATTACK) {
      return PlayerIntent.ATTACK;
    }

    for (EffectConfig effect : card.effects) {
      if (effect.type == EffectType.BLOCK) {
        return PlayerIntent.DEFEND;
      }
    }

    return PlayerIntent.OTHER;
  }

  //
  //  private void handleIfAllowed(BattleEvent event) {
  //    if (controller.canHandle(event)) {
  //      controller.handle(event);
  //    }
  //  }

  private void onStart() {
    game.setScreen(GdxGame.ScreenType.BATTLE_SCREEN);
  }

  private void onExit() {
    game.setScreen(GdxGame.ScreenType.MAIN_MENU);
  }
}
