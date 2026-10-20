package com.csse3200.game.components.encounters;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.runtime.CardResolver;
import com.csse3200.game.components.cards.CardWidget;
import com.csse3200.game.components.cards.CardWidgetAssets;
import com.csse3200.game.encounters.results.AcquisitionResult;
import com.csse3200.game.encounters.results.AcquisitionView;
import com.csse3200.game.rewards.ItemType;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.ui.ItemArtwork;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** A read-only result overlay for confirmed Shop and Event acquisitions. */
public final class AcquisitionResultDisplay implements AcquisitionView {
  private static final String WHITE = "white";
  private static final String SMALL = "small";
  private static final Color INK = new Color(0.94f, 0.86f, 0.72f, 1f);
  private static final Color GOLD = new Color(0.96f, 0.73f, 0.34f, 1f);
  private static final Color LOSS = new Color(0.95f, 0.53f, 0.50f, 1f);
  private final Stage stage;
  private final Skin skin;
  private final CardService cards;
  private final ResourceService resources;
  private Table overlay;
  private CardWidgetAssets cardAssets;
  private Table resultPanel;
  private Table cardDetails;
  private AcquisitionResult visibleResult;
  private final String headingText;
  private int previewPage;
  private static final String[] ART = {
    "images/ui/reward-panel-mythic-wingless.png",
    "images/ui/reward-card-mythic-aged.png",
    "images/ui/tutorial-continue-button.png",
    "images/ui/gold-reward.png",
    "images/ui/battle-menu-icons.png",
    "images/ui/victory-gold-icon.png",
    "images/heart.png"
  };
  private boolean loadedArt;
  private boolean continuing;
  private final List<Actor> sources = new ArrayList<>();
  private final List<Image> destinations = new ArrayList<>();
  private final List<String> itemTextures = new ArrayList<>();

  public AcquisitionResultDisplay(
      Stage stage, Skin skin, CardService cards, ResourceService resources) {
    this(stage, skin, cards, resources, "EVENT RESULTS");
  }

  public AcquisitionResultDisplay(
      Stage stage, Skin skin, CardService cards, ResourceService resources, String headingText) {
    this.stage = Objects.requireNonNull(stage, "stage");
    this.skin = Objects.requireNonNull(skin, "skin");
    this.cards = cards;
    this.resources = resources;
    this.headingText = Objects.requireNonNull(headingText);
  }

  @Override
  public void show(AcquisitionResult result, Runnable onContinue, Runnable onClose) {
    Objects.requireNonNull(result, "result");
    Objects.requireNonNull(onContinue, "onContinue");
    Objects.requireNonNull(onClose, "onClose");
    if (overlay != null) {
      throw new IllegalStateException("An acquisition result is already visible");
    }
    loadResultTextures(result);
    visibleResult = result;
    cardAssets = null;
    continuing = false;
    sources.clear();
    destinations.clear();

    Table blocker = new Table();
    blocker.setName("acquisition-result-overlay");
    blocker.setFillParent(true);
    blocker.setTouchable(Touchable.enabled);
    // This is only a translucent overlay: the live event/shop scene remains underneath.
    blocker.setBackground(skin.newDrawable(WHITE, new Color(0.015f, 0.01f, 0.025f, 0.48f)));

    Table panel = new Table();
    resultPanel = panel;
    panel.setName("acquisition-result-panel");
    panel.setBackground(art(0));
    panel.pad(145f, 140f, 110f, 140f);
    panel.top();
    Label heading = new Label(headingText, labelStyle("large", GOLD));
    heading.setName("acquisition-heading");
    heading.setFontScale(1.6f);
    heading.setAlignment(Align.center);
    panel.add(heading).height(50f).center().padBottom(10f);
    panel.row();

    Table entries = resultEntries(result);
    ScrollPane scroll = new ScrollPane(entries, skin);
    scroll.setFadeScrollBars(false);
    scroll.setScrollingDisabled(true, false);
    panel.add(scroll).grow().minHeight(0f);
    panel.row();

    Table buttons = new Table();
    buttons.defaults().width(360f).height(84f).pad(4f);
    buttons.add(
        button("Continue", "acquisition-continue", () -> continueWithFeedback(result, onContinue)));
    panel.add(buttons).center().padTop(8f);

    float width = Math.min(1120f, stage.getWidth() * .88f);
    float height = Math.min(740f, stage.getHeight() * .94f);
    blocker.add(panel).width(width).height(height);
    overlay = blocker;
    stage.addActor(blocker);
    blocker.toFront();
    addDestinations();
  }

