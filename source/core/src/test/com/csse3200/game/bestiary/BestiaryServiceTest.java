package com.csse3200.game.bestiary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.csse3200.game.entities.configs.EnemyConfig;
import com.csse3200.game.entities.configs.EnemyConfigs;
import com.csse3200.game.entities.configs.EnemyTier;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class BestiaryServiceTest {
  @Test
  void shouldLoadDefaultRosterWithResolvedSpritePaths() {
    BestiaryService service = BestiaryService.loadDefault();

    assertTrue(service.contains("lesser_shade"));
    assertTrue(service.recordEncountered("lesser_shade"));
    assertEquals(
        "images/enemies/lesser_shade.atlas",
        service.getEntry("lesser_shade").orElseThrow().sprite().orElseThrow());
  }

  @Test
  void shouldStartEntriesLockedInStableTierAndIdOrder() {
    BestiaryService service = createService();

    List<BestiaryEntryView> entries = service.getEntries();

    assertEquals(List.of("alpha", "shade", "knight", "guardian"), ids(entries));
    assertTrue(
        entries.stream().allMatch(entry -> entry.unlockState() == BestiaryUnlockState.LOCKED));
    assertTrue(entries.stream().allMatch(entry -> entry.displayName().equals("???")));
    assertTrue(entries.stream().allMatch(entry -> entry.sprite().isEmpty()));
    assertTrue(entries.stream().allMatch(entry -> entry.health().isEmpty()));
    assertThrows(UnsupportedOperationException.class, entries::clear);
  }

  @Test
  void shouldRevealIdentityOnEncounterButKeepDetailsHidden() {
    BestiaryService service = createService();

    assertTrue(service.recordEncountered("shade"));
    BestiaryEntryView entry = service.getEntry("shade").orElseThrow();

    assertEquals(BestiaryUnlockState.ENCOUNTERED, entry.unlockState());
    assertEquals("Lesser Shade", entry.displayName());
    assertEquals("images/enemies/shade.atlas", entry.sprite().orElseThrow());
    assertTrue(entry.description().isEmpty());
    assertTrue(entry.health().isEmpty());
    assertTrue(entry.baseAttack().isEmpty());
    assertTrue(entry.armour().isEmpty());
    assertTrue(entry.behaviour().isEmpty());
    assertFalse(entry.hasFullDetails());
  }

  @Test
  void shouldRevealFullDetailsOnDefeat() {
    BestiaryService service = createService();

    assertTrue(service.recordDefeated("shade"));
    BestiaryEntryView entry = service.getEntry("shade").orElseThrow();

    assertEquals(BestiaryUnlockState.DEFEATED, entry.unlockState());
    assertEquals("A creature made of living darkness.", entry.description().orElseThrow());
    assertEquals(24, entry.health().orElseThrow());
    assertEquals(5, entry.baseAttack().orElseThrow());
    assertEquals(2, entry.armour().orElseThrow());
    assertEquals("cycle_attack_defend", entry.behaviour().orElseThrow());
    assertTrue(entry.hasFullDetails());
  }

  @Test
  void shouldNeverRegressOrRepeatProgress() {
    BestiaryService service = createService();

    assertTrue(service.recordEncountered("shade"));
    assertFalse(service.recordEncountered("shade"));
    assertTrue(service.recordDefeated("shade"));
    assertFalse(service.recordEncountered("shade"));
    assertFalse(service.recordDefeated("shade"));
    assertEquals(
        BestiaryUnlockState.DEFEATED, service.getEntry("shade").orElseThrow().unlockState());
  }

  @Test
  void shouldNotifyPresentationCodeOnlyForActualTransitions() {
    BestiaryService service = createService();
    @SuppressWarnings("unchecked")
    EventListener1<BestiaryEntryView> listener =
        (EventListener1<BestiaryEntryView>) mock(EventListener1.class);
    service.getEvents().addListener(BestiaryService.ENTRY_UPDATED_EVENT, listener);

    service.recordEncountered("shade");
    service.recordEncountered("shade");
    service.recordDefeated("shade");

    ArgumentCaptor<BestiaryEntryView> updates = ArgumentCaptor.forClass(BestiaryEntryView.class);
    verify(listener, times(2)).handle(updates.capture());
    assertEquals(
        List.of(BestiaryUnlockState.ENCOUNTERED, BestiaryUnlockState.DEFEATED),
        updates.getAllValues().stream().map(BestiaryEntryView::unlockState).toList());
  }

  @Test
  void shouldFilterByTierAndIgnoreUnknownIds() {
    BestiaryService service = createService();

    assertEquals(List.of("alpha", "shade"), ids(service.getEntriesByTier(EnemyTier.NORMAL)));
    assertEquals(List.of("knight"), ids(service.getEntriesByTier(EnemyTier.ELITE)));
    assertEquals(List.of("guardian"), ids(service.getEntriesByTier(EnemyTier.BOSS)));
    assertTrue(service.getEntriesByTier(null).isEmpty());
    assertFalse(service.contains("missing"));
    assertTrue(service.getEntry("missing").isEmpty());
    assertFalse(service.recordEncountered("missing"));
    assertFalse(service.recordDefeated(null));
  }

  @Test
  void shouldRejectNullRoster() {
    assertThrows(IllegalArgumentException.class, () -> new BestiaryService(null));
  }

  private static BestiaryService createService() {
    EnemyConfigs configs = new EnemyConfigs();
    configs.enemies =
        new EnemyConfig[] {
          enemy("guardian", "Abyss Guardian", EnemyTier.BOSS, 120, 15, 8),
          enemy("shade", "Lesser Shade", EnemyTier.NORMAL, 24, 5, 2),
          enemy("knight", "Void Knight", EnemyTier.ELITE, 72, 10, 5),
          enemy("alpha", "Alpha Shade", EnemyTier.NORMAL, 30, 6, 1)
        };
    return new BestiaryService(configs, Map.of("shade", "A creature made of living darkness."));
  }

  private static EnemyConfig enemy(
      String id, String name, EnemyTier tier, int health, int attack, int armour) {
    EnemyConfig config = new EnemyConfig();
    config.id = id;
    config.name = name;
    config.tier = tier;
    config.health = health;
    config.baseAttack = attack;
    config.armour = armour;
    config.behaviour = "cycle_attack_defend";
    config.sprite = "images/enemies/" + id + ".atlas";
    return config;
  }

  private static List<String> ids(List<BestiaryEntryView> entries) {
    return entries.stream().map(BestiaryEntryView::enemyId).toList();
  }
}
