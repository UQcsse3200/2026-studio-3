package com.csse3200.game.components.cards;

import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.cards.runtime.ResolvedCard;
import java.util.Locale;
import java.util.Objects;

/**
 * Presentation-only Scene2D card face bound to one {@link ResolvedCard}.
 *
 * <p>The widget renders the resolved name, energy cost, rarity, type, rules description, artwork
 * and upgrade marker. It performs no card lookup, upgrade calculation, input handling, targeting or
 * deck mutation. Interactive consumers should embed this actor inside their own click/drag/hover
 * actor.
 */
public final class CardWidget extends Stack {
  /** Intended in-game card width. */
  public static final float CARD_WIDTH = 225f;

  /** Intended in-game card height. */
  public static final float CARD_HEIGHT = 456f;

  private static final float FRAME_THICKNESS = 4f;
  private static final float INNER_RIM_THICKNESS = 2f;
  private static final float FACE_PADDING = 6f;
  private static final float HEADER_HEIGHT = 40f;
  private static final float ARTWORK_HEIGHT = 226f;
  private static final float META_HEIGHT = 28f;
  private static final float COST_BADGE_SIZE = 34f;
  private static final float UPGRADE_BADGE_SIZE = 24f;
  private static final float NAME_TEXT_WIDTH = 123f;
  private static final float MIN_NAME_SCALE = 0.72f;
  static final Scaling ARTWORK_SCALING = Scaling.fit;

  private final CardWidgetAssets assets;
  private final Table frame;
  private final Table artworkFrame;
  private final Table descriptionFrame;
  private final Image artwork;
  private final Label nameLabel;
  private final Label costLabel;
  private final Label metaLabel;
  private final Label descriptionLabel;
  private final Label upgradeLabel;
  private ResolvedCard card;

  /**
   * Creates a card face and binds its first resolved snapshot.
   *
   * @param card authoritative values to display
   * @param assets shared, externally owned visual assets
   */
  public CardWidget(ResolvedCard card, CardWidgetAssets assets) {
    this.assets = Objects.requireNonNull(assets, "assets cannot be null");
    setTouchable(Touchable.disabled);
    setSize(CARD_WIDTH, CARD_HEIGHT);

    frame = new Table();
    frame.setTouchable(Touchable.disabled);
    frame.pad(FRAME_THICKNESS);

    Table face = new Table();
    face.setTouchable(Touchable.disabled);
    face.setBackground(assets.cardFace());
    face.pad(FACE_PADDING);

    nameLabel = new Label("", assets.nameStyle());
    nameLabel.setAlignment(Align.center);
    nameLabel.setEllipsis(true);
    nameLabel.setTouchable(Touchable.disabled);
    Table namePlate = new Table();
    namePlate.setTouchable(Touchable.disabled);
    namePlate.setBackground(assets.namePlate());
    namePlate.padLeft(4f).padRight(4f);
    namePlate.add(nameLabel).expand().fill().minWidth(0f);

    artwork = new Image();
    // Preserve the complete authored composition for both landscape and portrait card art. The
    // dark artwork backdrop deliberately absorbs any letterboxing instead of stretching or
    // silently cropping the configured texture.
    artwork.setScaling(ARTWORK_SCALING);
    artwork.setTouchable(Touchable.disabled);
    Table artworkBackdrop = new Table();
    artworkBackdrop.setTouchable(Touchable.disabled);
    artworkBackdrop.setBackground(assets.artworkBackdrop());
    Stack artworkStack = new Stack(artworkBackdrop, artwork);
    artworkStack.setTouchable(Touchable.disabled);

    artworkFrame = new Table();
    artworkFrame.setTouchable(Touchable.disabled);
    artworkFrame.pad(2f);
    artworkFrame.add(artworkStack).expand().fill();

    metaLabel = new Label("", assets.metaStyle());
    metaLabel.setAlignment(Align.center);
    metaLabel.setTouchable(Touchable.disabled);
    Table metaPlate = new Table();
    metaPlate.setTouchable(Touchable.disabled);
    metaPlate.setBackground(assets.namePlate());
    metaPlate.add(metaLabel).expand().fill();

    descriptionLabel = new Label("", assets.descriptionStyle());
    descriptionLabel.setAlignment(Align.center);
    descriptionLabel.setWrap(true);
    descriptionLabel.setTouchable(Touchable.disabled);
    Table descriptionPanel = new Table();
    descriptionPanel.setTouchable(Touchable.disabled);
    descriptionPanel.setBackground(assets.descriptionPanel());
    descriptionPanel.pad(8f, 9f, 8f, 9f);
    descriptionPanel.add(descriptionLabel).expand().fill();

    descriptionFrame = new Table();
    descriptionFrame.setTouchable(Touchable.disabled);
    descriptionFrame.pad(2f);
    descriptionFrame.add(descriptionPanel).expand().fill();

    costLabel = new Label("", assets.costStyle());
    costLabel.setAlignment(Align.center);
    costLabel.setTouchable(Touchable.disabled);
    Table costBadge = new Table();
    costBadge.setTouchable(Touchable.disabled);
    costBadge.setBackground(assets.costBadge());
    costBadge.add(costLabel).expand().fill();

    upgradeLabel = new Label("+", assets.upgradeStyle());
    upgradeLabel.setAlignment(Align.center);
    upgradeLabel.setTouchable(Touchable.disabled);
    Table upgradeBadge = new Table();
    upgradeBadge.setTouchable(Touchable.disabled);
    upgradeBadge.setBackground(assets.upgradeBadge());
    upgradeBadge.add(upgradeLabel).expand().fill();

    Table header = new Table();
    header.setTouchable(Touchable.disabled);
    header.add(costBadge).size(COST_BADGE_SIZE).padRight(5f);
    header.add(namePlate).expandX().fillX().height(HEADER_HEIGHT).minWidth(0f);
    header.add(upgradeBadge).size(UPGRADE_BADGE_SIZE).padLeft(5f);

    face.add(header).height(HEADER_HEIGHT).expandX().fillX();
    face.row();
    face.add(artworkFrame).height(ARTWORK_HEIGHT).expandX().fillX().padTop(6f);
    face.row();
    face.add(metaPlate).height(META_HEIGHT).expandX().fillX().padTop(5f);
    face.row();
    face.add(descriptionFrame).expand().fill().padTop(5f);

    Table innerRim = new Table();
    innerRim.setTouchable(Touchable.disabled);
    innerRim.setBackground(assets.innerRim());
    innerRim.pad(INNER_RIM_THICKNESS);
    innerRim.add(face).expand().fill();
    frame.add(innerRim).expand().fill();

    add(frame);
    setCard(card);
  }

