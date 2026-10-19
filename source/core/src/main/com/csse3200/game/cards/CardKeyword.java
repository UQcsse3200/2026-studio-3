package com.csse3200.game.cards;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Shared keyword names and rules explanations for card tooltips and inspection. */
public enum CardKeyword {
  BLOCK("Block", "Absorbs normal damage. Resets at your next turn's start."),
  STRENGTH("Strength", "+1 normal attack damage per point this combat."),
  VULNERABLE("Vulnerable", "Receives 50% more normal attack damage."),
  POISON("Poison", "Deals 1 damage per stack at enemy turn start. Ignores Block and Armour."),
  FEEBLE("Feeble", "Deals 25% less normal attack damage."),
  ARMOUR("Armour", "Absorbs normal damage after Block. Persists across turns.");

  private static final Pattern KEYWORD_PATTERN =
      Pattern.compile(
          "\\b("
              + Arrays.stream(values())
                  .map(keyword -> keyword == ARMOUR ? "Armou?r" : Pattern.quote(keyword.title))
                  .collect(Collectors.joining("|"))
              + ")\\b",
          Pattern.CASE_INSENSITIVE);

  private final String title;
  private final String description;

  CardKeyword(String title, String description) {
    this.title = title;
    this.description = description;
  }

  /** Returns the display name used as a tooltip heading. */
  public String title() {
    return title;
  }

  /** Returns the rules explanation, independent of a card's individual effect values. */
  public String description() {
    return description;
  }

  /**
   * Finds supported whole words, ignoring case and retaining their first appearance order.
   *
   * @param description plain rules text, normally from {@code ResolvedCard.description()}
   * @return each mentioned keyword once, including keywords mentioned as defences
   */
  public static List<CardKeyword> findIn(String description) {
    Matcher matcher = KEYWORD_PATTERN.matcher(description);
    Set<CardKeyword> keywords = new LinkedHashSet<>();
    while (matcher.find()) {
      String name = matcher.group().toUpperCase(Locale.ROOT);
      keywords.add("ARMOR".equals(name) ? ARMOUR : valueOf(name));
    }
    return List.copyOf(keywords);
  }
}
