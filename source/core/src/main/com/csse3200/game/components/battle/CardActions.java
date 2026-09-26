package com.csse3200.game.components.battle;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.combat.BattlePhase;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.entities.Entity;

/**
 * Watches the player's energy and ends their turn automatically once it hits zero, and re-shows
 * the hand ("up") when a fresh player turn starts with energy still available. Lives alongside
 * {@link BattleActions} on the battle UI entity rather than extending it, since it only needs the
 * phase-change signal and the player's own energy events, not any of BattleActions's UI-wiring
 * responsibilities (End Turn button, battle log, enemy/player effects, etc).
 */
public class CardActions extends Component {
    private final BattleController controller;
    private final Entity player;

    public CardActions(BattleController controller, Entity player) {
        this.controller = controller;
        this.player = player;
    }

    @Override
    public void create() {
        controller.addPhaseChangeListener(this::onPhaseChange);
        player.getEvents().addListener("updateEnergy", this::onEnergyChanged);
    }

    private void onPhaseChange(BattlePhase previousPhase, BattlePhase nextPhase) {
        if (nextPhase != BattlePhase.PLAYER_TURN) {
            return;
        }
        if (player.getComponent(EnergyComponent.class).getCurrentEnergy() == 0) {
            entity.getEvents().trigger("down");
            entity.getEvents().trigger("endTurn");
        } else {
            entity.getEvents().trigger("up");
        }
    }

    /**
     * Dims the hand immediately while a card is still resolving mid-turn. The actual auto-end-turn
     * happens in {@link #onPhaseChange}, once the FSM is actually back in {@code PLAYER_TURN} —
     * this fires while the battle is still in {@code CARD_RESOLVING}, too early for {@code
     * BattleController.endPlayerTurn()} to succeed.
     */
    private void onEnergyChanged(int currentEnergy, int maxEnergy) {
        if (currentEnergy == 0) {
            entity.getEvents().trigger("down");
        } else if (controller.isPlayerTurn()) {
            entity.getEvents().trigger("up");
        }
    }
}
