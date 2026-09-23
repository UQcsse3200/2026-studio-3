package com.csse3200.game.components.save;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.save.AutosaveCoordinator;
import com.csse3200.game.save.DeleteSaveResult;
import com.csse3200.game.save.LoadResult;
import com.csse3200.game.save.RestoreResult;
import com.csse3200.game.save.SaveErrorMessages;
import com.csse3200.game.save.SaveGameRestoreService;
import com.csse3200.game.save.SaveGameService;
import com.csse3200.game.save.SaveResult;
import com.csse3200.game.save.SaveSlotListResult;
import com.csse3200.game.save.SaveSlotMetadata;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.MenuTheme;
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

  private static final String BACKGROUND_TEXTURE = "images/main_menu_background.png";
  public static final String BUTTON_TEXTURE = "images/save_load_button_frame.png";
  private static final float ACTION_BUTTON_WIDTH = 168f;
  private static final float ACTION_BUTTON_HEIGHT = 56f;
  private static final float PANEL_WIDTH = 880f;
  private static final float PANEL_HEIGHT = 500f;

  private Stack rootStack;
  private Table rootTable;
  private Label statusLabel;
  private TextButton.TextButtonStyle archiveButtonStyle;

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
    rootStack = new Stack();
    rootStack.setFillParent(true);

    Texture backgroundTexture =
        ServiceLocator.getResourceService().getAsset(BACKGROUND_TEXTURE, Texture.class);
    backgroundTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    Image background = new Image(backgroundTexture);
    background.setScaling(Scaling.fill);
    rootStack.add(background);

    Color overlayColour = MenuTheme.deepPlum();
    overlayColour.a = 0.6f; // Darker than the main menu overlay so slot text stays readable
    Table overlay = new Table();
    overlay.setBackground(skin.newDrawable("white", overlayColour));
    rootStack.add(overlay);

    Table wrapper = new Table();
    wrapper.setFillParent(true);
    wrapper.center().pad(MenuTheme.SCREEN_PADDING);

    rootTable = new Table();
    rootTable.setBackground(skin.newDrawable("white", new Color(0.105f, 0.07f, 0.065f, 0.92f)));
    rootTable.pad(28f, 34f, 30f, 34f);
    addHeader();

    statusLabel = new Label("", themedLabelStyle());
    rootTable.add(statusLabel).colspan(4).padBottom(10f).row();

    wrapper.add(rootTable).width(PANEL_WIDTH).height(PANEL_HEIGHT);
    rootStack.add(wrapper);
    stage.addActor(rootStack);
  }

  private Label.LabelStyle themedLabelStyle() {
    Label.LabelStyle style = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
    style.fontColor = MenuTheme.warmParchment();
    return style;
  }

  private TextButton.TextButtonStyle themedButtonStyle() {
    if (archiveButtonStyle == null) {
      Texture texture = ServiceLocator.getResourceService().getAsset(BUTTON_TEXTURE, Texture.class);
      texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
      TextureRegionDrawable frame = new TextureRegionDrawable(new TextureRegion(texture));
      frame.setMinWidth(0f);
      frame.setMinHeight(0f);

      archiveButtonStyle =
          new TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle.class));
      archiveButtonStyle.up = frame;
      archiveButtonStyle.over = frame.tint(new Color(1f, 0.9f, 0.72f, 1f));
      archiveButtonStyle.down = frame.tint(new Color(0.78f, 0.62f, 0.68f, 1f));
      archiveButtonStyle.disabled = frame.tint(new Color(0.42f, 0.38f, 0.38f, 0.72f));
      archiveButtonStyle.fontColor = MenuTheme.warmParchment();
      archiveButtonStyle.overFontColor = Color.WHITE;
      archiveButtonStyle.downFontColor = MenuTheme.warmParchment();
      archiveButtonStyle.disabledFontColor = MenuTheme.mutedBrown();
    }
    return archiveButtonStyle;
  }

  private void addHeader() {
    Label.LabelStyle titleStyle = new Label.LabelStyle(skin.get("title", Label.LabelStyle.class));
    titleStyle.fontColor = MenuTheme.warmParchment();
    rootTable.add(new Label("Save / Load", titleStyle)).colspan(3).padBottom(20f);
    if (backAction != null) {
      TextButton backButton = new TextButton("Back", themedButtonStyle());
      backButton.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
              backAction.run();
            }
          });
      rootTable
          .add(backButton)
          .width(ACTION_BUTTON_WIDTH)
          .height(ACTION_BUTTON_HEIGHT)
          .padBottom(20f)
          .row();
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
    boolean isAutosave = slotId == AutosaveCoordinator.AUTOSAVE_SLOT_ID;
    String slotName = isAutosave ? "Autosave" : "Slot " + slotId;

    String label =
        hasSave
            ? slotName + " — " + describeTimestamp(metadata.savedAtEpochMillis)
            : slotName + " — empty";
    rootTable.add(new Label(label, themedLabelStyle())).left().padRight(20f);

    if (isAutosave) {
      rootTable
          .add(new Label("Auto", themedLabelStyle()))
          .width(ACTION_BUTTON_WIDTH)
          .height(ACTION_BUTTON_HEIGHT)
          .padRight(10f);
    } else {
      TextButton saveButton = new TextButton("Save", themedButtonStyle());
      saveButton.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
              onSave(slotId);
            }
          });
      rootTable
          .add(saveButton)
          .width(ACTION_BUTTON_WIDTH)
          .height(ACTION_BUTTON_HEIGHT)
          .padRight(10f);
    }

    TextButton loadButton = new TextButton("Load", themedButtonStyle());
    loadButton.setDisabled(!hasSave);
    loadButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            onLoad(slotId);
          }
        });
    rootTable.add(loadButton).width(ACTION_BUTTON_WIDTH).height(ACTION_BUTTON_HEIGHT).padRight(10f);

    TextButton deleteButton = new TextButton("Delete", themedButtonStyle());
    deleteButton.setDisabled(!hasSave);
    deleteButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            onDelete(slotId);
          }
        });
    rootTable
        .add(deleteButton)
        .width(ACTION_BUTTON_WIDTH)
        .height(ACTION_BUTTON_HEIGHT)
        .padBottom(12f)
        .row();
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
    if (rootStack != null) {
      rootStack.remove();
      rootStack.clear();
    }
    super.dispose();
  }
}
