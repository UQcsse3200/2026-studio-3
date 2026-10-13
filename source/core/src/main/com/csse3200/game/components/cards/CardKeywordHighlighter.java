package com.csse3200.game.components.cards;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Presentation-only keyword colouring shared by every card face. */
public final class CardKeywordHighlighter {
  private static final Pattern KEYWORDS =
      Pattern.compile(
          "(?<![\\p{L}\\p{N}_])(Block|Armour|Strength|Vulnerable|Poison|Feeble)(?![\\p{L}\\p{N}_])",
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  private CardKeywordHighlighter() {}

  /**
   * Adds dark brown libGDX markup to whole keywords, preserving casing and escaping literal
   * brackets. The original description and gameplay configuration remain plain text.
   *
   * @param description plain card rules text
   * @return rules text with presentation markup
   */
  public static String highlight(String description) {
    Matcher matcher = KEYWORDS.matcher(description);
    StringBuilder result = new StringBuilder();
    int previous = 0;
    while (matcher.find()) {
      result.append(escape(description.substring(previous, matcher.start())));
      result.append("[#8B4513]").append(matcher.group()).append("[]");
      previous = matcher.end();
    }
    return result.append(escape(description.substring(previous))).toString();
  }

  private static String escape(String text) {
    return text.replace("[", "[[");
  }
}
