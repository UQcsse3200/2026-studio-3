package com.csse3200.game.components.cards;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.services.ResourceService;
import java.util.Objects;

/**
 * Shared visual dependencies for {@link CardWidget}.
 *
 * <p>The widget deliberately receives these assets instead of loading or disposing textures. This
 * keeps texture ownership in the screen's {@link ResourceService} and lets read-only and
 * interactive card consumers reuse one presentation component.
 */
public final class CardWidgetAssets {
  /** Resolves an already-managed artwork drawable for a configured texture path. */
  @FunctionalInterface
  public interface ArtworkProvider {
    Drawable get(String texturePath);
  }

  private static final Color CARD_FACE = new Color(0.075f, 0.065f, 0.09f, 1f);
  private static final Color NAME_PLATE = new Color(0.16f, 0.095f, 0.07f, 0.98f);
  private static final Color ARTWORK_BACKDROP = new Color(0.025f, 0.025f, 0.04f, 1f);
  private static final Color DESCRIPTION_PANEL = new Color(0.84f, 0.76f, 0.61f, 1f);
  private static final Color COST_BADGE = new Color(0.12f, 0.31f, 0.59f, 1f);
  private static final Color UPGRADE_BADGE = new Color(0.18f, 0.48f, 0.29f, 1f);
  private static final Color COMMON_FRAME = new Color(0.49f, 0.42f, 0.36f, 1f);
  private static final Color UNCOMMON_FRAME = new Color(0.30f, 0.57f, 0.72f, 1f);
  private static final Color RARE_FRAME = new Color(0.91f, 0.64f, 0.19f, 1f);
  private static final Color LIGHT_TEXT = new Color(0.96f, 0.91f, 0.80f, 1f);
  private static final Color DARK_TEXT = new Color(0.12f, 0.08f, 0.07f, 1f);

  private final Drawable cardFace;
  private final Drawable namePlate;
  private final Drawable artworkBackdrop;
  private final Drawable descriptionPanel;
  private final Drawable costBadge;
  private final Drawable upgradeBadge;
  private final Drawable commonFrame;
  private final Drawable uncommonFrame;
  private final Drawable rareFrame;
  private final Label.LabelStyle nameStyle;
  private final Label.LabelStyle costStyle;
  private final Label.LabelStyle metaStyle;
  private final Label.LabelStyle descriptionStyle;
  private final Label.LabelStyle upgradeStyle;
  private final ArtworkProvider artworkProvider;

  private CardWidgetAssets(Skin skin, ArtworkProvider artworkProvider) {
    Objects.requireNonNull(skin, "skin cannot be null");
    this.artworkProvider =
        Objects.requireNonNull(artworkProvider, "artworkProvider cannot be null");

    cardFace = skin.newDrawable("white", CARD_FACE);
    namePlate = skin.newDrawable("white", NAME_PLATE);
    artworkBackdrop = skin.newDrawable("white", ARTWORK_BACKDROP);
    descriptionPanel = skin.newDrawable("white", DESCRIPTION_PANEL);
    costBadge = skin.newDrawable("white", COST_BADGE);
    upgradeBadge = skin.newDrawable("white", UPGRADE_BADGE);
    commonFrame = skin.newDrawable("white", COMMON_FRAME);
    uncommonFrame = skin.newDrawable("white", UNCOMMON_FRAME);
    rareFrame = skin.newDrawable("white", RARE_FRAME);

    nameStyle = copyLabelStyle(skin, "default", LIGHT_TEXT);
    costStyle = copyLabelStyle(skin, "large", Color.WHITE);
    metaStyle = copyLabelStyle(skin, "small", LIGHT_TEXT);
    descriptionStyle = copyLabelStyle(skin, "small", DARK_TEXT);
    upgradeStyle = copyLabelStyle(skin, "large", Color.WHITE);
  }

  /**
   * Creates the shared card style with a caller-supplied artwork provider.
   *
   * @param skin UI skin containing the project's fonts and white tintable drawable
   * @param artworkProvider provider for already-owned artwork drawables
   * @return reusable card-widget assets
   */
  public static CardWidgetAssets fromSkin(Skin skin, ArtworkProvider artworkProvider) {
    return new CardWidgetAssets(skin, artworkProvider);
  }

  /**
   * Creates assets backed by textures that have already been loaded through {@link
   * ResourceService}.
   *
   * @param skin UI skin containing the project's fonts and white tintable drawable
   * @param resources resource owner containing every card artwork texture used by the consumer
   * @return reusable card-widget assets
   */
  public static CardWidgetAssets fromManagedResources(Skin skin, ResourceService resources) {
    Objects.requireNonNull(resources, "resources cannot be null");
    return fromSkin(
        skin,
        texturePath -> {
          if (!resources.containsAsset(texturePath, Texture.class)) {
            return null;
          }
          Texture texture = resources.getAsset(texturePath, Texture.class);
          texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
          return new TextureRegionDrawable(new TextureRegion(texture));
        });
  }

  Drawable cardFace() {
    return cardFace;
  }

  Drawable namePlate() {
    return namePlate;
  }

  Drawable artworkBackdrop() {
    return artworkBackdrop;
  }

  Drawable descriptionPanel() {
    return descriptionPanel;
  }

  Drawable costBadge() {
    return costBadge;
  }

  Drawable upgradeBadge() {
    return upgradeBadge;
  }

  Drawable frameFor(Rarity rarity) {
    return switch (Objects.requireNonNull(rarity, "rarity cannot be null")) {
      case COMMON -> commonFrame;
      case UNCOMMON -> uncommonFrame;
      case RARE -> rareFrame;
    };
  }

  Color colourFor(Rarity rarity) {
    return switch (Objects.requireNonNull(rarity, "rarity cannot be null")) {
      case COMMON -> new Color(COMMON_FRAME);
      case UNCOMMON -> new Color(UNCOMMON_FRAME);
      case RARE -> new Color(RARE_FRAME);
    };
  }

  Label.LabelStyle nameStyle() {
    return nameStyle;
  }

  Label.LabelStyle costStyle() {
    return costStyle;
  }

  Label.LabelStyle metaStyle() {
    return metaStyle;
  }

  Label.LabelStyle descriptionStyle() {
    return descriptionStyle;
  }

  Label.LabelStyle upgradeStyle() {
    return upgradeStyle;
  }

  Drawable artwork(String texturePath) {
    return artworkProvider.get(texturePath);
  }

  private static Label.LabelStyle copyLabelStyle(Skin skin, String name, Color colour) {
    Label.LabelStyle style = new Label.LabelStyle(skin.get(name, Label.LabelStyle.class));
    style.fontColor = new Color(colour);
    return style;
  }
}
