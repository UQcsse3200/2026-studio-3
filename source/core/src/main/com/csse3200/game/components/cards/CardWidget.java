package com.csse3200.game.components.cards;

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

  private static final float FRAME_THICKNESS = 6f;
  private static final float FACE_PADDING = 8f;
  private static final float NAME_HEIGHT = 44f;
  private static final float ARTWORK_HEIGHT = 236f;
  private static final float META_HEIGHT = 24f;
  private static final float COST_BADGE_SIZE = 48f;
  private static final float UPGRADE_BADGE_SIZE = 40f;
  static final Scaling ARTWORK_SCALING = Scaling.fit;

  private final CardWidgetAssets assets;
  private final Table frame;
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
    namePlate.padLeft(COST_BADGE_SIZE * 0.65f).padRight(UPGRADE_BADGE_SIZE * 0.45f);
    namePlate.add(nameLabel).expand().fill();

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

    metaLabel = new Label("", assets.metaStyle());
    metaLabel.setAlignment(Align.center);
    metaLabel.setTouchable(Touchable.disabled);

    descriptionLabel = new Label("", assets.descriptionStyle());
    descriptionLabel.setAlignment(Align.center);
    descriptionLabel.setWrap(true);
    descriptionLabel.setTouchable(Touchable.disabled);
    Table descriptionPanel = new Table();
    descriptionPanel.setTouchable(Touchable.disabled);
    descriptionPanel.setBackground(assets.descriptionPanel());
    descriptionPanel.pad(8f, 9f, 8f, 9f);
    descriptionPanel.add(descriptionLabel).expand().fill();

    face.add(namePlate).height(NAME_HEIGHT).expandX().fillX();
    face.row();
    face.add(artworkStack).height(ARTWORK_HEIGHT).expandX().fillX().padTop(6f);
    face.row();
    face.add(metaLabel).height(META_HEIGHT).expandX().fillX().padTop(5f);
    face.row();
    face.add(descriptionPanel).expand().fill().padTop(5f);
    frame.add(face).expand().fill();

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

    Table badges = new Table();
    badges.setTouchable(Touchable.disabled);
    badges.top();
    badges.add(costBadge).size(COST_BADGE_SIZE).left().pad(2f);
    badges.add().expandX();
    badges.add(upgradeBadge).size(UPGRADE_BADGE_SIZE).right().pad(4f);

    add(frame);
    add(badges);
    setCard(card);
  }

  /** Refreshes every dynamic field from one authoritative resolved snapshot. */
  public void setCard(ResolvedCard card) {
    this.card = Objects.requireNonNull(card, "card cannot be null");

    nameLabel.setText(card.name());
    costLabel.setText(Integer.toString(card.cost()));
    metaLabel.setText(formatEnum(card.type().name()) + "  |  " + formatEnum(card.rarity().name()));
    metaLabel.setColor(assets.colourFor(card.rarity()));
    descriptionLabel.setText(card.description());
    frame.setBackground(assets.frameFor(card.rarity()));

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

  private static String formatEnum(String value) {
    String lowerCase = value.toLowerCase(Locale.ROOT).replace('_', ' ');
    return Character.toUpperCase(lowerCase.charAt(0)) + lowerCase.substring(1);
  }
}
