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
    EnergyComponent energy = new EnergyComponent(10);
    assertEquals(10, energy.getCurrentEnergy());
    assertEquals(10, energy.getMaxEnergy());
  }

  @Test
  void shouldSetGetCurrentEnergy() {
    EnergyComponent energy = new EnergyComponent(10);
    energy.setCurrentEnergy(5);
    assertEquals(5, energy.getCurrentEnergy());

    energy.setCurrentEnergy(-3);
    assertEquals(0, energy.getCurrentEnergy());

    energy.setCurrentEnergy(100);
    assertEquals(10, energy.getCurrentEnergy()); // clamped to max
  }

  @Test
  void shouldSetGetMaxEnergy() {
    EnergyComponent energy = new EnergyComponent(10);
    energy.setMaxEnergy(20);
    assertEquals(20, energy.getMaxEnergy());

    energy.setMaxEnergy(-5);
    assertEquals(1, energy.getMaxEnergy()); // forced to at least 1
  }

  @Test
  void shouldCheckCanAfford() {
    EnergyComponent energy = new EnergyComponent(10);
    assertTrue(energy.canAfford(5));
    assertTrue(energy.canAfford(10));
    assertFalse(energy.canAfford(11));
  }

  @Test
  void shouldSpendEnergyWhenAffordable() {
    EnergyComponent energy = new EnergyComponent(10);
    boolean result = energy.spendEnergy(4);

    assertTrue(result);
    assertEquals(6, energy.getCurrentEnergy());
  }

  @Test
  void shouldNotSpendEnergyWhenInsufficient() {
    EnergyComponent energy = new EnergyComponent(10);
    boolean result = energy.spendEnergy(20);

    assertFalse(result);
    assertEquals(10, energy.getCurrentEnergy()); // state unchanged
  }

  @Test
  void shouldNotSpendNegativeEnergy() {
    EnergyComponent energy = new EnergyComponent(10);
    boolean result = energy.spendEnergy(-1);

    assertFalse(result);
    assertEquals(10, energy.getCurrentEnergy());
  }

  @Test
  void shouldRestoreEnergyWithoutExceedingMax() {
    EnergyComponent energy = new EnergyComponent(10);
    energy.spendEnergy(8); // currentEnergy = 2

    energy.restoreEnergy(3);
    assertEquals(5, energy.getCurrentEnergy());

    energy.restoreEnergy(100);
    assertEquals(10, energy.getCurrentEnergy()); // clamped to max
  }

  @Test
  void shouldNotRestoreNegativeEnergy() {
    EnergyComponent energy = new EnergyComponent(10);
    energy.spendEnergy(5); // currentEnergy = 5

    energy.restoreEnergy(-2);
    assertEquals(5, energy.getCurrentEnergy()); // state unchanged
  }

  @Test
  void shouldResetEnergyOnTurnStart() {
    EnergyComponent energy = new EnergyComponent(10);
    energy.spendEnergy(7); // currentEnergy = 3

    energy.onTurnStart();
    assertEquals(10, energy.getCurrentEnergy());
  }
}
