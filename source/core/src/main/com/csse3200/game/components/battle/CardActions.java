package com.csse3200.game.components.battle;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.combat.BattlePhase;

/** Keeps the hand visible until the turn ends and restores it after enemy reveals finish. */
public class CardActions extends Component {
  @Override
  public void create() {
    entity.getEvents().addListener(BattleActions.PHASE_CHANGED_EVENT, this::onPhaseChange);
  }

  private void onPhaseChange(BattlePhase phase) {
    switch (phase) {
      case PLAYER_TURN -> entity.getEvents().trigger("up");
      case PLAYER_END, VICTORY, DEFEAT -> entity.getEvents().trigger("down");
      default -> {
        // Card resolution and enemy phases do not change the hand's visibility.
      }
    }
  }
}
