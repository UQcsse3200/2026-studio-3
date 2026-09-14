package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.effects.CardEffectResolutionContext;
import com.csse3200.game.cards.effects.CardEffectResolver;
import com.csse3200.game.cards.effects.PlayerEffectState;
import com.csse3200.game.cards.play.CardPlayService;
import com.csse3200.game.cards.play.CardPlayTarget;
import com.csse3200.game.cards.play.integration.Team1EnemyStateAdapter;
import com.csse3200.game.cards.play.integration.Team7PlayerStateAdapter;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.enemy.EnemyBehaviourComponent;
import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.components.player.PlayerIntent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Exercises the new definitions through deck admission, resolution and actual stat updates. */
@ExtendWith(GameExtension.class)
class SprintTwoCardsIntegrationTest {
  private static final List<String> IDS =
      List.of("starfall", "rift_lance", "astral_ward", "resurrection");
  private CardLibrary library;

  @BeforeEach
  void setUp() {
    library = new CardLibrary(CardConfigLoader.loadCards());
  }

  @Test
  void shouldAdmitNewCardsToDeckAndFindTheirArtwork() {
    BattleDeck deck = new BattleDeck(new PlayerDeck(IDS));
    deck.drawCards(IDS.size());
    assertEquals(IDS, deck.getHand());
    for (String id : IDS) {
      CardConfig card = library.getCard(id).orElseThrow();
      assertTrue(CardValidator.validate(card).isEmpty());
      assertTrue(Gdx.files.internal(card.texturePath).exists(), card.texturePath);
    }
  }

  @Test
  void shouldSpendFiveEnergyAndApplyTimedHealingWithoutAnImmediateHeal() {
    for (int maxHealth : List.of(60, 100, 175)) {
      CombatStatsComponent stats = new CombatStatsComponent(maxHealth, 1);
      stats.setHealth(7);
      EnergyComponent energy = new EnergyComponent(5);
      Team7PlayerStateAdapter player = new Team7PlayerStateAdapter(energy, stats);
      BattleDeck deck = deckWith("resurrection");
      CardPlayService service = new CardPlayService(library, deck, energy, player, null);

      var result =
          service.playCard(com.csse3200.game.cards.play.CardPlayRequest.self("resurrection"));
      assertTrue(result.success());
      player.applyPlayerEffects(result.playerEffects());

      assertEquals(7, stats.getHealth());
      assertEquals(6, stats.getStatusEffect(EffectType.HEAL.name()).getValue());
      assertEquals(3, stats.getStatusEffect(EffectType.HEAL.name()).getDuration());
      assertEquals(0, energy.getCurrentEnergy());
      assertTrue(result.updatedHand().isEmpty());
      assertEquals(List.of("resurrection"), result.updatedDiscardPile());
    }
  }

