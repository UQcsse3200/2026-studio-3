package com.csse3200.game.components.bestiary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.bestiary.BestiaryEntryView;
import com.csse3200.game.bestiary.BestiaryService;
import com.csse3200.game.bestiary.BestiaryUnlockState;
import com.csse3200.game.entities.configs.EnemyConfig;
import com.csse3200.game.entities.configs.EnemyConfigs;
import com.csse3200.game.entities.configs.EnemyTier;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class BestiaryDisplayTest {
  private final BestiaryEntryView locked = createView(BestiaryUnlockState.LOCKED);
  private final BestiaryEntryView encountered = createView(BestiaryUnlockState.ENCOUNTERED);
  private final BestiaryEntryView defeated = createView(BestiaryUnlockState.DEFEATED);

  @Test
  void shouldKeepLockedEntryDataObscuredAsDefenceInDepth() {
    assertEquals("???", BestiaryDisplay.visibleName(locked));
    assertEquals(
        "Encounter this enemy to reveal its record.", BestiaryDisplay.descriptionFor(locked));
    assertEquals("HP  ???     ATTACK  ???     ARMOUR  ???", BestiaryDisplay.statsFor(locked));
  }

  @Test
  void shouldKeepEncounteredCombatDetailsHidden() {
    assertEquals("Enemy", BestiaryDisplay.visibleName(encountered));
    assertEquals(
        "Defeat this enemy to reveal its complete record.",
        BestiaryDisplay.descriptionFor(encountered));
    assertEquals("HP  ???     ATTACK  ???     ARMOUR  ???", BestiaryDisplay.statsFor(encountered));
  }

  @Test
  void shouldShowDefeatedEnemyDetails() {
    assertEquals("Enemy description", BestiaryDisplay.descriptionFor(defeated));
    assertEquals(
        "HP  24     ATTACK  6     ARMOUR  2\nBEHAVIOUR  CYCLE ATTACK DEFEND",
        BestiaryDisplay.statsFor(defeated));
  }

  @Test
  void shouldUseFallbackWhenDefeatedDescriptionIsMissing() {
    BestiaryEntryView noDescription =
        new BestiaryEntryView(
            "enemy",
            EnemyTier.NORMAL,
            BestiaryUnlockState.DEFEATED,
            "Enemy",
            Optional.of("images/enemies/default.atlas"),
            Optional.empty(),
            OptionalInt.of(24),
            OptionalInt.of(6),
            OptionalInt.of(2),
            Optional.of("cycle_attack_defend"));

    assertEquals("No description available.", BestiaryDisplay.descriptionFor(noDescription));
  }

  @Test
  void shouldHideUndiscoveredEntryThenRevealItOnEncounter() {
    EnemyConfig config = new EnemyConfig();
    config.id = "enemy";
    config.name = "Enemy";
    config.tier = EnemyTier.NORMAL;
    config.health = 24;
    EnemyConfigs configs = new EnemyConfigs();
    configs.enemies = new EnemyConfig[] {config};
    BestiaryService service = new BestiaryService(configs);

    RenderService renderService = mock(RenderService.class);
    when(renderService.getStage()).thenReturn(mock(Stage.class));
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerResourceService(mock(ResourceService.class));

    BestiaryDisplay display = new BestiaryDisplay(service, () -> {});
    display.create();
    assertNull(display.getDisplayedEntry());

    service.recordEncountered("enemy");
    assertEquals(BestiaryUnlockState.ENCOUNTERED, display.getDisplayedEntry().unlockState());

    display.dispose();
    service.recordDefeated("enemy");
    assertEquals(BestiaryUnlockState.ENCOUNTERED, display.getDisplayedEntry().unlockState());
  }

  private BestiaryEntryView createView(BestiaryUnlockState state) {
    boolean encountered = state.isAtLeast(BestiaryUnlockState.ENCOUNTERED);
    boolean defeatedState = state == BestiaryUnlockState.DEFEATED;
    return new BestiaryEntryView(
        "enemy",
        EnemyTier.NORMAL,
        state,
        encountered ? "Enemy" : "???",
        encountered ? Optional.of("images/enemies/default.atlas") : Optional.empty(),
        defeatedState ? Optional.of("Enemy description") : Optional.empty(),
        defeatedState ? OptionalInt.of(24) : OptionalInt.empty(),
        defeatedState ? OptionalInt.of(6) : OptionalInt.empty(),
        defeatedState ? OptionalInt.of(2) : OptionalInt.empty(),
        defeatedState ? Optional.of("cycle_attack_defend") : Optional.empty());
  }
}
