package com.csse3200.game.components.encounters;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.runtime.CardResolver;
import com.csse3200.game.components.cards.CardWidget;
import com.csse3200.game.components.cards.CardWidgetAssets;
import com.csse3200.game.encounters.results.AcquisitionResult;
import com.csse3200.game.encounters.results.AcquisitionView;
import com.csse3200.game.services.ResourceService;
import java.util.Objects;

/** A read-only result overlay for confirmed Shop and Event acquisitions. */
public final class AcquisitionResultDisplay implements AcquisitionView {
  private static final Color INK = new Color(0.94f, 0.86f, 0.72f, 1f);
  private static final Color GOLD = new Color(0.96f, 0.73f, 0.34f, 1f);
  private static final Color LOSS = new Color(0.95f, 0.53f, 0.50f, 1f);
  private final Stage stage;
  private final Skin skin;
  private final CardService cards;
  private final ResourceService resources;
  private Table overlay;
  private CardWidgetAssets cardAssets;

  public AcquisitionResultDisplay(
      Stage stage, Skin skin, CardService cards, ResourceService resources) {
    this.stage = Objects.requireNonNull(stage, "stage");
    this.skin = Objects.requireNonNull(skin, "skin");
    this.cards = cards;
    this.resources = resources;
  }

  @Override
  public void show(AcquisitionResult result, Runnable onContinue, Runnable onClose) {
    Objects.requireNonNull(result, "result");
    Objects.requireNonNull(onContinue, "onContinue");
    Objects.requireNonNull(onClose, "onClose");
    if (overlay != null) {
      throw new IllegalStateException("An acquisition result is already visible");
    }

    Table blocker = new Table();
    blocker.setName("acquisition-result-overlay");
    blocker.setFillParent(true);
    blocker.setTouchable(Touchable.enabled);
    blocker.setBackground(skin.newDrawable("white", new Color(0.015f, 0.01f, 0.025f, 0.80f)));

    Table panel = new Table();
    panel.setBackground(skin.newDrawable("white", new Color(0.14f, 0.09f, 0.10f, 1f)));
    panel.pad(24f);
    panel.top();
    panel.add(new Label("RESULTS", labelStyle("large", GOLD))).left().padBottom(8f);
    panel.row();
    panel
        .add(new Label("These changes have already been applied.", labelStyle("small", INK)))
        .left()
        .padBottom(15f);
    panel.row();

    Table entries = new Table();
    entries.top();
    for (AcquisitionResult.Entry entry : result.entries()) {
      entries.add(entryRow(entry)).growX().padBottom(8f);
      entries.row();
    }
    ScrollPane scroll = new ScrollPane(entries, skin);
    scroll.setFadeScrollBars(false);
    scroll.setScrollingDisabled(true, false);
    panel.add(scroll).grow().minHeight(180f);
    panel.row();

    Table buttons = new Table();
    buttons.defaults().width(180f).height(52f).pad(8f);
    buttons.add(button("Continue", "acquisition-continue", onContinue));
    buttons.add(button("Close", "acquisition-close", onClose));
    panel.add(buttons).center().padTop(8f);

    float width = Math.min(850f, Math.max(440f, stage.getWidth() - 70f));
    float height = Math.min(620f, Math.max(320f, stage.getHeight() - 60f));
    blocker.add(panel).width(width).height(height);
    overlay = blocker;
    stage.addActor(blocker);
    blocker.toFront();
  }

  private Table entryRow(AcquisitionResult.Entry entry) {
    Table row = new Table();
    row.setBackground(skin.newDrawable("white", new Color(0.20f, 0.15f, 0.17f, 1f)));
    row.pad(10f);
    Actor card = cardPreview(entry);
    if (card != null) {
      row.add(card).width(92f).height(186f).padRight(14f);
    }

    Color accent = entry.kind() == AcquisitionResult.Kind.NEGATIVE_EFFECT ? LOSS : GOLD;
    Table text = new Table();
    text.top().left();
    String kind = entry.kind().name().replace('_', ' ');
    text.add(new Label(kind, labelStyle("small", accent))).left();
    text.row();
    String name = entry.name() + (entry.quantity() > 1 ? " x" + entry.quantity() : "");
    Label title = new Label(name, labelStyle("default", INK));
    title.setWrap(true);
    text.add(title).growX().left().padTop(5f);
    text.row();
    if (!entry.description().isBlank()) {
      Label description = new Label(entry.description(), labelStyle("small", INK));
      description.setWrap(true);
      text.add(description).growX().left().padTop(5f);
      text.row();
    }
    if (entry.durationTurns() != null) {
      text.add(new Label(entry.durationTurns() + " turn(s)", labelStyle("small", accent)))
          .left()
          .padTop(5f);
    }
    row.add(text).growX().minWidth(0f);
    return row;
  }

  private Actor cardPreview(AcquisitionResult.Entry entry) {
    if (entry.kind() != AcquisitionResult.Kind.CARD || cards == null || resources == null) {
      return null;
    }
    CardConfig config = cards.getCard(entry.id()).orElse(null);
    if (config == null) {
      return null;
    }
    if (cardAssets == null) {
      cardAssets = CardWidgetAssets.fromManagedResources(skin, resources);
    }
    return new CardWidget(
        new CardResolver().resolveBasePreview(config, "acquisition-preview-" + entry.id()),
        cardAssets);
  }

  private LabelStyle labelStyle(String size, Color colour) {
    LabelStyle style = new LabelStyle(skin.get(size, LabelStyle.class));
    style.fontColor = colour;
    return style;
  }

  private TextButton button(String text, String name, Runnable action) {
    TextButton button = new TextButton(text, skin);
    button.setName(name);
    button.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            action.run();
          }
        });
    return button;
  }

  @Override
  public void hide() {
    if (overlay != null) {
      stage.cancelTouchFocus();
      overlay.remove();
      overlay.clear();
      overlay = null;
    }
  }
}
