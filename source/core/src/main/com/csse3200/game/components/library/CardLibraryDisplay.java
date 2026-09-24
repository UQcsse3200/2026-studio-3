package com.csse3200.game.components.library;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.GdxGame;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardDiscoveryService;
import com.csse3200.game.cards.CardEntryView;
import com.csse3200.game.cards.CardLoadingException;
import com.csse3200.game.cards.CardUnlockState;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.runtime.ResolvedCard;
import com.csse3200.game.components.cards.CardWidget;
import com.csse3200.game.components.cards.CardWidgetAssets;
import com.csse3200.game.components.cards.UncommonCardLibraryWidget;
import com.csse3200.game.components.mainmenu.MainMenuDisplay;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.MenuTheme;
import com.csse3200.game.ui.UIComponent;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Read-only, discovery-aware card library backed by the current card configuration file. */
public class CardLibraryDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(CardLibraryDisplay.class);
  private static final float PANEL_WIDTH = 1120f;
  private static final float PANEL_HEIGHT = 680f;
  private static final Color PANEL_COLOUR = new Color(0.105f, 0.07f, 0.065f, 0.96f);
  private static final Color LIST_COLOUR = new Color(0.13f, 0.09f, 0.085f, 1f);
  private static final Color DETAIL_COLOUR = new Color(0.075f, 0.055f, 0.065f, 1f);
  private static final Color LOCKED_COLOUR = new Color(0.035f, 0.03f, 0.04f, 1f);
  private static final String WHITE = "white";
  private static final String LARGE = "large";
  private static final String SMALL = "small";
  private static final String DEFAULT = "default";

  private final GdxGame game;
  private final CardDiscoveryService discovery;
  private final EventListener1<CardEntryView> entryUpdatedListener = this::onEntryUpdated;
  private final Map<String, TextButton> cardButtons = new HashMap<>();
  private CardEntryView displayedEntry;

  private Stack rootStack;
  private Table cardList;
  private Stack cardPreview;
  private Table lockedPreview;
  private Label lockedArtLabel;
  private CardWidget cardWidget;
  private UncommonCardLibraryWidget uncommonCardWidget;
  private CardWidgetAssets widgetAssets;

  private Label discoveryCounterLabel;
  private Label stateLabel;
  private Label nameLabel;
  private Label descriptionLabel;
  private Label costLabel;
  private Label typeLabel;
  private Label targetLabel;
  private Label rarityLabel;
  private Label effectsLabel;
  private Label artworkLabel;
  private TextButton.TextButtonStyle buttonStyle;

  public CardLibraryDisplay(GdxGame game, CardDiscoveryService discovery) {
    this.game = Objects.requireNonNull(game, "game cannot be null");
    this.discovery = Objects.requireNonNull(discovery, "discovery cannot be null");
  }

  @Override
  public void create() {
    super.create();
    addActors();
    discovery
        .getEvents()
        .addListener(CardDiscoveryService.ENTRY_UPDATED_EVENT, entryUpdatedListener);
  }

  private void addActors() {
    rootStack = new Stack();
    rootStack.setFillParent(true);
    addBackground(rootStack);

    Texture buttonFrameTexture = getTexture(MainMenuDisplay.BUTTON_FRAME_TEXTURE);
    buttonFrameTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    buttonStyle = MenuTheme.createButtonStyle(skin, buttonFrameTexture);

    Table panel = new Table();
    panel.setBackground(skin.newDrawable(WHITE, PANEL_COLOUR));
    panel.pad(24f, 32f, 28f, 32f);

    addHeader(panel);
    panel.row();
    addDivider(panel);
    panel.row();
    addCardContent(panel);

    Table wrapper = new Table();
    wrapper.setFillParent(true);
    wrapper.center().pad(MenuTheme.SCREEN_PADDING);
    wrapper.add(panel).width(PANEL_WIDTH).height(PANEL_HEIGHT);
    rootStack.add(wrapper);
    stage.addActor(rootStack);
  }

  private void addBackground(Stack stack) {
    Texture backgroundTexture = getTexture(MainMenuDisplay.BACKGROUND_TEXTURE);
    backgroundTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    Image background = new Image(backgroundTexture);
    background.setScaling(Scaling.fill);
    stack.add(background);

    Color overlayColour = MenuTheme.deepPlum();
    overlayColour.a = 0.62f;
    Table overlay = new Table();
    overlay.setBackground(skin.newDrawable(WHITE, overlayColour));
    stack.add(overlay);
  }

  private void addHeader(Table panel) {
    Table titleBlock = new Table();
    Label eyebrow = new Label("CARD ARCHIVE", labelStyle(SMALL, MenuTheme.softCoral()));
    Label title = new Label("Card Library", labelStyle(LARGE, MenuTheme.warmParchment()));
    Label subtitle =
        new Label(
            "Discover cards during a run to reveal their archive records.",
            labelStyle(SMALL, MenuTheme.warmParchment()));
    eyebrow.setFontScale(1.15f);
    title.setFontScale(1.35f);
    subtitle.setFontScale(1.05f);
    discoveryCounterLabel = new Label("", labelStyle(SMALL, MenuTheme.softCoral()));
    discoveryCounterLabel.setFontScale(1.05f);

    titleBlock.add(eyebrow).left();
    titleBlock.row();
    titleBlock.add(title).left().padTop(2f);
    titleBlock.row();
    titleBlock.add(subtitle).left().padTop(5f);
    titleBlock.row();
    titleBlock.add(discoveryCounterLabel).left().padTop(5f);

    TextButton backButton = new TextButton("Back", buttonStyle);
    backButton.getLabel().setFontScale(0.85f);
    backButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            game.setScreen(GdxGame.ScreenType.LIBRARY);
          }
        });

    panel.add(titleBlock).left().expandX();
    panel.add(backButton).right().width(170f).height(58f);
  }

  private void addDivider(Table panel) {
    Table divider = new Table();
    divider.setBackground(skin.newDrawable(WHITE, MenuTheme.softCoral()));
    panel.add(divider).colspan(2).expandX().fillX().height(2f).padTop(16f).padBottom(16f);
  }

  private void addCardContent(Table panel) {
    try {
      List<CardConfig> cards = loadSortedCards();
      List<CardEntryView> entries =
          cards.stream()
              .flatMap(
                  card -> {
                    var entry = discovery.getEntry(card.id);
                    if (entry.isEmpty()) {
                      logger.warn(
                          "Skipping card '{}' because it is not registered for discovery", card.id);
                    }
                    return entry.stream();
                  })
              .toList();
      addCards(entries, panel);
      updateDiscoveryCounter();
      if (!entries.isEmpty()) {
        showCard(entries.getFirst());
      }
    } catch (CardLoadingException exception) {
      logger.warn("Failed to load cards for library display", exception);
      Label errorLabel =
          new Label("Unable to load card library: " + exception.getMessage(), bodyLabelStyle());
      errorLabel.setWrap(true);
      panel.add(errorLabel).colspan(2).width(900f).padTop(30f).row();
    }
  }

  private List<CardConfig> loadSortedCards() {
    return CardConfigLoader.loadCards().stream()
        .sorted(
            Comparator.comparing((CardConfig card) -> card.type).thenComparing(card -> card.name))
        .toList();
  }

  private void addCards(List<CardEntryView> cards, Table panel) {
    cardList = new Table();
    cardButtons.clear();
    cardList.top();
    cardList.defaults().width(290f).height(58f).padBottom(8f).left();

    for (CardEntryView card : cards) {
      TextButton cardButton = new TextButton(formatCardButton(card), buttonStyle);
      cardButton.getLabel().setFontScale(0.75f);
      String cardId = card.cardId();
      cardButton.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              discovery.getEntry(cardId).ifPresent(CardLibraryDisplay.this::showCard);
            }
          });
      cardList.add(cardButton).row();
      cardButtons.put(cardId, cardButton);
    }

    ScrollPane scrollPane = new ScrollPane(cardList, skin);
    scrollPane.setFadeScrollBars(false);
    scrollPane.setScrollingDisabled(true, false);

    Table listPanel = new Table();
    listPanel.setBackground(skin.newDrawable(WHITE, LIST_COLOUR));
    listPanel.pad(18f);
    listPanel.add(new Label("CARDS", labelStyle(SMALL, MenuTheme.softCoral()))).left().expandX();
    listPanel.row();
    listPanel.add(scrollPane).expand().fill().padTop(12f);

    if (!cards.isEmpty()) {
      widgetAssets =
          CardWidgetAssets.fromManagedResources(skin, ServiceLocator.getResourceService());
    }

    panel.add(listPanel).width(350f).expandY().fillY().padRight(22f);
    panel.add(createDetailPanel(cards.isEmpty())).expand().fill();
  }

  private Table createDetailPanel(boolean empty) {
    Table detailPanel = new Table();
    detailPanel.setBackground(skin.newDrawable(WHITE, DETAIL_COLOUR));
    detailPanel.pad(18f);

    if (empty) {
      detailPanel.add(new Label("No cards are available.", bodyLabelStyle()));
      return detailPanel;
    }

    stateLabel = new Label("", labelStyle(SMALL, MenuTheme.softCoral()));
    stateLabel.setFontScale(1.05f);

    cardPreview = new Stack();
    lockedPreview = createLockedPreview();
    cardPreview.add(lockedPreview);

    nameLabel = new Label("", labelStyle(LARGE, MenuTheme.warmParchment()));
    descriptionLabel = new Label("", bodyLabelStyle());
    costLabel = new Label("", labelStyle(DEFAULT, MenuTheme.softCoral()));
    typeLabel = new Label("", bodyLabelStyle());
    targetLabel = new Label("", bodyLabelStyle());
    rarityLabel = new Label("", bodyLabelStyle());
    effectsLabel = new Label("", labelStyle(SMALL, MenuTheme.warmParchment()));
    artworkLabel = new Label("", labelStyle(SMALL, MenuTheme.warmParchment()));

    nameLabel.setFontScale(0.9f);
    descriptionLabel.setFontScale(0.78f);
    descriptionLabel.setWrap(true);
    effectsLabel.setFontScale(0.72f);
    effectsLabel.setWrap(true);
    artworkLabel.setFontScale(0.62f);
    artworkLabel.setWrap(true);

    Table archiveDetails = new Table();
    archiveDetails.top().left();
    archiveDetails.defaults().left().padBottom(7f);
    archiveDetails.add(nameLabel).width(330f).row();
    archiveDetails.add(descriptionLabel).width(330f).padBottom(12f).row();
    archiveDetails.add(costLabel).row();
    archiveDetails.add(typeLabel).row();
    archiveDetails.add(targetLabel).row();
    archiveDetails.add(rarityLabel).row();
    archiveDetails.add(effectsLabel).width(330f).padTop(8f).row();
    archiveDetails.add(artworkLabel).width(330f).padTop(5f).row();

    detailPanel.top();
    detailPanel.add(stateLabel).colspan(2).left().expandX().padBottom(8f);
    detailPanel.row();
    detailPanel.add(cardPreview).size(CardWidget.CARD_WIDTH, CardWidget.CARD_HEIGHT).top();
    detailPanel.add(archiveDetails).width(330f).expandY().fillY().top().padLeft(18f);
    return detailPanel;
  }

  private Table createLockedPreview() {
    Table preview = new Table();
    preview.setBackground(skin.newDrawable(WHITE, LOCKED_COLOUR));
    lockedArtLabel = new Label("?", labelStyle(LARGE, MenuTheme.warmParchment()));
    lockedArtLabel.setFontScale(4f);
    Label lockedMessage = new Label("UNDISCOVERED", labelStyle(SMALL, MenuTheme.warmParchment()));
    preview.add(lockedArtLabel).center();
    preview.row();
    preview.add(lockedMessage).center().padTop(14f);
    return preview;
  }

  static String formatCardButton(CardEntryView card) {
    if (card.unlockState() == CardUnlockState.LOCKED) {
      return "???";
    }
    return card.cost().orElseThrow() + "  " + card.displayName();
  }

  void showCard(CardEntryView card) {
    displayedEntry = Objects.requireNonNull(card, "card cannot be null");
    nameLabel.setText(card.displayName());
    descriptionLabel.setText(descriptionFor(card));
    costLabel.setText("Cost: " + valueOrPlaceholder(card.cost()));
    typeLabel.setText("Type: " + card.type().map(Enum::name).orElse("???"));
    targetLabel.setText("Target: " + card.target().map(Enum::name).orElse("???"));
    rarityLabel.setText("Rarity: " + card.rarity().map(Enum::name).orElse("???"));

    if (card.unlockState() == CardUnlockState.LOCKED) {
      showLockedCard();
      return;
    }

    stateLabel.setText(card.unlockState().name());
    effectsLabel.setText(
        "Effects resolve in this order:\n" + formatEffects(card.effects().orElseThrow()));
    artworkLabel.setText("Artwork: " + card.texturePath().orElseThrow());
    showResolvedCard(resolveForDisplay(card));
  }

  private void showLockedCard() {
    stateLabel.setText("UNDISCOVERED");
    effectsLabel.setText("Effects: ???");
    artworkLabel.setText("Artwork: ???");
    lockedPreview.setVisible(true);
    if (cardWidget != null) {
      cardWidget.setVisible(false);
    }
    if (uncommonCardWidget != null) {
      uncommonCardWidget.setVisible(false);
    }
  }

  private void showResolvedCard(ResolvedCard card) {
    boolean useUncommonFrame = card.rarity() == Rarity.UNCOMMON;
    lockedPreview.setVisible(false);

    if (useUncommonFrame) {
      ensureUncommonWidget(card);
      uncommonCardWidget.setCard(card);
    } else {
      ensureStandardWidget(card);
      cardWidget.setCard(card);
    }

    if (cardWidget != null) {
      cardWidget.setVisible(!useUncommonFrame);
    }
    if (uncommonCardWidget != null) {
      uncommonCardWidget.setVisible(useUncommonFrame);
    }
  }

  private void ensureStandardWidget(ResolvedCard card) {
    if (cardWidget == null) {
      cardWidget = new CardWidget(card, widgetAssets);
      cardPreview.add(cardWidget);
    }
  }

  private void ensureUncommonWidget(ResolvedCard card) {
    if (uncommonCardWidget != null) {
      return;
    }
    Texture frameTexture = getTexture(UncommonCardLibraryWidget.FRAME_TEXTURE);
    frameTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    uncommonCardWidget =
        new UncommonCardLibraryWidget(
            card, widgetAssets, new TextureRegionDrawable(new TextureRegion(frameTexture)));
    cardPreview.add(uncommonCardWidget);
  }

  static ResolvedCard resolveForDisplay(CardEntryView card) {
    Objects.requireNonNull(card, "card cannot be null");
    if (!card.unlockState().isAtLeast(CardUnlockState.SEEN)) {
      throw new IllegalArgumentException("locked card cannot be resolved for display");
    }
    return new ResolvedCard(
        "library-preview-" + card.cardId(),
        card.cardId(),
        card.displayName(),
        card.description().orElseThrow(),
        card.cost().orElseThrow(),
        card.type().orElseThrow(),
        card.rarity().orElseThrow(),
        card.target().orElseThrow(),
        card.effects().orElseThrow(),
        card.texturePath().orElseThrow(),
        false);
  }

  static String descriptionFor(CardEntryView card) {
    if (card.unlockState() == CardUnlockState.LOCKED) {
      return "Find this card to reveal its record.";
    }
    return card.description().orElse("");
  }

  private static String valueOrPlaceholder(java.util.OptionalInt value) {
    return value.isPresent() ? Integer.toString(value.getAsInt()) : "???";
  }

  private String formatEffects(List<EffectConfig> effects) {
    if (effects == null || effects.isEmpty()) {
      return "None";
    }
    return effects.stream().map(this::formatEffect).reduce((a, b) -> a + "\n" + b).orElse("");
  }

  private String formatEffect(EffectConfig effect) {
    String text = "- " + effect.type + " value " + effect.value;
    if (effect.duration > 0) {
      text += " for " + effect.duration + " turns";
    }
    return text;
  }

  private void onEntryUpdated(CardEntryView entry) {
    TextButton button = cardButtons.get(entry.cardId());
    if (button != null) {
      button.setText(formatCardButton(entry));
    }
    if (displayedEntry != null && displayedEntry.cardId().equals(entry.cardId())) {
      showCard(entry);
    }
    updateDiscoveryCounter();
  }

  private void updateDiscoveryCounter() {
    if (discoveryCounterLabel == null) {
      return;
    }
    Map<String, CardUnlockState> progress = discovery.getProgressSnapshot();
    long discovered =
        progress.values().stream().filter(state -> state == CardUnlockState.SEEN).count();
    discoveryCounterLabel.setText(discovered + " / " + progress.size() + " discovered");
  }

  CardEntryView getDisplayedEntry() {
    return displayedEntry;
  }

  String getStateText() {
    return stateLabel.getText().toString();
  }

  String getDescriptionText() {
    return descriptionLabel.getText().toString();
  }

  String getCostText() {
    return costLabel.getText().toString();
  }

  boolean isLockedArtworkVisible() {
    return lockedPreview.isVisible()
        && lockedArtLabel.isVisible()
        && (cardWidget == null || !cardWidget.isVisible())
        && (uncommonCardWidget == null || !uncommonCardWidget.isVisible());
  }

  boolean isStandardCardVisible() {
    return cardWidget != null && cardWidget.isVisible();
  }

  boolean isUncommonCardVisible() {
    return uncommonCardWidget != null && uncommonCardWidget.isVisible();
  }

  private Label.LabelStyle bodyLabelStyle() {
    return labelStyle(DEFAULT, MenuTheme.warmParchment());
  }

  private Label.LabelStyle labelStyle(String baseStyle, Color colour) {
    Label.LabelStyle style = new Label.LabelStyle(skin.get(baseStyle, Label.LabelStyle.class));
    style.fontColor = colour;
    return style;
  }

  private Texture getTexture(String path) {
    return ServiceLocator.getResourceService().getAsset(path, Texture.class);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // Rendering handled by the stage.
  }

  @Override
  public void update() {
    stage.act(ServiceLocator.getTimeSource().getDeltaTime());
  }

  @Override
  public void dispose() {
    discovery
        .getEvents()
        .removeListener(CardDiscoveryService.ENTRY_UPDATED_EVENT, entryUpdatedListener);
    if (rootStack != null) {
      rootStack.remove();
      rootStack.clear();
    }
    super.dispose();
  }
}
