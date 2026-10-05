package com.csse3200.game.encounters.integration;

import com.csse3200.game.cards.fusion.CardFusionResult;
import com.csse3200.game.cards.fusion.CardFusionService;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.maps.RunState;
import java.util.List;
import java.util.Objects;

/** Bridges the delegated Card Fusion Event lifecycle to the Card Fusion domain service. */
public final class CardFusionEncounterFlow {
  private final ChanceEncounterSession encounterSession;
  private final RunState runState;
  private final CardFusionService fusionService;

  /**
   * Creates the flow used by the Card Fusion Event UI after its Event choice is delegated.
   *
   * @param encounterSession delegated Event session that returns the player to the map
   * @param runState active run containing the player's persistent deck and fusion allowance
   * @param fusionService domain service that validates and mutates the deck
   */
  public CardFusionEncounterFlow(
      ChanceEncounterSession encounterSession, RunState runState, CardFusionService fusionService) {
    this.encounterSession =
        Objects.requireNonNull(encounterSession, "encounterSession cannot be null");
    this.runState = Objects.requireNonNull(runState, "runState cannot be null");
    this.fusionService = Objects.requireNonNull(fusionService, "fusionService cannot be null");
  }

  /**
   * Returns the Common card instances that the player may select for fusion.
   *
   * @return owned Common card instances eligible for fusion
   */
  public List<CardInstance> getEligibleCards() {
    requireDelegatedSession();
    return fusionService.getEligibleCards(runState);
  }

  /**
   * Attempts a fusion and completes the Event only after a successful deck mutation.
   *
   * @param selectedInstanceIds exactly three owned Common card instance IDs
   * @return the domain result; failed attempts keep the Event open for another selection or leave
   */
  public CardFusionResult fuse(List<String> selectedInstanceIds) {
    requireDelegatedSession();
    CardFusionResult result = fusionService.fuse(runState, selectedInstanceIds);
    if (result.successful()) {
      encounterSession.completeDelegated();
    }
    return result;
  }

  /**
   * Leaves the Fusion UI and consumes the Event node without modifying the deck.
   *
   * @return true only when this call completes the delegated Event for the first time
   */
  public boolean leave() {
    requireDelegatedSession();
    return encounterSession.completeDelegated();
  }

  private void requireDelegatedSession() {
    if (!encounterSession.isAwaitingDelegatedCompletion()) {
      throw new IllegalStateException("Card Fusion requires an active delegated Event session");
    }
  }
}