  /** Refreshes every dynamic field from one authoritative resolved snapshot. */
  public void setCard(ResolvedCard card) {
    this.card = Objects.requireNonNull(card, "card cannot be null");

    nameLabel.setText(card.name());
    fitName(card.name());
    costLabel.setText(Integer.toString(card.cost()));
    metaLabel.setText(formatEnum(card.type().name()) + "  |  " + formatEnum(card.rarity().name()));
    metaLabel.setColor(assets.colourFor(card.rarity()));
    descriptionLabel.setText(card.description());
    frame.setBackground(assets.frameFor(card.rarity()));
    artworkFrame.setBackground(assets.frameFor(card.rarity()));
    descriptionFrame.setBackground(assets.frameFor(card.rarity()));

    Drawable artworkDrawable = assets.artwork(card.texturePath());
    artwork.setDrawable(artworkDrawable);
    artwork.setVisible(artworkDrawable != null);
    upgradeLabel.getParent().setVisible(card.upgraded());

    invalidateHierarchy();
  }

  /** Returns the immutable resolved snapshot currently displayed by this widget. */
  public ResolvedCard getCard() {
    return card;
  }

  @Override
  public float getPrefWidth() {
    return CARD_WIDTH;
  }

  @Override
  public float getPrefHeight() {
    return CARD_HEIGHT;
  }

  String displayedName() {
    return nameLabel.getText().toString();
  }

  String displayedCost() {
    return costLabel.getText().toString();
  }

  String displayedMeta() {
    return metaLabel.getText().toString();
  }

  String displayedDescription() {
    return descriptionLabel.getText().toString();
  }

  Drawable displayedArtwork() {
    return artwork.getDrawable();
  }

  boolean isArtworkVisible() {
    return artwork.isVisible();
  }

  boolean displaysUpgradeMarker() {
    return upgradeLabel.getParent().isVisible();
  }

  Drawable displayedFrame() {
    return frame.getBackground();
  }

  float displayedNameScale() {
    return nameLabel.getFontScaleX();
  }

  private void fitName(String name) {
    GlyphLayout layout = new GlyphLayout(nameLabel.getStyle().font, name);
    float scale = layout.width == 0f ? 1f : Math.min(1f, NAME_TEXT_WIDTH / layout.width);
    nameLabel.setFontScale(Math.max(MIN_NAME_SCALE, scale));
  }

  private static String formatEnum(String value) {
    String lowerCase = value.toLowerCase(Locale.ROOT).replace('_', ' ');
    return Character.toUpperCase(lowerCase.charAt(0)) + lowerCase.substring(1);
  }
}
