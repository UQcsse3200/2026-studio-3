package com.csse3200.game.cards.runtime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CardResolverTest {
  private final CardResolver resolver = new CardResolver();

  @Test
  void shouldResolveBaseAndUpgradedStrikeFromOfficialConfiguration() {
    CardConfig strike = strike();

    ResolvedCard base =
        resolver.resolve(
            strike, new CardInstance("strike-base", "strike", CardInstance.BASE_LEVEL));
    ResolvedCard upgraded =
        resolver.resolve(
            strike, new CardInstance("strike-upgrade", "strike", CardInstance.UPGRADED_LEVEL));

    assertAll(
        () -> assertEquals("Strike", base.name()),
        () -> assertEquals(1, base.cost()),
        () -> assertEquals(6, base.effects().get(0).value),
        () -> assertFalse(base.upgraded()),
        () -> assertEquals("Strike+", upgraded.name()),
        () -> assertEquals("Deal 12 damage.", upgraded.description()),
        () -> assertEquals(1, upgraded.cost()),
        () -> assertEquals(Rarity.COMMON, upgraded.rarity()),
        () -> assertEquals(12, upgraded.effects().get(0).value),
        () -> assertTrue(upgraded.upgraded()),
        () -> assertEquals(CardType.ATTACK, upgraded.type()),
        () -> assertEquals(TargetType.SINGLE_ENEMY, upgraded.target()),
        () -> assertEquals(base.texturePath(), upgraded.texturePath()),
        () -> assertEquals("images/cards/strike.png", upgraded.texturePath()));
  }

  @Test
  void shouldResolveApprovedRestoringLightUpgrade() {
    assertApprovedUpgrade(
        "resurrection",
        "Restoring Light+",
        "A quiet celestial rite sustains the wounded. Heal 9 HP at the start of your next 3 turns.",
        5,
        Rarity.RARE,
        List.of(new EffectConfig(EffectType.HEAL, 9, 3)));
  }

  @Test
  void shouldResolveApprovedRiftLanceUpgradeInExecutionOrder() {
    assertApprovedUpgrade(
        "rift_lance",
        "Rift Lance+",
        "A shaft of starlight loosens corruption's hold. Deal 6 damage. Apply 2 Vulnerable for 2 turns.",
        1,
        Rarity.UNCOMMON,
        List.of(
            new EffectConfig(EffectType.DAMAGE, 6, 0),
            new EffectConfig(EffectType.VULNERABLE, 2, 2)));
  }

  @Test
  void shouldResolveApprovedAstralWardUpgradeInExecutionOrder() {
    assertApprovedUpgrade(
        "astral_ward",
        "Astral Ward+",
        "The old star-readers' ward steadies the weary. Gain 6 Block. Gain 2 Strength for the rest of combat. Restore 1 Energy.",
        2,
        Rarity.UNCOMMON,
        List.of(
            new EffectConfig(EffectType.BLOCK, 6, 0),
            new EffectConfig(EffectType.STRENGTH, 2, 0),
            new EffectConfig(EffectType.ENERGY_GAIN, 1, 0)));
  }

  @Test
  void shouldResolveApprovedStarfallUpgrade() {
    assertApprovedUpgrade(
        "starfall",
        "Starfall+",
        "Heaven's light falls upon those still bound. Deal 9 damage to all enemies.",
        2,
        Rarity.UNCOMMON,
        List.of(new EffectConfig(EffectType.DAMAGE, 9, 0)));
  }

  private void assertApprovedUpgrade(
      String cardId,
      String name,
      String description,
      int cost,
      Rarity rarity,
      List<EffectConfig> effects) {
    CardLibrary library = new CardLibrary(CardConfigLoader.loadCards());
    CardConfig config = library.getCard(cardId).orElseThrow();
    PlayerDeck deck = new PlayerDeck(library, List.of(cardId, cardId));
    CardInstance selected = deck.getCards().get(0);
    CardInstance sibling = deck.getCards().get(1);
    String selectedInstanceId = selected.instanceId();
    ResolvedCard base = resolver.resolve(config, sibling);
    CardInstance upgradedInstance = deck.upgradeCard(selected.instanceId());
    ResolvedCard upgraded = resolver.resolve(config, upgradedInstance);

    assertAll(
        () -> assertEquals(selected.instanceId(), upgraded.instanceId()),
        () -> assertEquals(cardId, upgraded.cardId()),
        () -> assertEquals(name, upgraded.name()),
        () -> assertEquals(description, upgraded.description()),
        () -> assertEquals(cost, upgraded.cost()),
        () -> assertEquals(rarity, upgraded.rarity()),
        () -> assertEquals(base.cost(), upgraded.cost()),
        () -> assertEquals(base.rarity(), upgraded.rarity()),
        () -> assertEquals(base.type(), upgraded.type()),
        () -> assertEquals(base.target(), upgraded.target()),
        () -> assertEquals(base.texturePath(), upgraded.texturePath()),
        () -> assertTrue(upgraded.upgraded()),
        () -> assertFalse(base.upgraded()),
        () -> assertEquals(List.of(upgradedInstance, sibling), deck.getCards()),
        () ->
            assertThrows(IllegalStateException.class, () -> deck.upgradeCard(selectedInstanceId)));
    assertEquals(effects.size(), upgraded.effects().size());
    for (int index = 0; index < effects.size(); index++) {
      EffectConfig expected = effects.get(index);
      EffectConfig actual = upgraded.effects().get(index);
      assertAll(
          () -> assertEquals(expected.type, actual.type),
          () -> assertEquals(expected.value, actual.value),
          () -> assertEquals(expected.duration, actual.duration));
    }
  }

  @Test
  void shouldDeepCopyEffectsWithoutMutatingSourceConfiguration() {
    CardConfig strike = strike();
    EffectConfig sourceBaseEffect = strike.effects[0];
    EffectConfig sourceUpgradeEffect = strike.upgrade.effects[0];

    ResolvedCard upgraded =
        resolver.resolve(
            strike, new CardInstance("strike-upgrade", "strike", CardInstance.UPGRADED_LEVEL));

    assertEquals(6, sourceBaseEffect.value);
    assertEquals(12, sourceUpgradeEffect.value);
    assertThrows(
        UnsupportedOperationException.class,
        () -> upgraded.effects().add(new EffectConfig(EffectType.DAMAGE, 99)));

    List<EffectConfig> consumerCopy = upgraded.effects();
    consumerCopy.get(0).value = 99;
    strike.upgrade.effects[0].value = 20;

    assertEquals(12, upgraded.effects().get(0).value);
    assertEquals(6, strike.effects[0].value);
    assertEquals(20, strike.upgrade.effects[0].value);
  }

  @Test
  void shouldRejectNullArguments() {
    CardConfig strike = strike();
    CardInstance instance = new CardInstance("strike-base", "strike", CardInstance.BASE_LEVEL);

    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> resolver.resolve(null, instance)),
        () -> assertThrows(IllegalArgumentException.class, () -> resolver.resolve(strike, null)));
  }

  @Test
  void shouldRejectMismatchedCardIds() {
    CardInstance wrongInstance = new CardInstance("copy-1", "defend", CardInstance.BASE_LEVEL);

    assertThrows(IllegalArgumentException.class, () -> resolver.resolve(strike(), wrongInstance));
  }

  @Test
  void shouldResolveAReadOnlyBasePreviewWithoutChangingTheDefinition() {
    CardConfig strike = strike();

    ResolvedCard preview = resolver.resolveBasePreview(strike, "reward-preview-strike");

    assertEquals("reward-preview-strike", preview.instanceId());
    assertEquals("strike", preview.cardId());
    assertEquals("Strike", preview.name());
    assertEquals(1, preview.cost());
    assertFalse(preview.upgraded());
    assertEquals("Strike", strike.name);
    assertEquals(6, strike.effects[0].value);
  }

  @Test
  void shouldRejectUpgradedInstanceWhenUpgradeDefinitionIsMissing() {
    CardConfig card = strike();
    card.upgrade = null;
    CardInstance upgraded = new CardInstance("copy-1", "strike", CardInstance.UPGRADED_LEVEL);

    assertThrows(IllegalStateException.class, () -> resolver.resolve(card, upgraded));
  }

  private static CardConfig strike() {
    return CardConfigLoader.loadCards().stream()
        .filter(card -> "strike".equals(card.id))
        .findFirst()
        .orElseThrow();
  }
}
