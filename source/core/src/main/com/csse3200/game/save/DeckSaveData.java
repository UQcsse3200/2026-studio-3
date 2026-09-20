package com.csse3200.game.save;

import com.csse3200.game.cards.runtime.CardInstance;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Serializable long-term deck state — each owned card's instance identity and upgrade level. */
public class DeckSaveData {
  public List<CardInstanceSaveData> cards = new ArrayList<>();

  /** Required for JSON deserialisation. */
  public DeckSaveData() {}

  public DeckSaveData(Collection<CardInstanceSaveData> cards) {
    if (cards != null) {
      this.cards.addAll(cards);
    }
  }

  /**
   * Convenience factory for callers that only care about card IDs, not per-copy identity —
   * synthesises a fresh base-level instance ID for each. Not used for real captures (see
   * GameStateSnapshotProvider.captureDeck()), only for tests/legacy data that predates per-instance
   * tracking.
   */
  public static DeckSaveData ofCardIds(Collection<String> cardIds) {
    DeckSaveData data = new DeckSaveData();
    if (cardIds != null) {
      int index = 0;
      for (String cardId : cardIds) {
        data.cards.add(
            new CardInstanceSaveData("legacy-" + index++, cardId, CardInstance.BASE_LEVEL));
      }
    }
    return data;
  }
}
