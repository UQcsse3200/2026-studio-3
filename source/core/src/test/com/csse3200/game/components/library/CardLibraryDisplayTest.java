package com.csse3200.game.components.library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.GdxGame;
import com.csse3200.game.cards.CardDiscoveryService;
import com.csse3200.game.cards.CardUnlockState;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CardLibraryDisplayTest {
  @Test
  void shouldRenderLockedPlaceholders() {
    CardDiscoveryService discovery = CardDiscoveryService.loadDefault();
    CardLibraryDisplay display = createDisplay(discovery);

    display.create();

    assertEquals(CardUnlockState.LOCKED, display.getDisplayedEntry().unlockState());
    assertEquals("UNDISCOVERED", display.getStateText());
    assertEquals("Find this card to reveal its record.", display.getDescriptionText());
    assertEquals("Cost: ???", display.getCostText());
    assertEquals("???", CardLibraryDisplay.formatCardButton(display.getDisplayedEntry()));
    assertTrue(display.isLockedArtworkVisible());
    display.dispose();
  }

  @Test
  void shouldRefreshDisplayedEntryOnDiscoveryEventAndUnsubscribeOnDispose() {
    CardDiscoveryService discovery = CardDiscoveryService.loadDefault();
    CardLibraryDisplay display = createDisplay(discovery);
    display.create();
    String cardId = display.getDisplayedEntry().cardId();

    discovery.recordSeen(cardId);

    assertEquals(CardUnlockState.SEEN, display.getDisplayedEntry().unlockState());
    assertEquals("SEEN", display.getStateText());

    display.dispose();
    discovery.replaceProgress(Map.of());
    assertEquals(CardUnlockState.SEEN, display.getDisplayedEntry().unlockState());
  }

  private CardLibraryDisplay createDisplay(CardDiscoveryService discovery) {
    RenderService renderService = mock(RenderService.class);
    when(renderService.getStage()).thenReturn(mock(Stage.class));
    ServiceLocator.registerRenderService(renderService);

    ResourceService resources = mock(ResourceService.class);
    when(resources.getAsset(anyString(), eq(Texture.class))).thenReturn(mock(Texture.class));
    ServiceLocator.registerResourceService(resources);
    return new CardLibraryDisplay(mock(GdxGame.class), discovery);
  }
}
