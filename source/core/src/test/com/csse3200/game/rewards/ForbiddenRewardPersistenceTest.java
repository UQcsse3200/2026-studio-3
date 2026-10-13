package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.csse3200.game.bestiary.BestiaryService;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardDiscoveryService;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.save.GameStateSnapshotProvider;
import com.csse3200.game.save.JsonSaveGameRepository;
import com.csse3200.game.save.LoadResult;
import com.csse3200.game.save.RestoreResult;
import com.csse3200.game.save.SaveGameRestoreService;
import com.csse3200.game.save.SaveGameService;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * Full-journey check for Member 5 Forbidden rewards: claim → save → load → next battle draw pile.
 *
 * <p>Does not touch RewardDisplay UI, reward probability configs, or other teams' systems.
 */
@ExtendWith(GameExtension.class)
class ForbiddenRewardPersistenceTest {
  private static final List<String> FORBIDDEN_REWARDS =
      List.of("sealed_pact", "blood_price", "doom_sigil", "iron_oath");

  @TempDir Path saveDirectory;

  @Test
  void eachForbiddenRewardSurvivesSaveRestoreAndEntersNextBattleDeck() {
    for (String cardId : FORBIDDEN_REWARDS) {
      assertRewardJourney(cardId);
    }
  }

  private void assertRewardJourney(String cardId) {
    CardService cards = new CardLibrary(CardConfigLoader.loadCards());
    RunState run = activeRun();
    PlayerDeck persistentDeck = run.getOrCreatePlayerDeck(cards);
    RewardService rewards =
        new RewardService(new RewardGenerator(new Random(1)), cards, new Random(2));
    RewardOption option = RewardOption.cards(new CardRewardSelection(List.of(cardId)));

    rewards.claimRunReward(run, option, cardId);
    CardInstance acquired = persistentDeck.getCards().get(persistentDeck.size() - 1);
    assertEquals(cardId, acquired.cardId());

    CardDiscoveryService discovery = CardDiscoveryService.loadDefault();
    BestiaryService bestiary = BestiaryService.loadDefault();
    SaveGameService saves =
        new SaveGameService(
            new JsonSaveGameRepository(new FileHandle(saveDirectory.resolve(cardId).toFile())),
            new GameStateSnapshotProvider(
                run.getOrCreatePlayerState(), persistentDeck, run, bestiary, discovery));
    assertTrue(saves.saveGame(1).success(), cardId + " save");
    LoadResult loaded = saves.loadGame(1);
    assertTrue(loaded.success(), cardId + " load");

    PlayerRunState restoredPlayer = new PlayerRunState(1, 1, 0);
    PlayerDeck restoredDeck = new PlayerDeck(cards);
    RunState restoredRun = new RunState();
    RestoreResult restored =
        new SaveGameRestoreService(
                restoredPlayer,
                restoredDeck,
                restoredRun,
                BestiaryService.loadDefault(),
                CardDiscoveryService.loadDefault())
            .restore(loaded.data());

    assertTrue(restored.success(), cardId + " restore");
    CardInstance restoredCard = restoredDeck.getCard(acquired.instanceId()).orElseThrow();
    assertEquals(cardId, restoredCard.cardId());
    assertEquals(CardInstance.BASE_LEVEL, restoredCard.upgradeLevel());

    BattleDeck nextBattleDeck = new BattleDeck(restoredDeck);
    assertTrue(
        nextBattleDeck.getDrawPileInstances().stream()
            .anyMatch(card -> card.instanceId().equals(acquired.instanceId())),
        cardId + " missing from next battle draw pile");
  }

  private static RunState activeRun() {
    MapNode start = new MapNode(0, RoomType.COMBAT);
    RunState run = new RunState();
    assertTrue(run.startRun(new MapGraph(Map.of(0, start), false), 0));
    return run;
  }
}
