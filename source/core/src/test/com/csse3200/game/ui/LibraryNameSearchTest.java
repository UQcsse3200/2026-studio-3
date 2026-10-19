package com.csse3200.game.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class LibraryNameSearchTest {
  @Test
  void matchesPartialNamesAndTreatsWhitespaceAsEmpty() {
    for (String query : new String[] {"", " \t ", "SHADE", "  shAdE  ", "esser"}) {
      assertTrue(LibraryNameSearch.matches("Lesser Shade", query), query);
    }
    assertFalse(LibraryNameSearch.matches("Lesser Shade", "Knight"));
  }

  @Test
  void punctuationIsLiteralRatherThanARegularExpression() {
    assertTrue(LibraryNameSearch.matches("Ward [II]+", "[II]+"));
    for (String query : new String[] {".*", "[", "???", "^Ward"}) {
      assertFalse(LibraryNameSearch.matches("Lesser Shade", query), query);
    }
  }

  @Test
  void caseMatchingDoesNotDependOnTheDefaultLocale() {
    Locale previous = Locale.getDefault();
    try {
      Locale.setDefault(Locale.forLanguageTag("tr-TR"));
      assertTrue(LibraryNameSearch.matches("IRON Knight", "iron"));
      assertEquals("iron", LibraryNameSearch.normalize(" IRON "));
    } finally {
      Locale.setDefault(previous);
    }
  }
}