  @Test
  void shouldRejectResurrectionWithoutEnoughEnergyWithoutMovingCardOrHealing() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 1);
    stats.setHealth(20);
    EnergyComponent energy = new EnergyComponent(3);
    BattleDeck deck = deckWith("resurrection");
    CardPlayService service = new CardPlayService(library, deck, energy);

    var result =
        service.playCard(com.csse3200.game.cards.play.CardPlayRequest.self("resurrection"));

    assertFalse(result.success());
    assertTrue(result.playerEffects().isEmpty());
    assertEquals(20, stats.getHealth());
    assertEquals(3, energy.getCurrentEnergy());
    assertEquals(List.of("resurrection"), deck.getHand());
  }

  @Test
  void shouldHealSixHealthOnExactlyTheNextThreePlayerTurns() {
    CombatStatsComponent stats = new CombatStatsComponent(175, 1);
    stats.setHealth(12);
    EnergyComponent energy = new EnergyComponent(5);
    Entity player = new Entity().addComponent(stats).addComponent(energy);
    Entity enemy = defendingEnemy();
    BattleController controller =
        new BattleController(
            player,
            List.of(enemy),
            new CardEffectResolver(library),
            library,
            deckWith("resurrection"));
    controller.start();

    assertTrue(
        controller.submitCardPlayRequest(
            new CardPlayRequest("resurrection", "player"), PlayerIntent.DEFEND));

    assertEquals(12, stats.getHealth());
    assertEquals(0, energy.getCurrentEnergy());
    for (int turn = 1; turn <= 4; turn++) {
      controller.endPlayerTurn();
      assertEquals(12 + 6 * Math.min(turn, 3), stats.getHealth());
    }
    assertFalse(stats.hasStatusEffect(EffectType.HEAL.name()));
  }

  @Test
  void shouldExpireTimedHealingEvenWhenAlreadyAtFullHealth() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 1);
    stats.applyStatusEffect(EffectType.HEAL.name(), 6, 3);
    Entity player = new Entity().addComponent(stats).addComponent(new EnergyComponent(5));
    BattleController controller = new BattleController(player, List.of(defendingEnemy()));
    controller.start();
    controller.endPlayerTurn();
    controller.endPlayerTurn();
    assertEquals(100, stats.getHealth());
    assertFalse(stats.hasStatusEffect(EffectType.HEAL.name()));
    stats.setHealth(50);
    controller.endPlayerTurn();
    assertEquals(50, stats.getHealth());
  }

  @Test
  void shouldKeepBandageHealingImmediateAndRefreshRepeatedTimedHealing() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 1);
    stats.setHealth(10);
    Team7PlayerStateAdapter player = new Team7PlayerStateAdapter(new EnergyComponent(5), stats);
    CardEffectResolver resolver = new CardEffectResolver(library);
    player.applyPlayerEffects(
        resolver.resolve("resurrection", new PlayerEffectState()).playerEffects());
    stats.getStatusEffect(EffectType.HEAL.name()).tickAndCheckExpired();
    player.applyPlayerEffects(resolver.resolve("bandage", new PlayerEffectState()).playerEffects());
    assertEquals(16, stats.getHealth());
    assertEquals(2, stats.getStatusEffect(EffectType.HEAL.name()).getDuration());
    player.applyPlayerEffects(
        resolver.resolve("resurrection", new PlayerEffectState()).playerEffects());
    assertEquals(6, stats.getStatusEffect(EffectType.HEAL.name()).getValue());
    assertEquals(3, stats.getStatusEffect(EffectType.HEAL.name()).getDuration());
  }

  @Test
  void shouldClearTimedHealingOnDefeatWithoutRevivingThePlayer() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 1);
    stats.setHealth(0);
    stats.applyStatusEffect(EffectType.HEAL.name(), 6, 3);
    BattleController controller =
        new BattleController(new Entity().addComponent(stats), List.of(defendingEnemy()));
    controller.start();
    assertEquals(0, stats.getHealth());
    assertFalse(stats.hasStatusEffect(EffectType.HEAL.name()));
  }

  @Test
  void shouldGrantBlockAndStrengthThenBoostStarfallAgainstAllEnemies() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 1);
    Team7PlayerStateAdapter player = new Team7PlayerStateAdapter(new EnergyComponent(5), stats);
    CardEffectResolver resolver = new CardEffectResolver(library);
    var ward = resolver.resolve("astral_ward", new CardEffectResolutionContext(0, 0, 0));
    player.applyPlayerEffects(ward.playerEffects());
    assertEquals(4, stats.getBlock());
    assertEquals(1, player.statusValue(EffectType.STRENGTH));

    Entity first = new Entity().addComponent(new CombatStatsComponent(30, 0));
    Entity second = new Entity().addComponent(new CombatStatsComponent(20, 0));
    Team1EnemyStateAdapter enemies =
        new Team1EnemyStateAdapter(Map.of("first", first, "second", second));
    var starfall = resolver.resolve("starfall", new CardEffectResolutionContext(1, 0, 0));
    enemies.applyEnemyEffects(
        new CardPlayTarget(TargetType.ALL_ENEMIES, null), starfall.enemyEffects());
    assertEquals(23, first.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(13, second.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldDealRiftLanceDamageBeforeApplyingVulnerableToOnlySelectedEnemy() {
    Entity first = new Entity().addComponent(new CombatStatsComponent(30, 0));
    Entity second = new Entity().addComponent(new CombatStatsComponent(30, 0));
    Team1EnemyStateAdapter enemies =
        new Team1EnemyStateAdapter(Map.of("first", first, "second", second));
    var result = new CardEffectResolver(library).resolve("rift_lance", new PlayerEffectState());
    assertEquals(
        List.of(EffectType.DAMAGE, EffectType.VULNERABLE),
        result.enemyEffects().stream().map(effect -> effect.type()).toList());

    enemies.applyEnemyEffects(
        new CardPlayTarget(TargetType.SINGLE_ENEMY, "first"), result.enemyEffects());

    assertEquals(26, first.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(1, enemies.statusValue("first", EffectType.VULNERABLE));
    assertEquals(30, second.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(0, enemies.statusValue("second", EffectType.VULNERABLE));
  }

  private BattleDeck deckWith(String id) {
    BattleDeck deck = new BattleDeck(new PlayerDeck(List.of(id)));
    deck.drawCards(1);
    return deck;
  }

  private Entity defendingEnemy() {
    EnemyBehaviourComponent behaviour = mock(EnemyBehaviourComponent.class);
    when(behaviour.getCurrentIntent()).thenReturn(EnemyIntent.defend(1));
    when(behaviour.rollIntent()).thenReturn(EnemyIntent.defend(1));
    return new Entity().addComponent(new CombatStatsComponent(30, 0)).addComponent(behaviour);
  }
}
