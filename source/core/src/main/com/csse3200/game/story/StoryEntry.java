package com.csse3200.game.story;

import java.util.List;

/**
 * One readable entry in the Library's Story section.
 *
 * @param id unique, stable identifier (snake_case)
 * @param title heading shown in the entry list and above the text
 * @param paragraphs body text, one string per paragraph
 */
public record StoryEntry(String id, String title, List<String> paragraphs) {
  public StoryEntry {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("Story entry id must not be blank");
    }
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("Story entry title must not be blank: " + id);
    }
    if (paragraphs == null || paragraphs.isEmpty()) {
      throw new IllegalArgumentException("Story entry must have at least one paragraph: " + id);
    }
    for (String paragraph : paragraphs) {
      if (paragraph == null || paragraph.isBlank()) {
        throw new IllegalArgumentException("Story entry paragraphs must not be blank: " + id);
      }
    }
    paragraphs = List.copyOf(paragraphs);
  }

  /** Returns the paragraphs joined with a blank line between them, ready for a wrapped label. */
  public String bodyText() {
    return String.join("\n\n", paragraphs);
  }
}
