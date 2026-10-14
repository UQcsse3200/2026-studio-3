package com.csse3200.game.components.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.save.LoadResult;
import com.csse3200.game.save.RestoreResult;
import com.csse3200.game.save.SaveGameData;
import com.csse3200.game.save.SaveGameRestoreService;
import com.csse3200.game.save.SaveGameService;
import com.csse3200.game.save.SaveSlotListResult;
import com.csse3200.game.save.SaveSlotMetadata;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Regression tests for the in-place load confirmation used by active gameplay screens. */
@ExtendWith(GameExtension.class)
class SaveLoadPanelTest {
  private SaveGameService saveGameService;
  private SaveGameRestoreService restoreService;
  private SaveLoadPanel panel;
  private AtomicInteger loadSuccessCount;

  @BeforeEach
  void setUp() {
    RenderService renderService = new RenderService();
    Stage stage = new Stage(new FitViewport(1280f, 800f), mock(SpriteBatch.class));
    stage.getViewport().update(1280, 800, true);
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);

    ResourceService resources = mock(ResourceService.class);
    Texture texture = mock(Texture.class);
    when(resources.getAsset(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.eq(Texture.class)))
        .thenReturn(texture);
    ServiceLocator.registerResourceService(resources);

    saveGameService = mock(SaveGameService.class);
    restoreService = mock(SaveGameRestoreService.class);
    SaveSlotMetadata slot = new SaveSlotMetadata(1, 1L, "", 0L, "MAP");
    when(saveGameService.listSaveSlots()).thenReturn(SaveSlotListResult.success(List.of(slot)));
    loadSuccessCount = new AtomicInteger();

    panel =
        new SaveLoadPanel(
            saveGameService,
            List.of(1, 2, 3, 4),
            restoreService,
            () -> {},
            SaveLoadMode.LOAD_ONLY,
            true,
            loadSuccessCount::incrementAndGet);
    new Entity().addComponent(panel).create();
  }

  @Test
  void showingConfirmationKeepsButtonsInternallyClickable() {
    panel.requestLoad(1);

    assertTrue(panel.isLoadConfirmationVisible());
    assertTrue(
        panel
            .getConfirmLoadButton()
            .getListeners()
            .contains(panel.getConfirmLoadButton().getClickListener(), true));
    assertTrue(
        panel
            .getCancelLoadButton()
            .getListeners()
            .contains(panel.getCancelLoadButton().getClickListener(), true));
  }

  @Test
  void cancelDismissesConfirmationWithoutReadingSave() {
    panel.requestLoad(1);

    panel.getCancelLoadButton().fire(new ChangeListener.ChangeEvent());

    assertFalse(panel.isLoadConfirmationVisible());
    verify(saveGameService, never()).loadGame(1);
  }

  @Test
  void confirmationLoadsExactlyThePendingSlotOnce() {
    SaveGameData data = new SaveGameData();
    when(saveGameService.loadGame(1)).thenReturn(LoadResult.success(data));
    when(restoreService.restore(data)).thenReturn(RestoreResult.success("MAP"));
    panel.requestLoad(1);

    panel.getConfirmLoadButton().fire(new ChangeListener.ChangeEvent());

    verify(saveGameService).loadGame(1);
    verify(restoreService).restore(data);
    assertFalse(panel.isLoadConfirmationVisible());
    assertNull(panel.getConfirmLoadButton().getStage());
    assertEquals(1, loadSuccessCount.get());
  }

  @Test
  void cancelledSlotCannotLeakIntoLaterConfirmation() {
    SaveSlotMetadata autosave = new SaveSlotMetadata(4, 2L, "", 0L, "MAP");
    when(saveGameService.listSaveSlots())
        .thenReturn(
            SaveSlotListResult.success(
                List.of(new SaveSlotMetadata(1, 1L, "", 0L, "MAP"), autosave)));
    SaveGameData data = new SaveGameData();
    when(saveGameService.loadGame(4)).thenReturn(LoadResult.success(data));
    when(restoreService.restore(data)).thenReturn(RestoreResult.success("MAP"));

    panel.requestLoad(1);
    panel.cancelPendingLoad();
    panel.requestLoad(4);
    panel.confirmPendingLoad();

    verify(saveGameService, never()).loadGame(1);
    verify(saveGameService).loadGame(4);
  }
}
