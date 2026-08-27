package com.csse3200.game.cards.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** End-to-end effect coverage using Team 6's loader, library, service, and production cards. */
@ExtendWith(GameExtension.class)
class CardEffectTeam6IntegrationTest {
  private CardEffectResolver resolver;

  @BeforeEach
  void setUp() {
    CardService cardService = new CardLibrary(CardConfigLoader.loadCards());
    resolver = new CardEffectResolver(cardService);
  }

  @Test
  void shouldResolveStrikeAgainstSelectedEnemy() {
    RecordingCharacterEffectGateway self = new RecordingCharacterEffectGateway();
    RecordingCharacterEffectGateway enemy = new RecordingCharacterEffectGateway();

    resolver.resolve("strike", self, enemy, List.of(enemy));

    assertEquals(List.of("DAMAGE:6"), enemy.events);
    assertEquals(List.of(), self.events);
  }

  @Test
  void shouldResolveDefendOnSelf() {
    RecordingCharacterEffectGateway self = new RecordingCharacterEffectGateway();

    resolver.resolve("defend", self, null, List.of());

    assertEquals(5, self.block);
    assertEquals(List.of("BLOCK:5"), self.events);
  }

  @Test
  void shouldResolveBandageOnSelf() {
    RecordingCharacterEffectGateway self = new RecordingCharacterEffectGateway();

    resolver.resolve("bandage", self, null, List.of());

    assertEquals(6, self.healing);
    assertEquals(List.of("HEAL:6"), self.events);
  }

  @Test
  void shouldResolveInnerFocusForTheRemainderOfCombat() {
    RecordingCharacterEffectGateway self = new RecordingCharacterEffectGateway();

    resolver.resolve("inner_focus", self, null, List.of());

    assertEquals(2, self.strength);
    assertEquals(List.of("STRENGTH:2"), self.events);
  }

  @Test
  void shouldResolveExposeAgainstAllEnemiesWithDuration() {
    RecordingCharacterEffectGateway firstEnemy = new RecordingCharacterEffectGateway();
    RecordingCharacterEffectGateway secondEnemy = new RecordingCharacterEffectGateway();

    resolver.resolve(
        "expose", new RecordingCharacterEffectGateway(), null, List.of(firstEnemy, secondEnemy));

    assertEquals(2, firstEnemy.vulnerable);
    assertEquals(2, firstEnemy.vulnerableDuration);
    assertEquals(List.of("VULNERABLE:2:2"), firstEnemy.events);
    assertEquals(List.of("VULNERABLE:2:2"), secondEnemy.events);
  }

  @Test
  void shouldResolvePoisonDaggerInDeclarationOrderWithDuration() {
    RecordingCharacterEffectGateway enemy = new RecordingCharacterEffectGateway();

    resolver.resolve("poison_dagger", new RecordingCharacterEffectGateway(), enemy, List.of(enemy));

    assertEquals(4, enemy.damage);
    assertEquals(3, enemy.poison);
    assertEquals(3, enemy.poisonDuration);
    assertEquals(List.of("DAMAGE:4", "POISON:3:3"), enemy.events);
  }

  @Test
  void shouldRejectUnknownProductionCardId() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            resolver.resolve(
                "not-a-real-card", new RecordingCharacterEffectGateway(), null, List.of()));
  }
}
