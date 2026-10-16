package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class EnergyComponentTest {

  @Test
  void shouldInitializeWithFullEnergy() {
    EnergyComponent energy = new EnergyComponent(5);
    assertEquals(5, energy.getCurrentEnergy());
    assertEquals(5, energy.getMaxEnergy());
  }

  @Test
  void shouldSetGetCurrentEnergy() {
    EnergyComponent energy = new EnergyComponent(5);
    energy.setCurrentEnergy(3);
    assertEquals(3, energy.getCurrentEnergy());

    energy.setCurrentEnergy(-3);
    assertEquals(0, energy.getCurrentEnergy());

    energy.setCurrentEnergy(100);
    assertEquals(5, energy.getCurrentEnergy()); // clamped to max
  }

  @Test
  void shouldSetGetMaxEnergy() {
    EnergyComponent energy = new EnergyComponent(3);
    energy.setMaxEnergy(4);
    assertEquals(4, energy.getMaxEnergy());

    energy.setMaxEnergy(20);
    assertEquals(EnergyComponent.MAX_ENERGY_CAP, energy.getMaxEnergy()); // capped at 5

    energy.setMaxEnergy(-5);
    assertEquals(1, energy.getMaxEnergy()); // forced to at least 1
  }

  @Test
  void constructorClampsMaxEnergyToTheCap() {
    EnergyComponent energy = new EnergyComponent(6);
    assertEquals(EnergyComponent.MAX_ENERGY_CAP, energy.getMaxEnergy());
    assertEquals(EnergyComponent.MAX_ENERGY_CAP, energy.getCurrentEnergy());

    EnergyComponent minimum = new EnergyComponent(0);
    assertEquals(1, minimum.getMaxEnergy());
  }

  @Test
  void shouldCheckCanAfford() {
    EnergyComponent energy = new EnergyComponent(5);
    assertTrue(energy.canAfford(3));
    assertTrue(energy.canAfford(5));
    assertFalse(energy.canAfford(6));
  }

  @Test
  void shouldSpendEnergyWhenAffordable() {
    EnergyComponent energy = new EnergyComponent(5);
    boolean result = energy.spendEnergy(2);

    assertTrue(result);
    assertEquals(3, energy.getCurrentEnergy());
  }

  @Test
  void shouldNotSpendEnergyWhenInsufficient() {
    EnergyComponent energy = new EnergyComponent(5);
    boolean result = energy.spendEnergy(6);

    assertFalse(result);
    assertEquals(5, energy.getCurrentEnergy()); // state unchanged
  }

  @Test
  void shouldNotSpendNegativeEnergy() {
    EnergyComponent energy = new EnergyComponent(5);
    boolean result = energy.spendEnergy(-1);

    assertFalse(result);
    assertEquals(5, energy.getCurrentEnergy());
  }

  @Test
  void shouldRestoreEnergyWithoutExceedingMax() {
    EnergyComponent energy = new EnergyComponent(5);
    energy.spendEnergy(4); // currentEnergy = 1

    energy.restoreEnergy(2);
    assertEquals(3, energy.getCurrentEnergy());

    energy.restoreEnergy(100);
    assertEquals(5, energy.getCurrentEnergy()); // clamped to max
  }

  @Test
  void shouldNotRestoreNegativeEnergy() {
    EnergyComponent energy = new EnergyComponent(5);
    energy.spendEnergy(2); // currentEnergy = 3

    energy.restoreEnergy(-2);
    assertEquals(3, energy.getCurrentEnergy()); // state unchanged
  }

  @Test
  void shouldResetEnergyOnTurnStart() {
    EnergyComponent energy = new EnergyComponent(5);
    energy.spendEnergy(4); // currentEnergy = 1

    energy.onTurnStart();
    assertEquals(5, energy.getCurrentEnergy());
  }
}
