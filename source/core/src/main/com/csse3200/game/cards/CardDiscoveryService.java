package com.csse3200.game.cards;

import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.events.EventHandler;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Owns card definitions, player discovery progress, and UI-safe queries. */
public class CardDiscoveryService {
  /** Fired with a {@link CardEntryView} whenever an entry advances to a new unlock state. */
  public static final String ENTRY_UPDATED_EVENT = "entryUpdated";

  private static final Logger logger = LoggerFactory.getLogger(CardDiscoveryService.class);

  private final Map<String, CardConfig> entries = new LinkedHashMap<>();
  private final Map<String, CardUnlockState> progress = new LinkedHashMap<>();
  private final EventHandler events = new EventHandler();

  /** Loads card discovery from the game's default card definitions. */
  public static CardDiscoveryService loadDefault() {
    return new CardDiscoveryService(CardConfigLoader.loadCards());
  }

  /**
   * Creates card discovery from the supplied definitions in their existing order.
   *
   * @param cards registered card definitions
   */
  public CardDiscoveryService(List<CardConfig> cards) {
    if (cards == null) {
      throw new IllegalArgumentException("cards must not be null");
    }
    for (CardConfig card : cards) {
      if (card == null || card.id == null || card.id.isBlank()) {
        throw new IllegalArgumentException("cards must contain valid card IDs");
      }
      if (entries.putIfAbsent(card.id, card) != null) {
        throw new IllegalArgumentException("Duplicate card ID: " + card.id);
      }
      progress.put(card.id, CardUnlockState.LOCKED);
    }
  }

  /** Returns the event handler used to notify presentation code of progress changes. */
  public EventHandler getEvents() {
    return events;
  }

  /** Checks whether a card ID belongs to this discovery service. */
  public boolean contains(String cardId) {
    return cardId != null && entries.containsKey(cardId);
  }

  /** Returns every entry in stable configuration order. */
  public List<CardEntryView> getEntries() {
    return entries.keySet().stream().map(this::viewFor).toList();
  }

  /** Looks up one progress-aware card entry. */
  public Optional<CardEntryView> getEntry(String cardId) {
    if (!contains(cardId)) {
      return Optional.empty();
    }
    return Optional.of(viewFor(cardId));
  }

  /** Returns a stable, immutable copy of every card's current unlock state. */
  public Map<String, CardUnlockState> getProgressSnapshot() {
    return Collections.unmodifiableMap(new LinkedHashMap<>(progress));
  }

  /**
   * Atomically replaces discovery progress using stable card IDs.
   *
   * <p>Entries omitted from the supplied map become locked. Unknown but well-formed card IDs are
   * ignored so saves remain loadable when definitions change between versions.
   */
  public void replaceProgress(Map<String, CardUnlockState> restoredProgress) {
    if (restoredProgress == null) {
      throw new IllegalArgumentException("restoredProgress must not be null");
    }

    Map<String, CardUnlockState> nextProgress = new LinkedHashMap<>();
    for (String cardId : entries.keySet()) {
      nextProgress.put(cardId, CardUnlockState.LOCKED);
    }
    for (Map.Entry<String, CardUnlockState> restored : restoredProgress.entrySet()) {
      String cardId = restored.getKey();
      CardUnlockState state = restored.getValue();
      if (cardId == null || cardId.isBlank() || state == null) {
        throw new IllegalArgumentException("restored progress contains an invalid entry");
      }
      if (entries.containsKey(cardId)) {
        nextProgress.put(cardId, state);
      }
    }

    Map<String, CardUnlockState> previousProgress = new HashMap<>(progress);
    progress.clear();
    progress.putAll(nextProgress);
    for (String cardId : entries.keySet()) {
      if (previousProgress.get(cardId) != nextProgress.get(cardId)) {
        events.trigger(ENTRY_UPDATED_EVENT, viewFor(cardId));
      }
    }
  }

  /** Records that the player has seen a card. */
  public boolean recordSeen(String cardId) {
    if (cardId == null || cardId.isBlank() || !contains(cardId)) {
      logger.debug("Ignoring card discovery for unknown card id '{}'", cardId);
      return false;
    }
    if (progress.get(cardId).isAtLeast(CardUnlockState.SEEN)) {
      return false;
    }
    progress.put(cardId, CardUnlockState.SEEN);
    events.trigger(ENTRY_UPDATED_EVENT, viewFor(cardId));
    return true;
  }

  /** Records every supplied card ID as seen, ignoring unknown or blank IDs. */
  public void recordSeenAll(Collection<String> cardIds) {
    if (cardIds == null) {
      throw new IllegalArgumentException("cardIds must not be null");
    }
    for (String cardId : cardIds) {
      recordSeen(cardId);
    }
  }

  private CardEntryView viewFor(String cardId) {
    return CardEntryView.from(entries.get(cardId), progress.get(cardId));
  }
}
