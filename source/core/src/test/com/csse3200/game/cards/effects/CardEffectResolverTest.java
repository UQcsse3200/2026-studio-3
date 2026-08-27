package com.csse3200.game.cards.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

class CardEffectResolverTest {
  @Test
  void shouldResolveCardByIdThroughCardService() {
    CardConfig strike = card("strike", TargetType.SINGLE_ENEMY, damage(6));
    CardEffectResolver resolver = resolverWith(strike);
    RecordingCharacterEffectGateway self = new RecordingCharacterEffectGateway();
    RecordingCharacterEffectGateway enemy = new RecordingCharacterEffectGateway();

    resolver.resolve("strike", self, enemy, List.of(enemy));

    assertEquals(List.of("DAMAGE:6"), enemy.events);
    assertEquals(List.of(), self.events);
  }

  @Test
  void shouldResolveSelfTarget() {
    CardConfig defend = card("defend", TargetType.SELF, new EffectConfig(EffectType.BLOCK, 5));
    CardEffectResolver resolver = resolverWith(defend);
    RecordingCharacterEffectGateway self = new RecordingCharacterEffectGateway();
    RecordingCharacterEffectGateway enemy = new RecordingCharacterEffectGateway();

    resolver.resolve("defend", self, enemy, List.of(enemy));

    assertEquals(List.of("BLOCK:5"), self.events);
    assertEquals(List.of(), enemy.events);
  }

  @Test
  void shouldResolveEveryEnemy() {
    CardConfig expose =
        card("expose", TargetType.ALL_ENEMIES, new EffectConfig(EffectType.VULNERABLE, 2, 2));
    CardEffectResolver resolver = resolverWith(expose);
    RecordingCharacterEffectGateway self = new RecordingCharacterEffectGateway();
    RecordingCharacterEffectGateway firstEnemy = new RecordingCharacterEffectGateway();
    RecordingCharacterEffectGateway secondEnemy = new RecordingCharacterEffectGateway();

    resolver.resolve("expose", self, null, List.of(firstEnemy, secondEnemy));

    assertEquals(List.of("VULNERABLE:2:2"), firstEnemy.events);
    assertEquals(List.of("VULNERABLE:2:2"), secondEnemy.events);
    assertEquals(List.of(), self.events);
  }

  @Test
  void shouldPreserveTeam6EffectArrayOrder() {
    CardConfig poisonDagger =
        card(
            "poison_dagger",
            TargetType.SINGLE_ENEMY,
            damage(4),
            new EffectConfig(EffectType.POISON, 3, 3));
    CardEffectResolver resolver = resolverWith(poisonDagger);
    RecordingCharacterEffectGateway enemy = new RecordingCharacterEffectGateway();

    resolver.resolve("poison_dagger", new RecordingCharacterEffectGateway(), enemy, List.of(enemy));

    assertEquals(List.of("DAMAGE:4", "POISON:3:3"), enemy.events);
  }

  @Test
  void shouldAllowAlreadyRetrievedCardConfig() {
    CardConfig bandage = card("bandage", TargetType.SELF, new EffectConfig(EffectType.HEAL, 6));
    CardEffectResolver resolver = resolverWith(bandage);
    RecordingCharacterEffectGateway self = new RecordingCharacterEffectGateway();

    resolver.resolve(bandage, self, null, List.of());

    assertEquals(List.of("HEAL:6"), self.events);
  }

  @Test
  void shouldRejectBlankOrUnknownCardId() {
    CardEffectResolver resolver = resolverWith(card("strike", TargetType.SINGLE_ENEMY, damage(6)));

    assertThrows(IllegalArgumentException.class, () -> resolver.resolve("", null, null, List.of()));
    assertThrows(
        IllegalArgumentException.class, () -> resolver.resolve("missing", null, null, List.of()));
  }

  @Test
  void shouldRejectMissingRequiredTarget() {
    CardConfig strike = card("strike", TargetType.SINGLE_ENEMY, damage(6));
    CardConfig defend = card("defend", TargetType.SELF, new EffectConfig(EffectType.BLOCK, 5));
    CardEffectResolver resolver = resolverWith(strike, defend);

    assertThrows(
        IllegalArgumentException.class,
        () -> resolver.resolve("strike", new RecordingCharacterEffectGateway(), null, List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> resolver.resolve("defend", null, new RecordingCharacterEffectGateway(), List.of()));
  }

  @Test
  void shouldRejectInvalidEnemyCollection() {
    CardConfig expose =
        card("expose", TargetType.ALL_ENEMIES, new EffectConfig(EffectType.VULNERABLE, 2, 2));
    CardEffectResolver resolver = resolverWith(expose);

    assertThrows(
        IllegalArgumentException.class,
        () -> resolver.resolve("expose", new RecordingCharacterEffectGateway(), null, null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            resolver.resolve(
                "expose",
                new RecordingCharacterEffectGateway(),
                null,
                java.util.Arrays.asList(new RecordingCharacterEffectGateway(), null)));
  }

  @Test
  void shouldRejectCardMutatedAfterRegistration() {
    CardConfig strike = card("strike", TargetType.SINGLE_ENEMY, damage(6));
    CardEffectResolver resolver = resolverWith(strike);
    strike.effects[0].value = -1;

    assertThrows(
        IllegalArgumentException.class,
        () ->
            resolver.resolve(
                "strike",
                new RecordingCharacterEffectGateway(),
                new RecordingCharacterEffectGateway(),
                List.of()));
  }

  @Test
  void shouldRejectNullDependencies() {
    CardLibrary library = new CardLibrary();

    assertThrows(IllegalArgumentException.class, () -> new CardEffectResolver(null));
    assertThrows(IllegalArgumentException.class, () -> new CardEffectResolver(library, null));
  }

  private static CardEffectResolver resolverWith(CardConfig... cards) {
    return new CardEffectResolver(new CardLibrary(List.of(cards)));
  }

  private static EffectConfig damage(int amount) {
    return new EffectConfig(EffectType.DAMAGE, amount);
  }

  private static CardConfig card(
      String id, TargetType target, EffectConfig firstEffect, EffectConfig... remainingEffects) {
    CardConfig card = new CardConfig();
    card.id = id;
    card.name = id;
    card.description = "Test card";
    card.cost = 1;
    card.type = CardType.SKILL;
    card.rarity = Rarity.COMMON;
    card.target = target;
    card.effects = new EffectConfig[remainingEffects.length + 1];
    card.effects[0] = firstEffect;
    System.arraycopy(remainingEffects, 0, card.effects, 1, remainingEffects.length);
    card.texturePath = "images/cards/" + id + ".png";
    return card;
  }
}