  private void loadResultTextures(AcquisitionResult result) {
    if (resources != null) {
      resources.loadTextures(ART);
      resources.loadAll();
      loadedArt = true;
      for (AcquisitionResult.Entry entry : result.entries()) {
        ItemType item = itemType(entry);
        if (item != null) {
          String path = ItemArtwork.cutoutPath(item);
          if (!itemTextures.contains(path)) {
            resources.loadTextures(new String[] {path});
            itemTextures.add(path);
          }
        }
      }
      resources.loadAll();
      loadCardTextures(result);
      resources.loadAll();
    }
  }

  private void loadCardTextures(AcquisitionResult result) {
    if (resources == null) return;
    if (cards != null) {
      List<CardConfig> configs =
          result.entries().stream()
              .filter(entry -> entry.kind() == AcquisitionResult.Kind.CARD)
              .map(entry -> cards.getCard(entry.id()).orElse(null))
              .filter(Objects::nonNull)
              .toList();
      if (!configs.isEmpty()) {
        for (String path : CardWidgetAssets.collectTexturePaths(configs)) {
          if (!itemTextures.contains(path)) {
            resources.loadTextures(new String[] {path});
            itemTextures.add(path);
          }
        }
      }
    }
  }

  private Table resultEntries(AcquisitionResult result) {
    Table entries = new Table();
    entries.center();
    int displayedCount =
        (int) result.entries().stream().filter(e -> e.kind() != AcquisitionResult.Kind.CARD).count()
            + (result.entries().stream().anyMatch(e -> e.kind() == AcquisitionResult.Kind.CARD)
                ? 1
                : 0);
    int columns = Math.max(1, Math.min(3, displayedCount));
    float tileWidth =
        Math.min(250f, Math.max(140f, (stage.getWidth() * .88f - 300f) / columns - 18f));
    int index = 0;
    Actor cardSource = null;
    for (AcquisitionResult.Entry entry : result.entries()) {
      Table tile = entryRow(entry);
      if (entry.kind() == AcquisitionResult.Kind.CARD) {
        if (cardSource != null) {
          sources.set(sources.size() - 1, cardSource);
          continue;
        }
        cardSource = sources.get(sources.size() - 1);
      }
      entries.add(tile).width(tileWidth).height(330f).pad(7f).fillY();
      if (++index % columns == 0) entries.row();
    }
    return entries;
  }

  private void addEntryDescription(Table text, AcquisitionResult.Entry entry) {
    if (!entry.description().isBlank() || entry.kind() == AcquisitionResult.Kind.CARD) {
      String descriptionText = descriptionText(entry);
      Label description = new Label(descriptionText, labelStyle(SMALL, INK));
      description.setFontScale(entry.kind() == AcquisitionResult.Kind.CARD ? 1f : .88f);
      description.setWrap(true);
      description.setAlignment(Align.center);
      description.setName("acquisition-description-" + entry.id());
      // Fit long item rules inside their reserved area, rather than drawing over the frame.
      description.setWidth(206f);
      while (description.getPrefHeight() > 100f && description.getFontScaleX() > .72f) {
        description.setFontScale(description.getFontScaleX() - .02f);
      }
      text.add(description).growX().height(100f).padTop(2f);
      text.row();
    }
  }

  private static String descriptionText(AcquisitionResult.Entry entry) {
    if (entry.kind() == AcquisitionResult.Kind.CARD) return "click to check";
    if (entry.kind() == AcquisitionResult.Kind.GOLD) {
      return entry.delta() > 0 ? "Added to your run" : "Spent during this event";
    }
    return entry.description();
  }

