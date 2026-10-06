package com.csse3200.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.badlogic.gdx.Screen;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.deck.PlayerDeckFactory;
import com.csse3200.game.entities.factories.PlayerFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rewards.ItemType;
import com.csse3200.game.rewards.RewardOption;
import com.csse3200.game.screens.MapScreen;
import com.csse3200.game.screens.NarrationScreen;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;

@ExtendWith(GameExtension.class)
class GdxGameTest {
  @Test
  void shouldRejectBlankEventIdBeforeChangingScreen() {
    GdxGame game = gameWithActiveNode(RoomType.EVENT);
    Screen screen = mock(Screen.class);
    game.setScreen(screen);

    assertThrows(IllegalArgumentException.class, () -> game.startEvent(" "));
    assertThrows(IllegalArgumentException.class, () -> game.startEvent(null));
    assertSame(screen, game.getScreen());
    verify(screen, never()).dispose();
  }

  @Test
  void shouldRequireAnActiveEventNodeBeforeChangingScreen() {
    GdxGame game = new GdxGame();
    Screen screen = mock(Screen.class);
    game.setScreen(screen);

    assertThrows(IllegalStateException.class, () -> game.startEvent("dice-game"));
    assertSame(screen, game.getScreen());
    verify(screen, never()).dispose();
  }

  @Test
  void shouldRejectShopNodeBeforeChangingScreen() {
    GdxGame game = gameWithActiveNode(RoomType.SHOP);
    Screen screen = mock(Screen.class);
    game.setScreen(screen);

    assertThrows(IllegalStateException.class, () -> game.startEvent("dice-game"));
    assertSame(screen, game.getScreen());
    verify(screen, never()).dispose();
  }

  @Test
  void shouldRejectUnknownEventIdBeforeChangingScreen() {
    GdxGame game = gameWithActiveNode(RoomType.EVENT);
    Screen screen = mock(Screen.class);
    game.setScreen(screen);

    assertThrows(IllegalArgumentException.class, () -> game.startEvent("healing-spring"));
    assertSame(screen, game.getScreen());
    verify(screen, never()).dispose();
  }

  @Test
  void newRunDisposesOldScreenBeforeResettingPlayerAndDeck() {
    GdxGame game = gameWithActiveNode(RoomType.COMBAT);
    RunState runState = game.getRunState();
    CardService cards = new CardLibrary(CardConfigLoader.loadCards());

    PlayerRunState oldPlayer = runState.getOrCreatePlayerState();
    PlayerDeck oldDeck = runState.getOrCreatePlayerDeck(cards);
    oldPlayer.addOwnedItem(ItemType.LUCKY_COIN);
    oldDeck.clear();

    runState.initialisePlayerStats(1, 200, 8);
    runState.markCardFusionUsed();
    runState.setPendingEliteTempleReward(true);
    runState.setPendingReward(RewardOption.gold(15));

    Screen oldScreen = mock(Screen.class);
    game.setScreen(oldScreen);

    doAnswer(
            invocation -> {
              assertSame(oldPlayer, runState.getOrCreatePlayerState());
              oldPlayer.restore(1, 200, 999);
              return null;
            })
        .when(oldScreen)
        .dispose();

    AtomicReference<Runnable> finishOpening = new AtomicReference<>();

    try (MockedConstruction<NarrationScreen> narrations =
            mockConstruction(
                NarrationScreen.class,
                (narration, context) -> {
                  assertEquals("opening", context.arguments().get(0));
                  verify(oldScreen).dispose();
                  assertFalse(runState.isRunActive());
                  finishOpening.set((Runnable) context.arguments().get(1));
                });
        MockedConstruction<MapScreen> maps =
            mockConstruction(
                MapScreen.class,
                (map, context) -> {
                  verify(oldScreen).dispose();

                  assertFalse(runState.isRunActive());
                  assertNull(runState.getMapGraph());
                  assertNull(runState.getActiveNodeId());
                  assertNull(runState.getEncounterSeed());
                  assertEquals(0, runState.getPlayerHealth());
                  assertEquals(0, runState.getPlayerMaxHealth());
                  assertEquals(0, runState.getPlayerMaxEnergy());
                  assertFalse(runState.hasUsedCardFusion());
                  assertFalse(runState.hasPendingEliteTempleReward());
                  assertNull(runState.getPendingReward());

                  PlayerRunState newPlayer = runState.getOrCreatePlayerState();
                  PlayerRunState defaults = PlayerFactory.createInitialRunState();

                  assertNotSame(oldPlayer, newPlayer);
                  assertEquals(defaults.getCurrentHealth(), newPlayer.getCurrentHealth());
                  assertEquals(defaults.getMaxHealth(), newPlayer.getMaxHealth());
                  assertEquals(defaults.getGold(), newPlayer.getGold());
                  assertTrue(newPlayer.getOwnedItems().isEmpty());

                  PlayerDeck newDeck = runState.getOrCreatePlayerDeck(cards);
                  assertNotSame(oldDeck, newDeck);
                  assertEquals(PlayerDeckFactory.getStarterDeckCardIds(), newDeck.getCardIds());
                })) {
      game.startNewRun();

      assertEquals(1, narrations.constructed().size());
      assertSame(narrations.constructed().get(0), game.getScreen());
      verify(narrations.constructed().get(0)).show();
      assertEquals(0, maps.constructed().size());

      finishOpening.get().run();

      assertEquals(1, maps.constructed().size());
      assertSame(maps.constructed().get(0), game.getScreen());
      verify(maps.constructed().get(0)).show();
      verify(narrations.constructed().get(0)).dispose();
      verify(oldScreen).dispose();
    }
  }

  private static GdxGame gameWithActiveNode(RoomType roomType) {
    GdxGame game = new GdxGame();
    MapNode node = new MapNode(7, roomType);
    game.getRunState().restoreRun(new MapGraph(Map.of(7, node), false), 7);
    return game;
  }
}
