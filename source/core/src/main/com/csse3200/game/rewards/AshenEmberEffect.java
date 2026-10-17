package com.csse3200.game.rewards;

import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.entities.Entity;

/** Curse effect: reduces max energy by 1, never below 1. */
public class AshenEmberEffect implements ItemEffect {
  private static final int PENALTY = 1;
  private static final int MIN_MAX_ENERGY = 1;

  @Override
  public void apply(Entity player) {
    EnergyComponent energy = player.getComponent(EnergyComponent.class);
    int reducedMaxEnergy = Math.max(energy.getMaxEnergy() - PENALTY, MIN_MAX_ENERGY);
    energy.setMaxEnergy(reducedMaxEnergy);
  }
}
