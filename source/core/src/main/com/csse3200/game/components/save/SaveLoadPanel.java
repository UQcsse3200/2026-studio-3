package com.csse3200.game.components.save;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.save.DeleteSaveResult;
import com.csse3200.game.save.LoadResult;
import com.csse3200.game.save.RestoreResult;
import com.csse3200.game.save.SaveErrorMessages;
import com.csse3200.game.save.SaveGameRestoreService;
import com.csse3200.game.save.SaveGameService;
import com.csse3200.game.save.SaveResult;
import com.csse3200.game.save.SaveSlotListResult;
import com.csse3200.game.save.SaveSlotMetadata;
import com.csse3200.game.ui.UIComponent;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Self-contained Save/Load panel: slot list, save/load/delete actions, and status feedback.
 *
 * <p>Deliberately independent of any specific menu screen — it only needs a {@link SaveGameService}
 * and a list of slot IDs to display. Add it as a component to whatever entity hosts the eventual
 * menu UI; it doesn't assume where it's placed.
 */
public class SaveLoadPanel extends UIComponent {
  private static final DateTimeFormatter TIMESTAMP_FORMAT =
      DateTimeFormatter.ofPattern("dd MMM, HH:mm").withZone(ZoneId.systemDefault());

  private final SaveGameService saveGameService;
  private final SaveGameRestoreService restoreService;
  private final List<Integer> slotIds;
  private final Runnable backAction;

  private Table rootTable;
  private Label statusLabel;

  public SaveLoadPanel(SaveGameService saveGameService, List<Integer> slotIds) {
    this(saveGameService, slotIds, null, null);
  }

  public SaveLoadPanel(
      SaveGameService saveGameService,
      List<Integer> slotIds,
      SaveGameRestoreService restoreService,
      Runnable backAction) {
    this.saveGameService = saveGameService;
    this.restoreService = restoreService;
    this.slotIds = List.copyOf(slotIds);
    this.backAction = backAction;
  }

  @Override
  public void create() {
    super.create();
    addActors();
    refresh();
  }

  private void addActors() {
    rootTable = new Table();
    rootTable.setFillParent(true);

    addHeader();

    statusLabel = new Label("", skin);
    rootTable.add(statusLabel).colspan(4).padBottom(10f).row();

    stage.addActor(rootTable);
  }

  private void addHeader() {
    rootTable.add(new Label("Save / Load", skin, "title")).colspan(3).padBottom(20f);
    if (backAction != null) {
      TextButton backButton = new TextButton("Back", skin);
      backButton.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
              backAction.run();
            }
          });
      rootTable.add(backButton).padBottom(20f).row();
    } else {
      rootTable.add().padBottom(20f).row();
    }
  }

  /** Rebuilds the slot list from the current save state. Call after any save/load/delete. */
  public void refresh() {
    SaveSlotListResult listResult = saveGameService.listSaveSlots();
    if (!listResult.success()) {
      statusLabel.setText(
          SaveErrorMessages.forDelete(
              DeleteSaveResult.failure(listResult.error(), listResult.message())));
      return;
    }

    rootTable.clearChildren();
    addHeader();
    rootTable.add(statusLabel).colspan(4).padBottom(10f).row();

    for (int slotId : slotIds) {
      SaveSlotMetadata metadata = findSlot(listResult.slots(), slotId);
      addSlotRow(slotId, metadata);
    }
  }

  private SaveSlotMetadata findSlot(List<SaveSlotMetadata> slots, int slotId) {
    return slots.stream().filter(m -> m.slotId == slotId).findFirst().orElse(null);
  }

  private void addSlotRow(int slotId, SaveSlotMetadata metadata) {
    boolean hasSave = metadata != null && metadata.loadable;

    String label =
        hasSave
            ? "Slot " + slotId + " — " + describeTimestamp(metadata.savedAtEpochMillis)
            : "Slot " + slotId + " — empty";
    rootTable.add(new Label(label, skin)).left().padRight(20f);

    TextButton saveButton = new TextButton("Save", skin);
    saveButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            onSave(slotId);
          }
        });
    rootTable.add(saveButton).padRight(10f);

    TextButton loadButton = new TextButton("Load", skin);
    loadButton.setDisabled(!hasSave);
    loadButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            onLoad(slotId);
          }
        });
    rootTable.add(loadButton).padRight(10f);

    TextButton deleteButton = new TextButton("Delete", skin);
    deleteButton.setDisabled(!hasSave);
    deleteButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            onDelete(slotId);
          }
        });
    rootTable.add(deleteButton).row();
  }

  private void onSave(int slotId) {
    SaveResult result = saveGameService.saveGame(slotId);
    statusLabel.setText(SaveErrorMessages.forSave(result));
    refresh();
  }

  private void onLoad(int slotId) {
    LoadResult result = saveGameService.loadGame(slotId);
    if (!result.success() || restoreService == null) {
      statusLabel.setText(SaveErrorMessages.forLoad(result));
      refresh();
      return;
    }

    RestoreResult restoreResult = restoreService.restore(result.data());
    statusLabel.setText(formatRestoreMessage(restoreResult));
    refresh();
  }

  private String formatRestoreMessage(RestoreResult result) {
    if (result.success()) {
      String resumeScreen =
          result.resumeScreen().isBlank()
              ? "the saved run"
              : readableResumeScreen(result.resumeScreen());
      return "Save loaded successfully. Ready to resume from " + resumeScreen + ".";
    }
    return result.message().isBlank()
        ? "Load failed: unable to restore save data"
        : result.message();
  }

  private String readableResumeScreen(String resumeScreen) {
    return switch (resumeScreen) {
      case "MAP" -> "the Map";
      case "BATTLE_SCREEN" -> "the Battle";
      case "ENCOUNTER" -> "the Encounter";
      case "MAIN_MENU" -> "the Main Menu";
      default -> resumeScreen;
    };
  }

  private void onDelete(int slotId) {
    DeleteSaveResult result = saveGameService.deleteSave(slotId);
    statusLabel.setText(SaveErrorMessages.forDelete(result));
    refresh();
  }

  private String describeTimestamp(long epochMillis) {
    return TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(epochMillis));
  }

  @Override
  public void draw(com.badlogic.gdx.graphics.g2d.SpriteBatch batch) {
    // Rendering handled by the stage.
  }

  @Override
  public void dispose() {
    rootTable.remove();
    super.dispose();
  }
}
