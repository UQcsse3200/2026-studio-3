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

@ExtendWith(GameExtension.class)
class PostBattleCardRewardPersistenceTest {
  @TempDir Path saveDirectory;

  @Test
  void acquiredCardShouldSurviveSaveRestoreAndEnterTheNextBattleDeck() {
    CardService cards = new CardLibrary(CardConfigLoader.loadCards());
    RunState run = activeRun();
    PlayerDeck persistentDeck = run.getOrCreatePlayerDeck(cards);
    RewardService rewards =
        new RewardService(new RewardGenerator(new Random(1)), cards, new Random(2));
    RewardOption option = RewardOption.cards(new CardRewardSelection(List.of("bandage")));

    rewards.claimRunReward(run, option, "bandage");
    CardInstance acquired = persistentDeck.getCards().get(persistentDeck.size() - 1);

    CardDiscoveryService discovery = CardDiscoveryService.loadDefault();
    BestiaryService bestiary = BestiaryService.loadDefault();
    SaveGameService saves =
        new SaveGameService(
            new JsonSaveGameRepository(new FileHandle(saveDirectory.toFile())),
            new GameStateSnapshotProvider(
                run.getOrCreatePlayerState(), persistentDeck, run, bestiary, discovery));
    assertTrue(saves.saveGame(1).success());
    LoadResult loaded = saves.loadGame(1);
    assertTrue(loaded.success());

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

    assertTrue(restored.success());
    CardInstance restoredCard = restoredDeck.getCard(acquired.instanceId()).orElseThrow();
    assertEquals("bandage", restoredCard.cardId());
    assertEquals(CardInstance.BASE_LEVEL, restoredCard.upgradeLevel());

    BattleDeck nextBattleDeck = new BattleDeck(restoredDeck);
    assertTrue(
        nextBattleDeck.getDrawPileInstances().stream()
            .anyMatch(card -> card.instanceId().equals(acquired.instanceId())));
  }

  private static RunState activeRun() {
    MapNode start = new MapNode(0, RoomType.COMBAT);
    RunState run = new RunState();
    assertTrue(run.startRun(new MapGraph(Map.of(0, start), false), 0));
    return run;
  }
}
