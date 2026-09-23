package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.events.listeners.EventListener1;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CardDiscoveryServiceTest {
  @Test
  void shouldStartEveryEntryLockedInConfigurationOrder() {
    CardDiscoveryService service = createService();

    assertEquals(List.of("strike", "defend"), List.copyOf(service.getProgressSnapshot().keySet()));
    assertTrue(
        service.getEntries().stream()
            .allMatch(entry -> entry.unlockState() == CardUnlockState.LOCKED));
  }

  @Test
  void shouldRecordSeenWithoutRegressingOrRepeatingProgress() {
    CardDiscoveryService service = createService();

    assertTrue(service.recordSeen("strike"));
    assertFalse(service.recordSeen("strike"));
    assertEquals(CardUnlockState.SEEN, service.getEntry("strike").orElseThrow().unlockState());
  }

  @Test
  void shouldIgnoreUnknownAndBlankIds() {
    CardDiscoveryService service = createService();

    assertFalse(service.recordSeen("missing"));
    assertFalse(service.recordSeen(" "));
    assertFalse(service.recordSeen(null));
    assertTrue(
        service.getProgressSnapshot().values().stream()
            .allMatch(state -> state == CardUnlockState.LOCKED));
  }

  @Test
  void shouldExposeStableUnmodifiableProgressSnapshot() {
    CardDiscoveryService service = createService();
    service.recordSeen("defend");

    Map<String, CardUnlockState> snapshot = service.getProgressSnapshot();

    assertEquals(List.of("strike", "defend"), List.copyOf(snapshot.keySet()));
    assertEquals(CardUnlockState.SEEN, snapshot.get("defend"));
    assertThrows(
        UnsupportedOperationException.class, () -> snapshot.put("strike", CardUnlockState.SEEN));
  }

  @Test
  void shouldReplaceProgressAndDefaultOmittedIdsToLocked() {
    CardDiscoveryService service = createService();
    service.recordSeen("strike");

    service.replaceProgress(
        Map.of("defend", CardUnlockState.SEEN, "retired_card", CardUnlockState.SEEN));

    assertEquals(CardUnlockState.LOCKED, service.getProgressSnapshot().get("strike"));
    assertEquals(CardUnlockState.SEEN, service.getProgressSnapshot().get("defend"));
    assertFalse(service.getProgressSnapshot().containsKey("retired_card"));
  }

  @Test
  void shouldRejectInvalidReplacementWithoutMutatingProgress() {
    CardDiscoveryService service = createService();
    service.recordSeen("strike");
    Map<String, CardUnlockState> before = service.getProgressSnapshot();
    Map<String, CardUnlockState> invalid = new LinkedHashMap<>();
    invalid.put("defend", null);

    assertThrows(IllegalArgumentException.class, () -> service.replaceProgress(invalid));
    assertEquals(before, service.getProgressSnapshot());
  }

  @Test
  void shouldNotifyOnlyForActualChanges() {
    CardDiscoveryService service = createService();
    @SuppressWarnings("unchecked")
    EventListener1<CardEntryView> listener =
        (EventListener1<CardEntryView>) mock(EventListener1.class);
    service.getEvents().addListener(CardDiscoveryService.ENTRY_UPDATED_EVENT, listener);

    service.recordSeen("strike");
    service.recordSeen("strike");
    service.recordSeen("missing");

    ArgumentCaptor<CardEntryView> update = ArgumentCaptor.forClass(CardEntryView.class);
    verify(listener, times(1)).handle(update.capture());
    assertEquals("strike", update.getValue().cardId());
    assertEquals(CardUnlockState.SEEN, update.getValue().unlockState());
  }

  @Test
  void shouldRecordAllKnownIds() {
    CardDiscoveryService service = createService();

    service.recordSeenAll(List.of("strike", "missing", "defend"));

    assertTrue(
        service.getProgressSnapshot().values().stream()
            .allMatch(state -> state == CardUnlockState.SEEN));
  }

  private static CardDiscoveryService createService() {
    return new CardDiscoveryService(List.of(card("strike", "Strike"), card("defend", "Defend")));
  }

  static CardConfig card(String id, String name) {
    CardConfig card = new CardConfig();
    card.id = id;
    card.name = name;
    card.description = name + " description";
    card.cost = 1;
    card.type = CardType.ATTACK;
    card.target = TargetType.SINGLE_ENEMY;
    card.rarity = Rarity.COMMON;
    card.texturePath = "images/cards/" + id + ".png";
    return card;
  }
}
