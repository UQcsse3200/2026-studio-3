package com.csse3200.game.components.library;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
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
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.services.audio.AudioService;
import com.csse3200.game.services.audio.SoundId;
import com.csse3200.game.ui.LibraryNameSearch;
import com.csse3200.game.ui.MenuTheme;
import com.csse3200.game.ui.UIComponent;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Read-only, discovery-aware card library backed by the current card configuration file. */
public class CardLibraryDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(CardLibraryDisplay.class);
  public static final String BUTTON_TEXTURE = "images/card_library_button_frame.png";
  private static final float PANEL_WIDTH = 1120f;
  private static final float PANEL_HEIGHT = 680f;
  private static final Color PANEL_COLOUR = new Color(0.105f, 0.07f, 0.065f, 0.96f);
  private static final Color LIST_COLOUR = new Color(0.13f, 0.09f, 0.085f, 1f);
  private static final Color DETAIL_COLOUR = new Color(0.075f, 0.055f, 0.065f, 1f);
  private static final Color LOCKED_COLOUR = new Color(0.035f, 0.03f, 0.04f, 1f);
  private static final Color UPGRADED_COLOUR = Color.valueOf("80ED99");
  private static final String WHITE = "white";
  private static final String LARGE = "large";
  private static final String SMALL = "small";
  private static final String DEFAULT = "default";

  private final GdxGame game;
  private final CardDiscoveryService discovery;
  private final EventListener1<CardEntryView> entryUpdatedListener = this::onEntryUpdated;
  private final Map<String, TextButton> cardButtons = new HashMap<>();
  private final Set<String> upgradedPreviews = new HashSet<>();
  private List<String> cardOrder = List.of();
  private CardEntryView displayedEntry;

  private Stack rootStack;
  private Table cardList;
  private ScrollPane cardScroll;
  private TextField searchField;
  private ChangeListener searchChangedListener;
  private Label resultCountLabel;
  private Label noResultsLabel;
  private Table detailContent;
  private Table selectionPrompt;
  private Label selectionMessage;
  private Stack cardPreview;
  private Table lockedPreview;
  private Label lockedArtLabel;
  private CardWidget cardWidget;
  private UncommonCardLibraryWidget uncommonCardWidget;
  private CardWidgetAssets widgetAssets;

  private Label discoveryCounterLabel;
  private Label stateLabel;
  private CheckBox upgradePreview;
  private ChangeListener upgradeChangedListener;
  private Label nameLabel;
  private Label descriptionLabel;
  private Label loreLabel;
  private Cell<Label> loreCell;
  private Label costLabel;
  private Label typeLabel;
  private Label targetLabel;
  private Label rarityLabel;
  private Label effectsLabel;
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
    LibraryUiHelper.addBackground(skin, rootStack);

    buttonStyle = LibraryUiHelper.createButtonStyle(skin);

    Table panel = new Table();
    panel.setBackground(skin.newDrawable(WHITE, PANEL_COLOUR));
    panel.pad(24f, 32f, 28f, 32f);

    addHeader(panel);
    panel.row();
    LibraryUiHelper.addDivider(skin, panel);
    panel.row();
    addCardContent(panel);

    Table wrapper = new Table();
    wrapper.setFillParent(true);
    wrapper.center().pad(MenuTheme.SCREEN_PADDING);
    wrapper.add(panel).width(PANEL_WIDTH).height(PANEL_HEIGHT);
    rootStack.add(wrapper);
    stage.addActor(rootStack);
  }

  private void addHeader(Table panel) {
    Table titleBlock = new Table();
    Label eyebrow =
        new Label("CARD LIBRARY", LibraryUiHelper.labelStyle(skin, SMALL, MenuTheme.softCoral()));
    Label title =
        new Label(
            "Card Library", LibraryUiHelper.labelStyle(skin, LARGE, MenuTheme.warmParchment()));
    Label subtitle =
        new Label(
            "Discover cards during a run to reveal their lore.",
            LibraryUiHelper.labelStyle(skin, SMALL, MenuTheme.warmParchment()));
    eyebrow.setFontScale(1.15f);
    title.setFontScale(1.35f);
    subtitle.setFontScale(1.05f);
    discoveryCounterLabel =
        new Label("", LibraryUiHelper.labelStyle(skin, SMALL, MenuTheme.softCoral()));
    discoveryCounterLabel.setFontScale(1.05f);

    LibraryUiHelper.populateTitleBlock(titleBlock, eyebrow, title, subtitle);
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
    backButton.addListener(
        new InputListener() {
          @Override
          public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            if (pointer == -1) {
              AudioService.playSound(SoundId.MENU_HOVER, 0.5f);
            }
          }
        });
    panel.add(titleBlock).left().expandX();
    panel.add(backButton).right().width(170f).height(58f);
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
            Comparator.comparing((CardConfig card) -> card.rarity).thenComparing(card -> card.name))
        .toList();
  }

  private void addCards(List<CardEntryView> cards, Table panel) {
    cardOrder = cards.stream().map(CardEntryView::cardId).toList();
    cardList = new Table();
    cardList.setName("card-library-list");
    cardButtons.clear();
    cardList.top();
    cardList.defaults().width(290f).height(76f).padBottom(8f).left();

    ButtonGroup<TextButton> cardGroup = new ButtonGroup<>();
    cardGroup.setMinCheckCount(0);
    cardGroup.setMaxCheckCount(1);
    cardGroup.setUncheckLast(true);
    TextButton firstButton = null;

    for (CardEntryView card : cards) {
      TextButton cardButton = new TextButton(formatCardButton(card), buttonStyle);
      cardButton.getLabel().setFontScale(0.75f);
      String cardId = card.cardId();
      cardGroup.add(cardButton);
      if (firstButton == null) {
        firstButton = cardButton;
      }
      cardButton.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              if (cardButton.isChecked() && cardButton.getParent() == cardList) {
                discovery.getEntry(cardId).ifPresent(CardLibraryDisplay.this::showCard);
              }
            }
          });
      cardButton.addListener(
          new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
              if (pointer == -1) {
                AudioService.playSound(SoundId.MENU_HOVER, 0.5f);
              }
            }
          });
      cardList.add(cardButton).row();
      cardButtons.put(cardId, cardButton);
    }

    cardScroll = new ScrollPane(cardList, skin);
    cardScroll.setFadeScrollBars(false);
    cardScroll.setScrollingDisabled(true, false);

    Table listPanel = new Table();
    listPanel.setBackground(skin.newDrawable(WHITE, LIST_COLOUR));
    listPanel.pad(18f);
    listPanel
        .add(new Label("CARDS", LibraryUiHelper.labelStyle(skin, SMALL, MenuTheme.softCoral())))
        .left()
        .expandX();
    listPanel.row();
    addSearchControls(listPanel);
    listPanel.row();
    listPanel.add(cardScroll).expand().fill().padTop(12f);

    if (!cards.isEmpty()) {
      widgetAssets =
          CardWidgetAssets.fromManagedResources(skin, ServiceLocator.getResourceService());
    }

    panel.add(listPanel).width(350f).expandY().fillY().padRight(22f);
    panel.add(createDetailPanel(cards.isEmpty())).expand().fill();

    // Filtering may leave no selected card; retain single-selection without choosing a hidden one.
    cardGroup.setMinCheckCount(0);
    if (firstButton != null) {
      firstButton.setChecked(true);
    }
    refreshCards(false);
  }

  private void addSearchControls(Table listPanel) {
    listPanel
        .add(
            new Label(
                "Search discovered cards",
                LibraryUiHelper.labelStyle(skin, SMALL, MenuTheme.warmParchment())))
        .left()
        .padTop(10f);
    listPanel.row();
    searchField = new TextField("", skin);
    searchField.setProgrammaticChangeEvents(true);
    searchField.setName("card-library-search");
    searchField.setMessageText("Card name...");
    searchChangedListener =
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            refreshCards(true);
          }
        };
    searchField.addListener(searchChangedListener);
    TextButton clear = new TextButton("Clear", skin);
    clear.setName("card-library-search-clear");
    clear.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            searchField.setText("");
            stage.setKeyboardFocus(searchField);
          }
        });
    Table searchRow = new Table();
    searchRow.add(searchField).growX().minWidth(0f).height(42f);
    searchRow.add(clear).width(76f).height(42f).padLeft(8f);
    listPanel.add(searchRow).growX().padTop(5f);
    listPanel.row();
    resultCountLabel =
        new Label("", LibraryUiHelper.labelStyle(skin, SMALL, MenuTheme.softCoral()));
    resultCountLabel.setName("card-library-result-count");
    listPanel.add(resultCountLabel).left().padTop(8f);
    noResultsLabel = new Label("No matches", bodyLabelStyle());
    noResultsLabel.setName("card-library-no-results");
  }

  static boolean matchesSearch(CardEntryView card, String query) {
    String normalized = LibraryNameSearch.normalize(query);
    return normalized.isEmpty()
        || (card.unlockState().isAtLeast(CardUnlockState.SEEN)
            && LibraryNameSearch.matches(card.displayName(), normalized));
  }

  private void refreshCards(boolean resetScroll) {
    if (searchField == null) return;
    List<CardEntryView> visible =
        cardOrder.stream()
            .flatMap(id -> discovery.getEntry(id).stream())
            .filter(card -> matchesSearch(card, searchField.getText()))
            .toList();
    Actor focus = stage.getKeyboardFocus();
    cardList.clearChildren();
    for (CardEntryView card : visible) {
      TextButton button = cardButtons.get(card.cardId());
      button.setText(formatCardButton(card));
      cardList.add(button).row();
    }
    if (focus != null && focus.isDescendantOf(cardList)) stage.setKeyboardFocus(focus);
    boolean searching = !searchField.getText().trim().isEmpty();
    String countSuffix = " cards";
    if (searching) {
      countSuffix = visible.size() == 1 ? " result" : " results";
    }
    resultCountLabel.setText(visible.size() + countSuffix);
    if (visible.isEmpty()) cardList.add(noResultsLabel).padTop(16f);
    refreshSelection(visible);
    if (selectionMessage != null)
      selectionMessage.setText(
          visible.isEmpty()
              ? "No discovered cards match your search."
              : "Select a card to view its details.");
    if (resetScroll) {
      cardScroll.setScrollY(0f);
      cardScroll.updateVisualScroll();
    }
  }

  private void refreshSelection(List<CardEntryView> visible) {
    if (displayedEntry != null) {
      CardEntryView current =
          visible.stream()
              .filter(card -> card.cardId().equals(displayedEntry.cardId()))
              .findFirst()
              .orElse(null);
      if (current != null) showCard(current);
      else {
        cardButtons.get(displayedEntry.cardId()).setChecked(false);
        clearDetails();
      }
    }
  }

  private void clearDetails() {
    displayedEntry = null;
    detailContent.setVisible(false);
    selectionPrompt.setVisible(true);
    upgradePreview.setChecked(false);
    upgradePreview.setDisabled(true);
    upgradePreview.setVisible(false);
    for (Label label :
        new Label[] {
          stateLabel,
          nameLabel,
          descriptionLabel,
          loreLabel,
          costLabel,
          typeLabel,
          targetLabel,
          rarityLabel,
          effectsLabel
        }) label.setText("");
    lockedPreview.setVisible(false);
    if (cardWidget != null) cardWidget.setVisible(false);
    if (uncommonCardWidget != null) uncommonCardWidget.setVisible(false);
  }

  private Table createDetailPanel(boolean empty) {
    Table detailPanel = new Table();
    detailPanel.setBackground(skin.newDrawable(WHITE, DETAIL_COLOUR));
    detailPanel.pad(18f);

    if (empty) {
      detailPanel.add(new Label("No cards are available.", bodyLabelStyle()));
      return detailPanel;
    }

    stateLabel = new Label("", LibraryUiHelper.labelStyle(skin, SMALL, MenuTheme.softCoral()));
    stateLabel.setFontScale(1.05f);
    upgradePreview = new CheckBox(" Upgraded", skin);
    upgradePreview.setName("card-library-upgrade-preview");
    upgradePreview.setProgrammaticChangeEvents(false);
    upgradePreview.getLabel().setFontScale(0.8f);
    upgradePreview.getLabel().setColor(MenuTheme.warmParchment());
    upgradePreview.getImageCell().size(24f);
    upgradeChangedListener =
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (displayedEntry == null || upgradePreview.isDisabled()) return;
            String cardId = displayedEntry.cardId();
            if (upgradePreview.isChecked()) upgradedPreviews.add(cardId);
            else upgradedPreviews.remove(cardId);
            discovery.getEntry(cardId).ifPresent(CardLibraryDisplay.this::showCard);
          }
        };
    upgradePreview.addListener(upgradeChangedListener);

    cardPreview = new Stack();
    lockedPreview = createLockedPreview();
    cardPreview.add(lockedPreview);

    nameLabel = new Label("", LibraryUiHelper.labelStyle(skin, LARGE, MenuTheme.warmParchment()));
    descriptionLabel = new Label("", bodyLabelStyle());
    loreLabel = new Label("", bodyLabelStyle());
    costLabel = new Label("", LibraryUiHelper.labelStyle(skin, DEFAULT, MenuTheme.softCoral()));
    typeLabel = new Label("", bodyLabelStyle());
    targetLabel = new Label("", bodyLabelStyle());
    rarityLabel = new Label("", bodyLabelStyle());
    effectsLabel =
        new Label("", LibraryUiHelper.labelStyle(skin, SMALL, MenuTheme.warmParchment()));

    nameLabel.setFontScale(0.9f);
    descriptionLabel.setFontScale(0.78f);
    descriptionLabel.setWrap(true);
    loreLabel.setFontScale(0.78f);
    loreLabel.setWrap(true);
    effectsLabel.setFontScale(0.72f);
    effectsLabel.setWrap(true);

    Table archiveDetails = new Table();
    archiveDetails.top().left();
    archiveDetails.defaults().left().padBottom(7f);
    archiveDetails.add(nameLabel).width(330f).row();
    archiveDetails.add(descriptionLabel).width(330f).padBottom(12f).row();
    loreCell = archiveDetails.add(loreLabel).width(330f).padBottom(0f);
    archiveDetails.row();
    archiveDetails.add(costLabel).row();
    archiveDetails.add(typeLabel).row();
    archiveDetails.add(targetLabel).row();
    archiveDetails.add(rarityLabel).row();
    archiveDetails.add(effectsLabel).width(330f).padTop(8f).row();

    detailContent = new Table();
    detailContent.top();
    detailContent.add(stateLabel).left().expandX().padBottom(8f);
    detailContent.add(upgradePreview).right().padBottom(8f);
    detailContent.row();
    detailContent.add(cardPreview).size(CardWidget.CARD_WIDTH, CardWidget.CARD_HEIGHT).top();
    ScrollPane detailsScroll = new ScrollPane(archiveDetails, skin);
    detailsScroll.setFadeScrollBars(false);
    detailsScroll.setScrollingDisabled(true, false);
    detailContent.add(detailsScroll).width(330f).expandY().fillY().top().padLeft(18f);
    selectionMessage = new Label("Select a card to view its details.", bodyLabelStyle());
    selectionMessage.setName("card-library-selection-message");
    selectionMessage.setWrap(true);
    selectionPrompt = new Table();
    selectionPrompt.add(selectionMessage).growX();
    selectionPrompt.setVisible(false);
    detailPanel.add(new Stack(detailContent, selectionPrompt)).grow();
    return detailPanel;
  }

  private Table createLockedPreview() {
    Table preview = new Table();
    preview.setBackground(skin.newDrawable(WHITE, LOCKED_COLOUR));
    lockedArtLabel =
        new Label("?", LibraryUiHelper.labelStyle(skin, LARGE, MenuTheme.warmParchment()));
    lockedArtLabel.setFontScale(4f);
    Label lockedMessage =
        new Label(
            "UNDISCOVERED", LibraryUiHelper.labelStyle(skin, SMALL, MenuTheme.warmParchment()));
    preview.add(lockedArtLabel).center();
    preview.row();
    preview.add(lockedMessage).center().padTop(14f);
    return preview;
  }

  static String formatCardButton(CardEntryView card) {
    if (card.unlockState() == CardUnlockState.LOCKED) {
      return "???";
    }
    return card.displayName();
  }

  void showCard(CardEntryView card) {
    displayedEntry = Objects.requireNonNull(card, "card cannot be null");
    detailContent.setVisible(true);
    selectionPrompt.setVisible(false);
    nameLabel.setText(card.displayName());
    descriptionLabel.setText(descriptionFor(card));
    String lore = card.lore().orElse("");
    loreLabel.setText(lore);
    if (lore.isEmpty()) {
      loreCell.setActor(null);
      loreCell.padBottom(0f);
    } else {
      loreCell.setActor(loreLabel);
      loreCell.padBottom(12f);
    }
    costLabel.setText("Cost: " + valueOrPlaceholder(card.cost()));
    typeLabel.setText("Type: " + card.type().map(Enum::name).orElse("???"));
    targetLabel.setText("Target: " + card.target().map(Enum::name).orElse("???"));
    rarityLabel.setText("Rarity: " + card.rarity().map(Enum::name).orElse("???"));

    if (card.unlockState() == CardUnlockState.LOCKED) {
      upgradePreview.setChecked(false);
      upgradePreview.setDisabled(true);
      upgradePreview.setVisible(false);
      showLockedCard();
      return;
    }

    var upgraded = discovery.getUpgradePreview(card.cardId());
    upgradePreview.setVisible(true);
    upgradePreview.setDisabled(upgraded.isEmpty());
    upgradePreview.setChecked(upgraded.isPresent() && upgradedPreviews.contains(card.cardId()));
    ResolvedCard resolved =
        upgradePreview.isChecked() ? upgraded.orElseThrow() : resolveForDisplay(card);
    nameLabel.setText(resolved.name());
    descriptionLabel.setText(resolved.description());
    nameLabel.setColor(resolved.upgraded() ? UPGRADED_COLOUR : Color.WHITE);
    descriptionLabel.setColor(resolved.upgraded() ? UPGRADED_COLOUR : Color.WHITE);
    costLabel.setText("Cost: " + resolved.cost());
    costLabel.setColor(resolved.upgraded() ? UPGRADED_COLOUR : Color.WHITE);
    typeLabel.setText("Type: " + resolved.type().name());
    targetLabel.setText("Target: " + resolved.target().name());
    rarityLabel.setText("Rarity: " + resolved.rarity().name());
    stateLabel.setText(card.unlockState().name());
    effectsLabel.setText("Effects resolve in this order:\n" + formatEffects(resolved.effects()));
    showResolvedCard(resolved);
  }

  private void showLockedCard() {
    nameLabel.setColor(Color.WHITE);
    descriptionLabel.setColor(Color.WHITE);
    costLabel.setColor(Color.WHITE);
    stateLabel.setText("UNDISCOVERED");
    effectsLabel.setText("Effects: ???");
    lockedPreview.setVisible(true);
    if (cardWidget != null) {
      cardWidget.setVisible(false);
    }
    if (uncommonCardWidget != null) {
      uncommonCardWidget.setVisible(false);
    }
  }

  private void showResolvedCard(ResolvedCard card) {
    boolean useUncommonFrame =
        card.rarity() == Rarity.UNCOMMON && !widgetAssets.hasAuthoredFrame(card.rarity());
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
      return "Find this card to reveal its lore.";
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
    if (!entry.unlockState().isAtLeast(CardUnlockState.SEEN)) {
      upgradedPreviews.remove(entry.cardId());
    }
    refreshCards(false);
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

  String getLoreText() {
    return loreLabel.getText().toString();
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
    return LibraryUiHelper.labelStyle(skin, DEFAULT, MenuTheme.warmParchment());
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
    if (searchField != null) searchField.removeListener(searchChangedListener);
    if (upgradePreview != null) upgradePreview.removeListener(upgradeChangedListener);
    if (rootStack != null) {
      Actor focus = stage.getKeyboardFocus();
      if (focus != null && focus.isDescendantOf(rootStack)) stage.setKeyboardFocus(null);
      Actor scrollFocus = stage.getScrollFocus();
      if (scrollFocus != null && scrollFocus.isDescendantOf(rootStack)) stage.setScrollFocus(null);
      rootStack.remove();
      rootStack.clear();
    }
    cardButtons.clear();
    upgradedPreviews.clear();
    cardOrder = List.of();
    searchField = null;
    searchChangedListener = null;
    super.dispose();
  }
}