  private Table entryRow(AcquisitionResult.Entry entry) {
    Table row = new Table();
    row.setBackground(art(1));
    row.pad(30f, 22f, 26f, 22f);
    row.setTouchable(
        entry.kind() == AcquisitionResult.Kind.CARD ? Touchable.enabled : Touchable.disabled);
    if (entry.kind() == AcquisitionResult.Kind.CARD) {
      row.setName("acquisition-card-reward-" + entry.id());
      row.addListener(
          new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
              showCardDetails();
            }
          });
    }

    Color accent =
        isNegative(entry) || (entry.isResourceChange() && entry.delta() < 0) ? LOSS : GOLD;
    Table text = new Table();
    text.center();
    String name = entry.kind() == AcquisitionResult.Kind.CARD ? "CARD REWARD" : entryTitle(entry);
    Label title = new Label(name, labelStyle("default", INK));
    title.setName("acquisition-entry-" + entry.id());
    title.setWrap(true);
    title.setAlignment(Align.center);
    title.setFontScale(.85f);
    text.add(title).growX().height(48f).padTop(0f);
    text.row();
    if (loadedArt || entry.kind() == AcquisitionResult.Kind.CARD) {
      Image icon = new Image(entryIcon(entry));
      icon.setScaling(Scaling.fit);
      icon.setName("acquisition-art-" + entry.id());
      text.add(icon).size(100f).pad(10f, 0f, 10f, 0f).row();
      sources.add(icon);
    } else {
      sources.add(title);
    }
    addEntryDescription(text, entry);
    if (entry.durationTurns() != null) {
      text.add(new Label(entry.durationTurns() + " turn(s)", labelStyle(SMALL, accent)))
          .left()
          .padTop(5f);
    }
    row.add(text).grow().minWidth(0f);
    return row;
  }

  private static String entryTitle(AcquisitionResult.Entry entry) {
    if (entry.isResourceChange()) {
      return entry.name() + " " + (entry.delta() > 0 ? "+" : "") + entry.delta();
    }
    return entry.name() + (entry.quantity() > 1 ? " x" + entry.quantity() : "");
  }

  private void showCardDetails() {
    previewPage = 0;
    renderCardDetails();
  }

  private void renderCardDetails() {
    if (continuing || overlay == null || cardDetails != null) return;
    cardDetails = new Table();
    cardDetails.setName("acquisition-card-details");
    cardDetails.setFillParent(true);
    cardDetails.setBackground(skin.newDrawable(WHITE, new Color(0.01f, 0.008f, 0.018f, .78f)));
    cardDetails.setTouchable(Touchable.enabled);
    cardDetails.pad(60f, 100f, 30f, 50f);
    Table grid = new Table();
    int index = 0;
    float width =
        Math.min(
            225f,
            Math.min(
                (stage.getWidth() - 170f) / 3f - 24f, (stage.getHeight() - 180f) * 225f / 456f));
    for (AcquisitionResult.Entry entry : visibleResult.entries()) {
      if (entry.kind() != AcquisitionResult.Kind.CARD) continue;
      for (int copy = 0; copy < entry.quantity(); copy++) {
        int position = index++;
        if (position < previewPage * 3 || position >= (previewPage + 1) * 3) continue;
        Actor card = cardPreview(entry);
        if (card == null) {
          Label fallback =
              new Label(entry.name() + "\n" + entry.description(), labelStyle(SMALL, INK));
          fallback.setWrap(true);
          card = fallback;
        }
        card.setName("acquisition-preview-" + entry.id() + "-" + copy);
        card.setTouchable(Touchable.disabled);
        grid.add(card).size(width, width * 456f / 225f).pad(12f);
      }
    }
    ScrollPane scroll = new ScrollPane(grid, skin);
    scroll.setScrollingDisabled(true, false);
    scroll.setFadeScrollBars(false);
    TextButton previous = button("<", "acquisition-card-previous", () -> changePreviewPage(-1));
    previous.setDisabled(previewPage == 0);
    previous.setVisible(index > 3);
    cardDetails.add(previous).size(64f, 70f);
    cardDetails.add(scroll).grow();
    TextButton next = button(">", "acquisition-card-next", () -> changePreviewPage(1));
    next.setDisabled((previewPage + 1) * 3 >= index);
    next.setVisible(index > 3);
    cardDetails.add(next).size(64f, 70f).row();
    TextButton back = button("Back", "acquisition-card-back", this::hideCardDetails);
    cardDetails.add(back).colspan(3).size(200f, 66f).right().padTop(12f);
    resultPanel.setVisible(false);
    overlay.addActor(cardDetails);
    cardDetails.validate();
  }

  private void hideCardDetails() {
    if (cardDetails == null) return;
    cardDetails.remove();
    cardDetails.clear();
    cardDetails = null;
    if (resultPanel != null) resultPanel.setVisible(true);
  }

  private void changePreviewPage(int direction) {
    int count =
        visibleResult.entries().stream()
            .filter(e -> e.kind() == AcquisitionResult.Kind.CARD)
            .mapToInt(AcquisitionResult.Entry::quantity)
            .sum();
    int target = previewPage + direction;
    if (target < 0 || target * 3 >= count) return;
    hideCardDetails();
    previewPage = target;
    renderCardDetails();
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
    TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(button.getStyle());
    style.up = art(2);
    style.down = art(2);
    style.over = art(2);
    style.fontColor = INK;
    button.setStyle(style);
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
    hideCardDetails();
    if (overlay != null) {
      stage.cancelTouchFocus();
      overlay.remove();
      overlay.clear();
      overlay = null;
    }
    sources.clear();
    visibleResult = null;
    resultPanel = null;
    cardAssets = null;
    destinations.clear();
    if (resources != null && loadedArt) {
      if (!itemTextures.isEmpty()) resources.unloadAssets(itemTextures.toArray(String[]::new));
      itemTextures.clear();
      resources.unloadAssets(ART);
      loadedArt = false;
    }
  }

  private Drawable art(int index) {
    if (resources == null || !loadedArt)
      return skin.newDrawable(WHITE, new Color(.08f, .08f, .10f, 1f));
    Texture texture = resources.getAsset(ART[index], Texture.class);
    if (texture == null) return skin.newDrawable(WHITE, new Color(.08f, .08f, .10f, 1f));
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
    if (index <= 2) {
      drawable.setMinWidth(0f);
      drawable.setMinHeight(0f);
    }
    return drawable;
  }

  private Drawable destinationIcon(int index) {
    if (resources == null || !loadedArt || index == 2) return art(5);
    Texture texture = resources.getAsset(ART[4], Texture.class);
    if (texture == null) return art(5);
    int cell = texture.getWidth() / 3;
    return new TextureRegionDrawable(
        new TextureRegion(texture, index * cell, 0, cell, texture.getHeight()));
  }

  private Drawable entryIcon(AcquisitionResult.Entry entry) {
    if (entry.kind() == AcquisitionResult.Kind.CARD
        && cards != null
        && resources != null
        && loadedArt) {
      CardConfig config = cards.getCard(entry.id()).orElse(null);
      if (config != null && config.texturePath != null && !config.texturePath.isBlank()) {
        Texture texture = resources.getAsset(config.texturePath, Texture.class);
        if (texture != null) return new TextureRegionDrawable(new TextureRegion(texture));
      }
    }
    ItemType item = itemType(entry);
    if (item != null && resources != null && loadedArt) {
      Texture texture = resources.getAsset(ItemArtwork.cutoutPath(item), Texture.class);
      if (texture != null) return new TextureRegionDrawable(new TextureRegion(texture));
    }
    return switch (entry.kind()) {
      case GOLD -> art(3);
      case HP -> art(6);
      case CARD -> destinationIcon(0);
      default -> destinationIcon(1);
    };
  }

  private void addDestinations() {
    for (int i = 0; i < 3; i++) {
      Image icon = new Image(destinationIcon(i));
      icon.setName("acquisition-destination-" + i);
      icon.setScaling(Scaling.fit);
      icon.setTouchable(Touchable.disabled);
      icon.setBounds(20f, stage.getHeight() - 180f - i * 65f, 52f, 52f);
      overlay.addActor(icon);
      destinations.add(icon);
    }
  }

  private void continueWithFeedback(AcquisitionResult result, Runnable onContinue) {
    if (continuing || overlay == null || cardDetails != null) return;
    continuing = true;
    ((TextButton) overlay.findActor("acquisition-continue")).setDisabled(true);
    overlay.validate();
    float delay = 0f;
    for (int i = 0; i < result.entries().size(); i++) {
      AcquisitionResult.Entry entry = result.entries().get(i);
      // A cursed item is still newly acquired inventory: fly it to the item destination.
      // Gold changes share the same result-to-HUD feedback, including displayed spending.
      if (entry.kind() != AcquisitionResult.Kind.CARD
          && entry.kind() != AcquisitionResult.Kind.ITEM
          && !(entry.kind() == AcquisitionResult.Kind.GOLD && entry.delta() != 0)) continue;
      int target =
          entry.kind() == AcquisitionResult.Kind.CARD
              ? 0
              : entry.kind() == AcquisitionResult.Kind.ITEM ? 1 : 2;
      Vector2 start =
          sources
              .get(i)
              .localToActorCoordinates(
                  overlay,
                  new Vector2(sources.get(i).getWidth() / 2f, sources.get(i).getHeight() / 2f));
      Image destination = destinations.get(target);
      Vector2 end =
          destination.localToActorCoordinates(
              overlay, new Vector2(destination.getWidth() / 2f, destination.getHeight() / 2f));
      Image flying = new Image(entryIcon(entry));
      flying.setName("acquisition-flight-" + entry.id());
      flying.setScaling(Scaling.fit);
      flying.setBounds(start.x - 25f, start.y - 25f, 50f, 50f);
      flying.setOrigin(25f, 25f);
      flying.setVisible(false);
      overlay.addActor(flying);
      addTrail(flying, delay, target == 0 ? Color.valueOf("C6E9FF") : GOLD);
      flying.addAction(
          Actions.sequence(
              Actions.delay(delay),
              Actions.visible(true),
              Actions.parallel(
                  Actions.moveTo(end.x - 25f, end.y - 25f, .85f),
                  Actions.scaleTo(.55f, .55f, .85f)),
              Actions.run(
                  () -> {
                    destination.addAction(
                        Actions.sequence(
                            Actions.color(GOLD, .12f), Actions.color(Color.WHITE, .2f)));
                  }),
              Actions.removeActor()));
      delay += .12f;
    }
    // No reward mutations here. Even negative-only results complete via the same single callback.
    overlay.addAction(
        Actions.sequence(Actions.delay(delay > 0 ? delay + 1.05f : .15f), Actions.run(onContinue)));
  }

  private static boolean isNegative(AcquisitionResult.Entry entry) {
    if (entry.kind() == AcquisitionResult.Kind.NEGATIVE_EFFECT) return true;
    if (entry.kind() != AcquisitionResult.Kind.ITEM) return false;
    try {
      return ItemType.valueOf(entry.id()).isCurse();
    } catch (IllegalArgumentException ignored) {
      return false;
    }
  }

  private static ItemType itemType(AcquisitionResult.Entry entry) {
    if (entry.kind() != AcquisitionResult.Kind.ITEM) return null;
    try {
      return ItemType.valueOf(entry.id());
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }

  private void addTrail(Image flying, float delay, Color colour) {
    for (int i = 1; i <= 12; i++) {
      overlay.addAction(
          Actions.sequence(
              Actions.delay(delay + i * .06f),
              Actions.run(
                  () -> {
                    if (overlay == null) return;
                    Image dot = new Image(skin.newDrawable(WHITE, colour));
                    dot.setTouchable(Touchable.disabled);
                    dot.setBounds(flying.getX() + 24f, flying.getY() + 24f, 4f, 4f);
                    overlay.addActor(dot);
                    dot.addAction(
                        Actions.sequence(
                            Actions.parallel(Actions.fadeOut(.25f), Actions.moveBy(0f, -8f, .25f)),
                            Actions.removeActor()));
                  })));
    }
  }
}
