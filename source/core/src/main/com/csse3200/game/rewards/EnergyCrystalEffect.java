package com.csse3200.game.rewards;

import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.entities.Entity;

public class EnergyCrystalEffect implements ItemEffect {
    private static final int BONUS = 1;

    @Override
    public void apply(Entity player) {
        EnergyComponent energy = player.getComponent(EnergyComponent.class);
        energy.setMaxEnergy(energy.getMaxEnergy() + BONUS);
    }
}