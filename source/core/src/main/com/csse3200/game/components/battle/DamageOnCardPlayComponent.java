package com.csse3200.game.components.battle;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.StatusEffect;
import java.util.Objects;

/** Applies the active damage-on-card-play effect after a successful play. */
public class DamageOnCardPlayComponent extends Component {
  private static final String EFFECT_TYPE = "DAMAGE_ON_CARD_PLAY";

  private final CombatStatsComponent playerStats;

  public DamageOnCardPlayComponent(CombatStatsComponent playerStats) {
    this.playerStats = Objects.requireNonNull(playerStats);
  }

  @Override
  public void create() {
    entity.getEvents().addListener("cardPlayed", this::onCardPlayed);
  }

  private void onCardPlayed(String cardName, String targetId) {
    StatusEffect effect = playerStats.getStatusEffect(EFFECT_TYPE);

    if (effect == null || effect.getValue() <= 0 || playerStats.isDead()) {
      return;
    }

    int healthBefore = playerStats.getHealth();
    playerStats.takeDamage(effect.getValue());
    int healthLost = healthBefore - playerStats.getHealth();

    entity
        .getEvents()
        .trigger(BattleActions.BATTLE_LOG_EVENT, "Damage on play: you lost " + healthLost + " HP.");
  }
}
