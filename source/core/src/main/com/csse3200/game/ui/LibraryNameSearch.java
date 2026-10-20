package com.csse3200.game.ui;

import java.util.Locale;

/** Name matching shared by library screens; each screen applies its own discovery rules first. */
public final class LibraryNameSearch {
  private LibraryNameSearch() {}

  /**
   * Normalises a query without treating punctuation as a regular expression.
   *
   * @param query entered search text
   * @return trimmed query in locale-independent lower case
   */
  public static String normalize(String query) {
    return query.trim().toLowerCase(Locale.ROOT);
  }

  /**
   * Matches a visible name against a case-insensitive literal substring, including empty queries.
   *
   * @param displayedName name the player is allowed to see
   * @param query entered search text
   * @return whether the name contains the normalised query
   */
  public static boolean matches(String displayedName, String query) {
    return displayedName.toLowerCase(Locale.ROOT).contains(normalize(query));
  }
}
