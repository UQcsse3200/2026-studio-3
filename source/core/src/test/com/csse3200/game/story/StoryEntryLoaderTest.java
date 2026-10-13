package com.csse3200.game.story;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class StoryEntryLoaderTest {
  private static final String TEST_DIRECTORY = "test/story/";

  @Test
  void defaultFileLoadsUniqueNonEmptyEntries() {
    List<StoryEntry> entries = StoryEntryLoader.loadDefault();

    assertFalse(entries.isEmpty());
    Set<String> ids = new HashSet<>();
    for (StoryEntry entry : entries) {
      assertTrue(ids.add(entry.id()), "duplicate id " + entry.id());
      assertFalse(entry.title().isBlank(), entry.id());
      assertFalse(entry.paragraphs().isEmpty(), entry.id());
    }
  }

  @Test
  void invalidAndDuplicateEntriesAreSkippedAndOrderIsKept() {
    List<StoryEntry> entries = StoryEntryLoader.load(TEST_DIRECTORY + "mixed_entries.json");

    assertAll(
        () ->
            assertEquals(List.of("first", "second"), entries.stream().map(StoryEntry::id).toList()),
        () -> assertEquals("First", entries.get(0).title()),
        () -> assertEquals(List.of("One.", "Two."), entries.get(0).paragraphs()),
        () -> assertEquals("One.\n\nTwo.", entries.get(0).bodyText()));
  }

  @Test
  void unusableFilesReturnAnEmptyListInsteadOfThrowing() {
    assertAll(
        () -> assertTrue(StoryEntryLoader.load(TEST_DIRECTORY + "missing.json").isEmpty()),
        () -> assertTrue(StoryEntryLoader.load(TEST_DIRECTORY + "malformed.json").isEmpty()),
        () -> assertTrue(StoryEntryLoader.load(TEST_DIRECTORY + "missing_entries.json").isEmpty()),
        () -> assertTrue(StoryEntryLoader.load(null).isEmpty()),
        () -> assertTrue(StoryEntryLoader.load(" ").isEmpty()));
  }

  @Test
  void loadedListIsImmutable() {
    List<StoryEntry> entries = StoryEntryLoader.loadDefault();

    assertThrows(
        UnsupportedOperationException.class,
        () -> entries.add(new StoryEntry("extra", "Extra", List.of("Text."))));
  }

  @Test
  void entryRejectsBlankFieldsAndCopiesParagraphs() {
    assertAll(
        () ->
            assertThrows(
                IllegalArgumentException.class, () -> new StoryEntry(" ", "T", List.of("p"))),
        () ->
            assertThrows(
                IllegalArgumentException.class, () -> new StoryEntry("id", "", List.of("p"))),
        () ->
            assertThrows(
                IllegalArgumentException.class, () -> new StoryEntry("id", "T", List.of())),
        () ->
            assertThrows(
                IllegalArgumentException.class, () -> new StoryEntry("id", "T", List.of(" "))));

    List<String> paragraphs = new java.util.ArrayList<>(List.of("p"));
    StoryEntry entry = new StoryEntry("id", "T", paragraphs);
    paragraphs.add("changed");
    assertEquals(List.of("p"), entry.paragraphs());
  }
}
