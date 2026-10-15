package com.csse3200.game.components.mainmenu;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.chance.ChanceEncounterConfigLoader;
import com.csse3200.game.ui.PixelButtonStyles;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Removable main-menu-only event preview chooser. No choice is stored in a run, save or selector.
 * An empty ID requests the existing random demo; catalogue IDs request one isolated preview.
 */
public final class DemoEventPicker extends Dialog {
  public static final String SELECT_EVENT = "demoEventSelection";
  private final ScrollPane eventScroll;
  private final Consumer<String> onSelect;
  private boolean selected;

  public DemoEventPicker(Skin skin, Texture buttonTexture, Consumer<String> onSelect) {
    super("Demo Event", skin);
    this.onSelect = onSelect;
    setName("demo-event-picker");
    setModal(true);
    setMovable(false);
    WindowStyle windowStyle = new WindowStyle(getStyle());
    windowStyle.background = skin.newDrawable("white", Color.valueOf("281911"));
    windowStyle.stageBackground = skin.newDrawable("white", new Color(0f, 0f, 0f, 0.65f));
    windowStyle.titleFontColor = Color.valueOf("E8D6AE");
    setStyle(windowStyle);
    pad(36f, 22f, 16f, 22f);
    getContentTable().pad(20f);

    Label.LabelStyle noteStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
    noteStyle.fontColor = Color.valueOf("E8D6AE");
    Label note =
        new Label(
            "Choose an event to preview.\nDemo only - your current run is unchanged.", noteStyle);
    note.setAlignment(Align.center);
    getContentTable().add(note).padBottom(14f).row();

    Table events = new Table();
    events.defaults().growX().height(62f).pad(4f);
    events.add(choice("Random Event", "", skin, buttonTexture)).colspan(2).row();
    int count = 0;
    for (var encounter : ChanceEncounterConfigLoader.loadEncounters()) {
      events.add(choice(displayName(encounter.getId()), encounter.getId(), skin, buttonTexture));
      if (++count % 2 == 0) {
        events.row();
      }
    }
    eventScroll = new ScrollPane(events, skin);
    eventScroll.setScrollingDisabled(true, false);
    eventScroll.setFadeScrollBars(false);
    getContentTable().add(eventScroll).growX().row();
    TextButton back = new TextButton("Back", PixelButtonStyles.create(skin, buttonTexture));
    back.setName("demo-event-back");
    button(back, Boolean.FALSE);
    getButtonTable().defaults().width(240f).height(60f).pad(10f);
    getButtonTable().getCell(back).width(240f).height(60f).pad(10f);
    key(Input.Keys.ESCAPE, Boolean.FALSE);
  }

  private TextButton choice(String title, String id, Skin skin, Texture texture) {
    TextButton button = new TextButton(title, PixelButtonStyles.create(skin, texture));
    button.setName(id.isEmpty() ? "demo-event-random" : "demo-event-" + id);
    button.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (!selected) {
              selected = true;
              remove();
              onSelect.accept(id);
            }
          }
        });
    return button;
  }

  private static String displayName(String id) {
    StringBuilder title = new StringBuilder();
    for (String word : id.split("-")) {
      if (!title.isEmpty()) {
        title.append(' ');
      }
      title.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
    }
    return title.toString();
  }

  @Override
  public Dialog show(Stage stage) {
    float width = Math.min(700f, stage.getWidth() * 0.9f);
    getContentTable()
        .getCell(eventScroll)
        .width(width - 60f)
        .height(Math.min(340f, stage.getHeight() * 0.5f));
    return super.show(stage);
  }

  @Override
  protected void result(Object result) {
    remove();
  }
}
