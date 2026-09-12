package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class EnergyCrystalEffectTest {

  private static final int STARTING_MAX_ENERGY = 3;

  private Entity player;
  private EnergyComponent energy;
  private EnergyCrystalEffect effect;

  @BeforeEach
  void setUp() {
    energy = new EnergyComponent(STARTING_MAX_ENERGY);
    player = new Entity().addComponent(energy);
    player.create();
    effect = new EnergyCrystalEffect();
  }

  @Test
  void increasesMaxEnergyByOne() {
    effect.apply(player);

    assertEquals(STARTING_MAX_ENERGY + 1, energy.getMaxEnergy());
  }

  @Test
  void stacksAcrossMultipleApplications() {
    effect.apply(player);
    effect.apply(player);
    effect.apply(player);

    assertEquals(STARTING_MAX_ENERGY + 3, energy.getMaxEnergy());
  }

  @Test
  void firesUpdateEnergyEventSoUiRefreshes() {
    // Regression test: setMaxEnergy previously fired an event nobody listened for
    // ("updateMaxEnergy" with one arg), so PlayerStatsDisplay's HUD never refreshed.
    // It must now fire "updateEnergy" with (currentEnergy, maxEnergy), matching what
    // PlayerStatsDisplay actually listens for.
    final int[] receivedCurrent = {-1};
    final int[] receivedMax = {-1};

    player
        .getEvents()
        .addListener(
            "updateEnergy",
            (Integer current, Integer max) -> {
              receivedCurrent[0] = current;
              receivedMax[0] = max;
            });

    effect.apply(player);

    assertEquals(STARTING_MAX_ENERGY + 1, receivedMax[0]);
    assertTrue(receivedCurrent[0] >= 0, "updateEnergy listener should have fired");
  }
}
