package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.effects.ResolvedCardEffect;
import com.csse3200.game.cards.play.integration.Team7PlayerStateAdapter;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.entities.Entity;
import java.util.List;
import org.junit.jupiter.api.Test;

class CurseItemEffectsTest {

  private static ResolvedCardEffect selfEffect(EffectType type, int value) {
    return new ResolvedCardEffect("test-card", type, TargetType.SELF, value, 0, 0);
  }

  @Test
  void ashenEmberReducesMaxEnergyButNeverBelowOne() {
    EnergyComponent energy = new EnergyComponent(3);
    Entity player = new Entity().addComponent(energy);

    new AshenEmberEffect().apply(player);
    assertEquals(2, energy.getMaxEnergy());

    new AshenEmberEffect().apply(player);
    new AshenEmberEffect().apply(player);
    assertEquals(1, energy.getMaxEnergy());
  }

  @Test
  void itemEffectApplierDispatchesAshenEmber() {
    EnergyComponent energy = new EnergyComponent(3);
    Entity player = new Entity().addComponent(energy);

    ItemEffectApplier.applyItemEffect(ItemType.ASHEN_EMBER, player);

    assertEquals(2, energy.getMaxEnergy());
  }

  @Test
  void brandOfTheDamnedReducesEachStrengthGainToAMinimumOfZero() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 5);
    EnergyComponent energy = new EnergyComponent(3);
    Entity player = new Entity().addComponent(stats).addComponent(energy);
    Team7PlayerStateAdapter adapter = new Team7PlayerStateAdapter(energy, stats);

    new BrandOfTheDamnedEffect().apply(player);
    adapter.applyPlayerEffects(List.of(selfEffect(EffectType.STRENGTH, 2)));
    assertEquals(1, adapter.statusValue(EffectType.STRENGTH));

    // Blessed Crest grants exactly 1, so the curse cancels it completely.
    new WarriorsCrestEffect().apply(player);
    assertEquals(1, adapter.statusValue(EffectType.STRENGTH));
  }

  @Test
  void shatteredAegisReducesEachBlockGainToAMinimumOfZero() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 5);
    EnergyComponent energy = new EnergyComponent(3);
    Entity player = new Entity().addComponent(stats).addComponent(energy);
    Team7PlayerStateAdapter adapter = new Team7PlayerStateAdapter(energy, stats);

    new ShatteredAegisEffect().apply(player);
    adapter.applyPlayerEffects(List.of(selfEffect(EffectType.BLOCK, 5)));
    assertEquals(4, stats.getBlock());

    adapter.applyPlayerEffects(List.of(selfEffect(EffectType.BLOCK, 1)));
    assertEquals(4, stats.getBlock());
  }

  @Test
  void curseCopiesStackTheirPenalties() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 5);
    Entity player = new Entity().addComponent(stats);

    new BloodOathSigilEffect().apply(player);
    new BloodOathSigilEffect().apply(player);
    new ShatteredAegisEffect().apply(player);
    new ShatteredAegisEffect().apply(player);

    assertEquals(2, CursePenalties.getPenalty(stats, CursePenalties.BLOOD_OATH));
    assertEquals(0, CursePenalties.reduce(stats, CursePenalties.BLOCK_PENALTY, 2));
    assertEquals(3, CursePenalties.reduce(stats, CursePenalties.BLOCK_PENALTY, 5));
  }

  @Test
  void repeatedBrandAndAegisCopiesReduceGainsButNeverBelowZero() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 5);
    Entity player = new Entity().addComponent(stats);

    new BrandOfTheDamnedEffect().apply(player);
    new BrandOfTheDamnedEffect().apply(player);
    new ShatteredAegisEffect().apply(player);
    new ShatteredAegisEffect().apply(player);

    assertEquals(2, CursePenalties.getPenalty(stats, CursePenalties.STRENGTH_PENALTY));
    assertEquals(2, CursePenalties.getPenalty(stats, CursePenalties.BLOCK_PENALTY));
    assertEquals(1, CursePenalties.reduce(stats, CursePenalties.STRENGTH_PENALTY, 3));
    assertEquals(0, CursePenalties.reduce(stats, CursePenalties.BLOCK_PENALTY, 1));
  }

  @Test
  void repeatedBloodOathCopiesIncreaseTheStartOfTurnPenalty() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 5);
    Entity player = new Entity().addComponent(stats);

    new BloodOathSigilEffect().apply(player);
    new BloodOathSigilEffect().apply(player);

    assertEquals(2, CursePenalties.getPenalty(stats, CursePenalties.BLOOD_OATH));
    assertEquals(
        4,
        CursePenalties.getPenalty(stats, CursePenalties.BLOOD_OATH)
            * CursePenalties.BLOOD_OATH_DAMAGE_PER_COPY);
  }

  @Test
  void penaltiesDoNothingWhenTheCurseIsNotOwned() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 5);

    assertEquals(0, CursePenalties.getPenalty(stats, CursePenalties.BLOOD_OATH));
    assertEquals(5, CursePenalties.reduce(stats, CursePenalties.STRENGTH_PENALTY, 5));
  }

  @Test
  void curseEffectsRequireCombatStats() {
    Entity player = new Entity();

    assertThrows(IllegalArgumentException.class, () -> new BrandOfTheDamnedEffect().apply(player));
    assertThrows(IllegalArgumentException.class, () -> new ShatteredAegisEffect().apply(player));
    assertThrows(IllegalArgumentException.class, () -> new BloodOathSigilEffect().apply(player));
  }
}
