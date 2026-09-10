package com.csse3200.game.encounters.integration;

import com.csse3200.game.components.player.InventoryComponent;
import java.util.Objects;

/** Temporary Sprint 1 deck adapter backed by the current mock player inventory. */
public final class InventoryDeckAdapter implements DeckGateway {
  private final InventoryComponent inventory;

  /**
   * Creates a temporary deck adapter for the Sprint 1 inventory.
   *
   * @param inventory temporary inventory that stores card IDs
   */
  public InventoryDeckAdapter(InventoryComponent inventory) {
    this.inventory = Objects.requireNonNull(inventory, "inventory cannot be null");
  }

  @Override
  public boolean addCard(String cardId) {
    return inventory.addCard(cardId);
  }

  @Override
  public boolean removeCard(String cardId) {
    return inventory.removeCard(cardId);
  }
}
