package com.csse3200.game.rewards;

import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.entities.Entity;

public class EnergyCrystalEffect implements ItemEffect {
  private static final int BONUS = 1;
  private static final int MAX_ENERGY_CAP = 5;

  @Override
  public void apply(Entity player) {
    EnergyComponent energy = player.getComponent(EnergyComponent.class);
    int cappedMaxEnergy = Math.min(energy.getMaxEnergy() + BONUS, MAX_ENERGY_CAP);
    energy.setMaxEnergy(cappedMaxEnergy);
  }
}
