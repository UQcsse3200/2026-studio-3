package com.csse3200.game.components.combat;

import com.csse3200.game.cards.CardPlayRequest;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.effects.*;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffect;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CardEffectHandler {
  /** Team 5's card-effect resolver (Team 6 configs -> resolved effects); null without cards. */
  private final CardEffectResolver effectResolver;

  /** Team 6 card library, used to look up a played card's config; null without cards. */
  private final CardService cardService;

  /** Team 5-owned deck state; null when the loop runs without cards. */
  private final BattleDeck battleDeck;

  /** Team 5's per-battle player effect state, carrying Strength between plays. */
  private final PlayerEffectState playerEffectState;

  public CardEffectHandler(
      CardEffectResolver effectResolver,
      CardService cardService,
      BattleDeck battleDeck,
      PlayerEffectState playerEffectState) {

    this.effectResolver = effectResolver;
    this.cardService = cardService;
    this.battleDeck = battleDeck;
    this.playerEffectState = playerEffectState;
  }

  /**
   * Plays the submitted card: looks its config up in Team 6's library, checks it is in hand and
   * affordable, resolves its effects through Team 5's {@link CardEffectResolver}, then commits the
   * energy spend, moves the card to the discard pile and draws a replacement so the hand stays
   * topped up.
   *
   * @param request the card that the player intends to play
   * @param player the player instance in the game
   * @return the result, or {@code null} when no card system is wired in
   */
  public CardPlayResult playCard(CardPlayRequest request, Entity player) {
    // check whether card systems exist
    if (effectResolver == null || cardService == null || battleDeck == null) {
      return null;
    }

    Optional<CardConfig> maybeCard = cardService.getCard(request.cardID());
    if (maybeCard.isEmpty()) {
      return CardPlayResult.failure(
          "Unknown card: " + request.cardID(), request.cardID(), request.targetID(), battleDeck);
    }
    CardConfig card = maybeCard.get();

    // check if card is in the player's hand
    if (!battleDeck.getHand().contains(card.id)) {
      return CardPlayResult.failure("Card not in hand", card.id, request.targetID(), battleDeck);
    }

    // check if player has enough energy
    EnergyComponent energy = player.getComponent(EnergyComponent.class);
    if (energy != null && !energy.canAfford(card.cost)) {
      return CardPlayResult.failure("Not enough energy", card.id, request.targetID(), battleDeck);
    }

    // asks what the card does
    CardEffectResolution resolution = effectResolver.resolve(card, playerEffectState);

    // use up player's energy to play the card
    if (energy != null) {
      energy.spendEnergy(card.cost);
    }
    // card leaving the hand, and another one replaces it
    battleDeck.playCard(card.id);
    battleDeck.drawOne();

    return CardPlayResult.success(
        card.id,
        request.targetID(),
        resolution.enemyEffects(),
        resolution.playerEffects(),
        battleDeck,
        card.cost);
  }

  /**
   * Applies the given card effects on the enemy targets
   *
   * @param targets the enemies to apply the card effects to
   * @param effects the card effects to apply to the enemies
   */
  public void applyEnemyEffects(List<Entity> targets, List<ResolvedCardEffect> effects) {
    for (Entity enemy : targets) {
      CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
      if (stats == null) {
        continue;
      }
      for (ResolvedCardEffect effect : effects) {
        switch (effect.type()) {
          case DAMAGE -> stats.takeDamage(effect.value());
          case POISON ->
              stats.applyStatusEffect(
                  new StatusEffect("poison", effect.value(), effect.duration()));
          case VULNERABLE ->
              stats.applyStatusEffect(
                  new StatusEffect("vulnerable", effect.value(), effect.duration()));
          default -> {
            // BLOCK / HEAL / STRENGTH are not enemy-facing.
          }
        }
      }
    }
  }

  /**
   * Applies the given effects from the enemies on the player
   *
   * @param effects the effects to apply to the player
   * @param player the player instance in the game
   */
  public void applyPlayerEffects(List<ResolvedCardEffect> effects, Entity player) {
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    if (stats == null) {
      return;
    }
    for (ResolvedCardEffect effect : effects) {
      switch (effect.type()) {
        case BLOCK -> stats.addArmor(effect.value());
        case HEAL -> stats.heal(effect.value());
        default -> {
          // STRENGTH is already folded into the resolver's running player state.
        }
      }
    }
  }

  /**
   * Returns if the enemy is alive.
   *
   * @param enemy The enemy to be checked.
   * @return True if the enemy is alive, False if not.
   */
  public boolean isEnemyAlive(Entity enemy) {
    CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
    return !stats.isDead();
  }

  /**
   * Chooses which enemies a card's enemy effects hit. Self-targeting cards hit nothing; everything
   * else hits every living enemy, which covers both the single-enemy encounter and ALL_ENEMIES
   * cards. Precise single-target selection can be layered on when encounters have several enemies.
   */
  public List<Entity> getLivingEnemyTargets(CardPlayRequest request, List<Entity> enemies) {
    if ("player".equalsIgnoreCase(request.targetID())) {
      return List.of();
    }
    List<Entity> targets = new ArrayList<>();
    for (Entity enemy : enemies) {
      if (isEnemyAlive(enemy)) {
        targets.add(enemy);
      }
    }
    return targets;
  }
}
