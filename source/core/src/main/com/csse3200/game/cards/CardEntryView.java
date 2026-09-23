package com.csse3200.game.cards;

import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/** Read-only, progress-aware card definition intended for presentation code. */
public record CardEntryView(
    String cardId,
    CardUnlockState unlockState,
    String displayName,
    Optional<String> description,
    OptionalInt cost,
    Optional<CardType> type,
    Optional<TargetType> target,
    Optional<Rarity> rarity,
    Optional<List<EffectConfig>> effects,
    Optional<String> texturePath) {
  static final String LOCKED_DISPLAY_NAME = "???";

  /** Creates a non-null, immutable view value. */
  public CardEntryView {
    Objects.requireNonNull(cardId, "cardId");
    Objects.requireNonNull(unlockState, "unlockState");
    Objects.requireNonNull(displayName, "displayName");
    Objects.requireNonNull(description, "description");
    Objects.requireNonNull(cost, "cost");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(target, "target");
    Objects.requireNonNull(rarity, "rarity");
    Objects.requireNonNull(effects, "effects");
    Objects.requireNonNull(texturePath, "texturePath");
    effects = effects.map(List::copyOf);
  }

  /** Creates a view that exposes card details only after the card has been seen. */
  static CardEntryView from(CardConfig card, CardUnlockState state) {
    boolean seen = state.isAtLeast(CardUnlockState.SEEN);
    return new CardEntryView(
        card.id,
        state,
        seen ? card.name : LOCKED_DISPLAY_NAME,
        seen ? optionalText(card.description) : Optional.empty(),
        seen ? OptionalInt.of(card.cost) : OptionalInt.empty(),
        seen ? Optional.ofNullable(card.type) : Optional.empty(),
        seen ? Optional.ofNullable(card.target) : Optional.empty(),
        seen ? Optional.ofNullable(card.rarity) : Optional.empty(),
        seen ? Optional.of(List.copyOf(Arrays.asList(card.effects))) : Optional.empty(),
        seen ? optionalText(card.texturePath) : Optional.empty());
  }

  private static Optional<String> optionalText(String value) {
    return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
  }
}
