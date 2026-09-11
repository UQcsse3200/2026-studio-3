package com.csse3200.game.save;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Serializable long-term deck state represented by stable card IDs. */
public class DeckSaveData {
  public List<String> cardIds = new ArrayList<>();

  /** Required for JSON deserialisation. */
  public DeckSaveData() {}

  public DeckSaveData(Collection<String> cardIds) {
    if (cardIds != null) {
      this.cardIds.addAll(cardIds);
    }
  }
}
