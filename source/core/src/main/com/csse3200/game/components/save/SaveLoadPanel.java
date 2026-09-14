package com.csse3200.game.components.save;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Scaling;
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
  private static final int PILL_HEIGHT = 64;
  private static final int PILL_WIDTH = 160;
  private static final int PILL_RADIUS = PILL_HEIGHT / 2;

  private Stack rootStack;
  private Table rootTable;
  private Label statusLabel;
  private final List<Texture> generatedPillTextures = new java.util.ArrayList<>();

  // Cached once and reused across every button and every refresh() call — MenuTheme colors don't
  // change at runtime, so regenerating these per-button (the original approach) created roughly
  // 40 new 160x64 textures per save/load/delete refresh, an unbounded GPU memory leak flagged in
  // review (PR #208, Anran).
  private NinePatchDrawable upDrawable;
  private NinePatchDrawable downDrawable;
  private NinePatchDrawable overDrawable;

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

    rootTable = new Table();
    addHeader();

    statusLabel = new Label("", themedLabelStyle());
    rootTable.add(statusLabel).colspan(4).padBottom(10f).row();

    rootStack.add(rootTable);
    stage.addActor(rootStack);
  }

  private Label.LabelStyle themedLabelStyle() {
    Label.LabelStyle style = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
    style.fontColor = MenuTheme.warmParchment();
    return style;
  }

  private TextButton.TextButtonStyle themedButtonStyle() {
    if (upDrawable == null) {
      upDrawable = pillDrawable(MenuTheme.burntRust());
      downDrawable = pillDrawable(MenuTheme.dustyMauve());
      overDrawable = pillDrawable(MenuTheme.softCoral());
    }

    TextButton.TextButtonStyle style =
        new TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle.class));
    style.up = upDrawable;
    style.down = downDrawable;
    style.over = overDrawable;
    style.disabled = upDrawable; // same color as enabled, by design (see disabledFontColor)
    style.fontColor = MenuTheme.warmParchment();
    style.overFontColor = Color.WHITE;
    style.downFontColor = MenuTheme.warmParchment();
    style.disabledFontColor = MenuTheme.warmParchment();
    return style;
  }

  /**
   * Builds a rounded-pill drawable in an exact {@link MenuTheme} color. The flat-earth skin only
   * ships pre-colored green pill textures (tinting them would multiply, not replace, the color) and
   * its only neutral region is a 1x1 white pixel (no rounding), so the shape is generated here
   * instead: two filled circles at the ends plus a connecting rectangle, composited onto one
   * texture via {@link Pixmap}. Wrapping it in a {@link NinePatch} keeps the rounded ends fixed
   * size while the middle stretches to fit each button's actual width.
   */
  private NinePatchDrawable pillDrawable(Color color) {
    Pixmap pixmap = new Pixmap(PILL_WIDTH, PILL_HEIGHT, Pixmap.Format.RGBA8888);
    pixmap.setColor(color);
    pixmap.fillCircle(PILL_RADIUS, PILL_RADIUS, PILL_RADIUS);
    pixmap.fillCircle(PILL_WIDTH - PILL_RADIUS - 1, PILL_RADIUS, PILL_RADIUS);
    pixmap.fillRectangle(PILL_RADIUS, 0, PILL_WIDTH - PILL_HEIGHT, PILL_HEIGHT);
    Texture texture = new Texture(pixmap);
    pixmap.dispose();
    generatedPillTextures.add(texture);
    NinePatch ninePatch = new NinePatch(texture, PILL_RADIUS, PILL_RADIUS, 0, 0);
    return new NinePatchDrawable(ninePatch);
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
    rootTable.add(new Label(label, themedLabelStyle())).left().padRight(20f);

    TextButton saveButton = new TextButton("Save", themedButtonStyle());
    saveButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            onSave(slotId);
          }
        });
    rootTable.add(saveButton).padRight(10f);

    TextButton loadButton = new TextButton("Load", themedButtonStyle());
    loadButton.setDisabled(!hasSave);
    loadButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            onLoad(slotId);
          }
        });
    rootTable.add(loadButton).padRight(10f);

    TextButton deleteButton = new TextButton("Delete", themedButtonStyle());
    deleteButton.setDisabled(!hasSave);
    deleteButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            onDelete(slotId);
          }
        });
    rootTable.add(deleteButton).padBottom(12f).row();
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
    for (Texture texture : generatedPillTextures) {
      texture.dispose();
    }
    generatedPillTextures.clear();
    super.dispose();
  }
}
