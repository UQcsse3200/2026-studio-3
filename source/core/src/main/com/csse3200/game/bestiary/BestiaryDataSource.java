package com.csse3200.game.bestiary;

import java.util.List;

/** Supplies presentation-ready enemy entries to the bestiary UI. */
public interface BestiaryDataSource {
  /**
   * Returns the currently available bestiary entries.
   *
   * @return enemy entries in display order
   */
  List<BestiaryEntry> getEntries();
}
