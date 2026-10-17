package com.csse3200.game.components.status;

import com.csse3200.game.components.enemy.IntentIcons;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Display name, rules text and icon for each status effect that can appear on a combatant.
 *
 * <p>Constants are named after the keys {@code CombatStatsComponent} stores statuses under. A key
 * may carry a suffix after a colon, such as {@code TAUNT:12} for a taunt applied by enemy 12; the
 * suffix is ignored when looking up the entry.
 *
 * <p>Poison, Vulnerable, Feeble and Strength use the same wording as the card keyword explanations,
 * so an effect reads the same on a card and on a combatant.
 */
public enum StatusInfo {
  POISON(
      "Poison",
      "Deals 1 damage per stack at enemy turn start. Ignores Block and Armour.",
      "images/status/poison.png"),
  VULNERABLE(
      "Vulnerable", "Receives 50% more normal attack damage.", "images/status/vulnerable.png"),
  FEEBLE("Feeble", "Deals 25% less normal attack damage.", "images/status/feeble.png"),
  STRENGTH("Strength", "+1 normal attack damage per point this combat.", IntentIcons.BUFF),
  SILENCE("Silence", "Cannot play cards.", IntentIcons.SILENCE),
  DAMAGE_ON_CARD_PLAY(
      "Cursed Cards",
      "Takes %d damage each time a card is played.",
      IntentIcons.DAMAGE_ON_CARD_PLAY),
  TAUNT("Taunt", "Single-target cards must target the taunting enemy.", IntentIcons.TAUNT);

  private static final String VALUE_PLACEHOLDER = "%d";

  private final String title;
  private final String description;
  private final String iconPath;

  StatusInfo(String title, String description, String iconPath) {
    this.title = title;
    this.description = description;
    this.iconPath = iconPath;
  }

  /**
   * @return the name shown as the tooltip heading
   */
  public String title() {
    return title;
  }

  /**
   * @return the internal path of this status's icon texture
   */
  public String iconPath() {
    return iconPath;
  }

  /**
   * Returns the rules text for this status at its current magnitude.
   *
   * <p>Only descriptions that mention an amount use the value; the rest ignore it.
   *
   * @param value the effect's current value, such as the damage dealt per card played
   * @return the rules text shown in the tooltip
   */
  public String describe(int value) {
    if (!description.contains(VALUE_PLACEHOLDER)) {
      return description;
    }
    return String.format(Locale.ROOT, description, value);
  }

  /**
   * Describes how long a status lasts, using the duration convention of {@code
   * CombatStatsComponent}: a positive number is the turns remaining, zero or less means permanent.
   *
   * @param duration remaining duration of the status
   * @return text such as "2 turns remaining"
   */
  public static String durationText(int duration) {
    if (duration <= 0) {
      return "Lasts until the end of combat";
    }
    return duration == 1 ? "1 turn remaining" : duration + " turns remaining";
  }

  /**
   * Describes each group of poison on its own line. Poison applied with different durations ticks
   * down separately, so some stacks can run out before others.
   *
   * <p>Groups are listed soonest to expire first, with permanent stacks last.
   *
   * @param stacksByDuration poison stacks keyed by remaining duration, as returned by {@code
   *     CombatStatsComponent.getPoisonStacksByDuration()}; a key of zero or less means permanent
   * @return text such as "4 stacks: 2 turns remaining", one line per group
   */
  public static String poisonDurationText(Map<Integer, Integer> stacksByDuration) {
    return stacksByDuration.entrySet().stream()
        .sorted(
            Comparator.comparingInt(
                (Map.Entry<Integer, Integer> group) ->
                    group.getKey() <= 0 ? Integer.MAX_VALUE : group.getKey()))
        .map(group -> stacksText(group.getValue()) + ": " + groupDurationText(group.getKey()))
        .collect(Collectors.joining("\n"));
  }

  private static String stacksText(int stacks) {
    return stacks == 1 ? "1 stack" : stacks + " stacks";
  }

  private static String groupDurationText(int duration) {
    return duration <= 0 ? "until the end of combat" : durationText(duration);
  }

  /**
   * Finds the entry for a status key as stored by {@code CombatStatsComponent}.
   *
   * @param statusKey stored key, such as {@code POISON} or {@code TAUNT:12}; may be null
   * @return the matching entry, or empty when the key is null, blank or not a known status
   */
  public static Optional<StatusInfo> forKey(String statusKey) {
    if (statusKey == null || statusKey.isBlank()) {
      return Optional.empty();
    }
    String baseKey = statusKey.split(":", 2)[0].trim().toUpperCase(Locale.ROOT);
    return Arrays.stream(values()).filter(info -> info.name().equals(baseKey)).findFirst();
  }

  /**
   * Returns every icon path once, for preloading through the resource service.
   *
   * @return distinct icon texture paths
   */
  public static String[] iconPaths() {
    return Arrays.stream(values()).map(StatusInfo::iconPath).distinct().toArray(String[]::new);
  }
}
