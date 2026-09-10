package com.csse3200.game.bestiary;

import com.csse3200.game.entities.configs.EnemyConfig;
import com.csse3200.game.entities.configs.EnemyConfigs;
import com.csse3200.game.entities.configs.EnemyTier;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.files.FileLoader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Owns Bestiary definitions, player discovery progress, and UI-safe queries. */
public class BestiaryService {
  /** Fired with a {@link BestiaryEntryView} whenever an entry advances to a new unlock state. */
  public static final String ENTRY_UPDATED_EVENT = "entryUpdated";

  private static final Logger logger = LoggerFactory.getLogger(BestiaryService.class);
  private static final String DEFAULT_ENEMY_CONFIG = "configs/enemies.json";

  private final Map<String, BestiaryEntry> entries = new LinkedHashMap<>();
  private final Map<String, BestiaryUnlockState> progress = new HashMap<>();
  private final EventHandler events = new EventHandler();

  /**
   * Loads a Bestiary from the game's default enemy roster.
   *
   * @return service containing every valid configured enemy, or an empty service when loading fails
   */
  public static BestiaryService loadDefault() {
    EnemyConfigs configs = FileLoader.readClass(EnemyConfigs.class, DEFAULT_ENEMY_CONFIG);
    if (configs == null) {
      logger.warn("Failed to load Bestiary enemy definitions from {}", DEFAULT_ENEMY_CONFIG);
      configs = new EnemyConfigs();
    }
    return new BestiaryService(configs);
  }

  /**
   * Creates a Bestiary from the given enemy roster.
   *
   * @param configs enemy definitions owned by the Enemy System
   */
  public BestiaryService(EnemyConfigs configs) {
    this(configs, Map.of());
  }

  /**
   * Creates a Bestiary with optional descriptive text keyed by stable enemy ID.
   *
   * @param configs enemy definitions owned by the Enemy System
   * @param descriptions Bestiary-specific descriptions keyed by enemy ID
   */
  public BestiaryService(EnemyConfigs configs, Map<String, String> descriptions) {
    if (configs == null) {
      throw new IllegalArgumentException("configs must not be null");
    }
    Map<String, String> safeDescriptions = descriptions == null ? Map.of() : descriptions;

    List<BestiaryEntry> sortedEntries = new ArrayList<>();
    for (String id : configs.ids()) {
      EnemyConfig config = configs.get(id);
      if (config != null) {
        sortedEntries.add(
            BestiaryEntry.fromEnemyConfig(config, safeDescriptions.getOrDefault(id, "")));
      }
    }
    sortedEntries.sort(
        Comparator.comparing(BestiaryEntry::tier).thenComparing(BestiaryEntry::enemyId));

    for (BestiaryEntry entry : sortedEntries) {
      entries.put(entry.enemyId(), entry);
      progress.put(entry.enemyId(), BestiaryUnlockState.LOCKED);
    }
  }

  /**
   * Gets the event handler used to notify presentation code of progress changes.
   *
   * @return Bestiary event handler
   */
  public EventHandler getEvents() {
    return events;
  }

  /**
   * Checks whether an enemy ID belongs to this Bestiary.
   *
   * @param enemyId enemy identifier
   * @return true when the ID has a registered entry
   */
  public boolean contains(String enemyId) {
    return enemyId != null && entries.containsKey(enemyId);
  }

  /**
   * Returns every entry in stable tier-and-ID order.
   *
   * @return immutable list of progress-aware entry views
   */
  public List<BestiaryEntryView> getEntries() {
    return entries.keySet().stream().map(this::viewFor).toList();
  }

  /**
   * Returns entries belonging to one enemy tier.
   *
   * @param tier tier to filter by
   * @return immutable list of matching entry views
   */
  public List<BestiaryEntryView> getEntriesByTier(EnemyTier tier) {
    if (tier == null) {
      return List.of();
    }
    return entries.values().stream()
        .filter(entry -> entry.tier() == tier)
        .map(entry -> viewFor(entry.enemyId()))
        .toList();
  }

  /**
   * Looks up one progress-aware entry view.
   *
   * @param enemyId stable enemy identifier
   * @return matching view, or empty when the ID is unknown
   */
  public Optional<BestiaryEntryView> getEntry(String enemyId) {
    if (!contains(enemyId)) {
      return Optional.empty();
    }
    return Optional.of(viewFor(enemyId));
  }

  /**
   * Records that the player has encountered an enemy.
   *
   * @param enemyId stable enemy identifier
   * @return true when the entry advanced to {@link BestiaryUnlockState#ENCOUNTERED}
   */
  public boolean recordEncountered(String enemyId) {
    return advance(enemyId, BestiaryUnlockState.ENCOUNTERED);
  }

  /**
   * Records that the player has defeated an enemy. A direct defeat also implies an encounter.
   *
   * @param enemyId stable enemy identifier
   * @return true when the entry advanced to {@link BestiaryUnlockState#DEFEATED}
   */
  public boolean recordDefeated(String enemyId) {
    return advance(enemyId, BestiaryUnlockState.DEFEATED);
  }

  private boolean advance(String enemyId, BestiaryUnlockState target) {
    if (!contains(enemyId)) {
      logger.warn("Ignoring Bestiary progress for unknown enemy id '{}'", enemyId);
      return false;
    }

    BestiaryUnlockState current = progress.get(enemyId);
    if (current.isAtLeast(target)) {
      return false;
    }

    progress.put(enemyId, target);
    events.trigger(ENTRY_UPDATED_EVENT, viewFor(enemyId));
    return true;
  }

  private BestiaryEntryView viewFor(String enemyId) {
    return BestiaryEntryView.from(entries.get(enemyId), progress.get(enemyId));
  }
}
