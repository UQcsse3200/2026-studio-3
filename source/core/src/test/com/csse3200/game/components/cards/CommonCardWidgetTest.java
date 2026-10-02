package com.csse3200.game.components.cards;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.cards.runtime.CardResolver;
import com.csse3200.game.cards.runtime.ResolvedCard;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ResourceService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CommonCardWidgetTest {
  private Skin skin;
  private ResourceService resources;
  private CardWidgetAssets assets;
  private List<CardConfig> configs;
  private final CardResolver resolver = new CardResolver();

  @BeforeEach
  void setUp() {
    skin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
    configs = CardConfigLoader.loadCards();
    resources = new ResourceService();
    resources.loadTextures(CardWidgetAssets.collectTexturePaths(configs));
    resources.loadAll();
    assets = CardWidgetAssets.fromManagedResources(skin, resources);
  }

  @AfterEach
  void tearDown() {
    skin.dispose();
    resources.dispose();
  }

  @Test
  void everyCommonBaseAndUpgradeUsesManagedFrameWithCompleteDynamicText() {
    int checked = 0;
    for (CardConfig config : configs) {
      for (int level = 0; level <= (config.upgrade == null ? 0 : 1); level++) {
        ResolvedCard resolved =
            resolver.resolve(config, new CardInstance("preview", config.id, level));
        if (resolved.rarity() != Rarity.COMMON) {
          continue;
        }
        CardWidget widget = new CardWidget(resolved, assets);
        widget.validate();
        assertCommonFrame(widget);
        assertEquals(resolved.name(), widget.displayedName());
        assertEquals(Integer.toString(resolved.cost()), widget.displayedCost());
        assertEquals(resolved.description(), widget.displayedDescription());
        assertEquals(FramedCardFace.formatType(resolved.type()), widget.displayedType());
        assertEquals(FramedCardFace.formatTarget(resolved.target()), widget.displayedTarget());
        assertEquals(resolved.upgraded(), widget.displaysUpgradeMarker());
        assertEquals(Touchable.disabled, widget.getTouchable());
        assertTrue(widget.getListeners().isEmpty());
        assertBounds(widget);
        Label description = findLabel(widget, resolved.description());
        assertNotNull(description);
        assertTrue(description.getPrefHeight() <= description.getHeight() + 1f, config.id);
        assertTrue(description.getGlyphLayout().width <= description.getWidth() + 1f, config.id);
        checked++;
      }
    }
    assertTrue(checked > 1);
  }

  @Test
  void resizingKeepsFrameAspectRatioAndAllSlotsInsideMiniatureCard() {
    ResolvedCard strike = resolver.resolveBasePreview(config("strike"), "strike-preview");
    for (float[] size : new float[][] {{90f, 130f}, {150f, 214f}, {272f, 355f}, {225f, 456f}}) {
      CardWidget widget = new CardWidget(strike, assets);
      widget.setSize(size[0], size[1]);
      widget.validate();
      assertBounds(widget);
      Group face = (Group) widget.getChildren().first();
      Image frame = face.findActor("card-frame");
      assertEquals(
          CardWidget.CARD_WIDTH / CardWidget.CARD_HEIGHT,
          frame.getWidth() / frame.getHeight(),
          0.0001f);
      Label cost = findLabel(widget, "1");
      Label name = findLabel(widget, "Strike");
      assertTrue(cost.getX() + cost.getWidth() <= name.getX());
    }
  }

  @Test
  void rebindSwitchesFrameByResolvedRarityWithoutLeavingOldFacesBehind() {
    ResolvedCard common = resolver.resolveBasePreview(config("strike"), "common-preview");
    ResolvedCard rare = resolver.resolveBasePreview(config("poison_mark"), "rare-preview");
    CardWidget widget = new CardWidget(common, assets);
    assertCommonFrame(widget);

    widget.setCard(rare);
    widget.validate();
    assertSame(assets.frameFor(Rarity.RARE), widget.displayedFrame());
    assertEquals(1, widget.getChildren().size);
    assertEquals(rare.description(), widget.displayedDescription());

    widget.setCard(common);
    widget.validate();
    assertCommonFrame(widget);
    assertEquals(1, widget.getChildren().size);
    assertEquals(common.description(), widget.displayedDescription());
  }

  @Test
  void textureCollectionIncludesSharedFrameAndDeduplicatesArtworkAndSkipsBlankPaths() {
    CardConfig repeated = config("strike");
    CardConfig blank = new CardConfig();
    blank.texturePath = " ";
    CardConfig missing = new CardConfig();
    assertEquals(
        List.of(CardWidgetAssets.COMMON_FRAME_TEXTURE, repeated.texturePath),
        Arrays.asList(
            CardWidgetAssets.collectTexturePaths(List.of(repeated, repeated, blank, missing))));
    assertTrue(
        Arrays.asList(CardWidgetAssets.collectTexturePaths(List.of()))
            .contains(CardWidgetAssets.COMMON_FRAME_TEXTURE));
  }

  @Test
  void centersTypeInMiddleAndSingleTargetAtBottomWithoutRarityText() {
    ResolvedCard strike = resolver.resolveBasePreview(config("strike"), "strike-preview");
    for (TargetType target : TargetType.values()) {
      ResolvedCard card =
          new ResolvedCard(
              strike.instanceId(),
              strike.cardId(),
              "Resurrection+",
              strike.description(),
              12,
              strike.type(),
              strike.rarity(),
              target,
              strike.effects(),
              strike.texturePath(),
              true);
      CardWidget widget = new CardWidget(card, assets);
      widget.validate();
      Label cost = findLabel(widget, "12");
      Label name = findLabel(widget, "Resurrection+");
      Label targetLabel = findLabel(widget, FramedCardFace.formatTarget(target));
      Label typeLabel = findLabel(widget, "Attack");
      assertCenteredSlot(cost, 34.5f, 423.5f);
      assertCenteredSlot(name, 131.5f, 428f);
      assertCenteredSlot(typeLabel, 112.5f, 158.5f);
      assertCenteredSlot(targetLabel, 112.5f, 23f);
      assertTrue(cost.getX() + cost.getWidth() <= name.getX());
      assertNull(findLabel(widget, "Attack  |  Common"));
      assertEquals(1, countLabels(widget, FramedCardFace.formatTarget(target)));
    }
  }

  @Test
  void refreshesEveryCardTypeAndTargetInSeparateSlotsAtAllConsumerSizes() {
    ResolvedCard strike = resolver.resolveBasePreview(config("strike"), "type-preview");
    CardWidget widget = new CardWidget(strike, assets);
    for (CardType type : CardType.values()) {
      for (TargetType target : TargetType.values()) {
        ResolvedCard card =
            new ResolvedCard(
                strike.instanceId(),
                strike.cardId(),
                strike.name(),
                strike.description(),
                strike.cost(),
                type,
                strike.rarity(),
                target,
                strike.effects(),
                strike.texturePath(),
                false);
        widget.setCard(card);
        for (float[] size : new float[][] {{225f, 456f}, {90f, 130f}, {272f, 355f}}) {
          widget.setSize(size[0], size[1]);
          widget.validate();
          Label typeLabel = widget.findActor("card-type");
          Label targetLabel = widget.findActor("card-target");
          Label description = findLabel(widget, card.description());
          typeLabel.validate();
          targetLabel.validate();
          assertEquals(FramedCardFace.formatType(type), widget.displayedType());
          assertEquals(FramedCardFace.formatTarget(target), widget.displayedTarget());
          assertEquals(Align.center, typeLabel.getLabelAlign());
          assertEquals(Align.center, targetLabel.getLabelAlign());
          assertTrue(typeLabel.getY() >= description.getY() + description.getHeight());
          assertTrue(targetLabel.getY() + targetLabel.getHeight() <= description.getY());
          assertTrue(typeLabel.getGlyphLayout().width <= typeLabel.getWidth() + 0.01f);
          assertTrue(targetLabel.getGlyphLayout().width <= targetLabel.getWidth() + 0.01f);
          assertTrue(targetLabel.getPrefHeight() <= targetLabel.getHeight() + 0.01f);
          assertEquals(1, countLabels(widget, FramedCardFace.formatTarget(target)));
          assertEquals(1, countLabels(widget, FramedCardFace.formatType(type)));
          assertNull(findLabel(widget, "Common"));
          assertBounds(widget);
        }
      }
    }
  }

  @Test
  void restoresWholeDescriptionScaleAfterLongTextAndMiniatureRebind() {
    ResolvedCard strike = resolver.resolveBasePreview(config("strike"), "strike-preview");
    String longText = "A long rule with several effects and durations. ".repeat(8);
    ResolvedCard longCard =
        new ResolvedCard(
            strike.instanceId(),
            strike.cardId(),
            strike.name(),
            longText,
            strike.cost(),
            strike.type(),
            strike.rarity(),
            strike.target(),
            strike.effects(),
            strike.texturePath(),
            false);
    float originalFontScale = assets.descriptionStyle().font.getScaleY();
    CardWidget widget = new CardWidget(longCard, assets);
    widget.setSize(90f, 130f);
    widget.validate();
    Label description = findLabel(widget, longText);
    assertTrue(description.getFontScaleY() < 0.90f * 130f / CardWidget.CARD_HEIGHT);
    assertTrue(description.getPrefHeight() <= description.getHeight() + 1f);

    widget.setCard(strike);
    widget.setSize(CardWidget.CARD_WIDTH, CardWidget.CARD_HEIGHT);
    widget.validate();
    assertSame(description, findLabel(widget, strike.description()));
    assertEquals(0.90f, description.getFontScaleY(), 0.0001f);
    assertEquals(description.getFontScaleX(), description.getFontScaleY());
    assertEquals(originalFontScale, assets.descriptionStyle().font.getScaleY(), 0.0001f);
  }

  @Test
  void opaqueCommonWindowCannotCoverTheManagedArtworkAtAnyConsumerSize() {
    Pixmap png = new Pixmap(Gdx.files.internal(CardWidgetAssets.COMMON_FRAME_TEXTURE));
    try {
      assertEquals(0x000000ff, png.getPixel(225, 300), "the frame window is opaque black");
    } finally {
      png.dispose();
    }
    Texture frameTexture = resources.getAsset(CardWidgetAssets.COMMON_FRAME_TEXTURE, Texture.class);
    for (CardConfig config : configs) {
      ResolvedCard card = resolver.resolveBasePreview(config, "artwork-" + config.id);
      if (card.rarity() != Rarity.COMMON) {
        continue;
      }
      Texture artTexture = resources.getAsset(config.texturePath, Texture.class);
      for (float[] size : new float[][] {{225f, 456f}, {90f, 130f}, {272f, 355f}}) {
        CardWidget widget = new CardWidget(card, assets);
        widget.setSize(size[0], size[1]);
        widget.validate();
        Group face = (Group) widget.getChildren().first();
        Image frame = face.findActor("card-frame");
        Image art = face.findActor("card-artwork");
        assertNotNull(art.getDrawable(), config.id);
        assertTrue(art.isVisible(), config.id);
        assertTrue(frame.getZIndex() < art.getZIndex(), config.id);

        List<Texture> drawn = new ArrayList<>();
        Batch batch = mock(Batch.class);
        when(batch.getColor()).thenReturn(new Color(Color.WHITE));
        when(batch.getTransformMatrix()).thenReturn(new Matrix4());
        doAnswer(
                invocation -> {
                  TextureRegion region = invocation.getArgument(0);
                  drawn.add(region.getTexture());
                  return null;
                })
            .when(batch)
            .draw(any(TextureRegion.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());
        widget.draw(batch, 1f);
        assertTrue(drawn.contains(frameTexture), config.id);
        assertTrue(drawn.indexOf(artTexture) > drawn.indexOf(frameTexture), config.id);
      }
    }
  }

  @Test
  void overlaidArtworkBoundsStayInsideBlackWindowWithoutCoveringOrnaments() {
    Pixmap png = new Pixmap(Gdx.files.internal(CardWidgetAssets.COMMON_FRAME_TEXTURE));
    try {
      CardFrameLayout.Bounds art = CardFrameLayout.COMMON.artwork();
      int left = Math.round(art.x() * 2f);
      int right = Math.round((art.x() + art.width()) * 2f) - 1;
      int top = Math.round((CardWidget.CARD_HEIGHT - art.y() - art.height()) * 2f);
      int bottom = Math.round((CardWidget.CARD_HEIGHT - art.y()) * 2f) - 1;
      for (int x = left; x <= right; x++) {
        assertDarkWindowPixel(png, x, top);
        assertDarkWindowPixel(png, x, bottom);
      }
      for (int y = top; y <= bottom; y++) {
        assertDarkWindowPixel(png, left, y);
        assertDarkWindowPixel(png, right, y);
      }
    } finally {
      png.dispose();
    }
  }

  private static void assertDarkWindowPixel(Pixmap png, int x, int y) {
    int pixel = png.getPixel(x, y);
    assertTrue(((pixel >>> 24) & 255) < 35, "artwork covers red frame detail at " + x + "," + y);
    assertTrue(((pixel >>> 16) & 255) < 35, "artwork covers green frame detail at " + x + "," + y);
    assertTrue(((pixel >>> 8) & 255) < 35, "artwork covers blue frame detail at " + x + "," + y);
  }

  private static void assertCenteredSlot(Label label, float x, float y) {
    assertNotNull(label);
    assertEquals(x, label.getX() + label.getWidth() / 2f, 0.0001f);
    assertEquals(y, label.getY() + label.getHeight() / 2f, 0.0001f);
    assertEquals(Align.center, label.getLabelAlign());
    assertEquals(Align.center, label.getLineAlign());
    assertTrue(label.getGlyphLayout().width <= label.getWidth() + 0.01f);
    assertTrue(label.getPrefHeight() <= label.getHeight() + 0.01f);
  }

  private static int countLabels(Group group, String text) {
    int count = 0;
    for (Actor actor : group.getChildren()) {
      if (actor instanceof Label label && text.contentEquals(label.getText())) {
        count++;
      } else if (actor instanceof Group child) {
        count += countLabels(child, text);
      }
    }
    return count;
  }

  private CardConfig config(String id) {
    return configs.stream().filter(config -> id.equals(config.id)).findFirst().orElseThrow();
  }

  private void assertCommonFrame(CardWidget widget) {
    TextureRegionDrawable drawable =
        assertInstanceOf(TextureRegionDrawable.class, widget.displayedFrame());
    Texture texture = resources.getAsset(CardWidgetAssets.COMMON_FRAME_TEXTURE, Texture.class);
    assertSame(texture, drawable.getRegion().getTexture());
    assertEquals(450, texture.getWidth());
    assertEquals(912, texture.getHeight());
    assertEquals(Texture.TextureFilter.Nearest, texture.getMinFilter());
  }

  private static void assertBounds(Group group) {
    for (Actor actor : group.getChildren()) {
      assertTrue(actor.getX() >= -1f);
      assertTrue(actor.getY() >= -1f);
      assertTrue(actor.getX() + actor.getWidth() <= group.getWidth() + 1f);
      assertTrue(actor.getY() + actor.getHeight() <= group.getHeight() + 1f);
      assertEquals(Touchable.disabled, actor.getTouchable());
      if (actor instanceof Group child) {
        assertBounds(child);
      }
    }
  }

  private static Label findLabel(Group group, String text) {
    for (Actor actor : group.getChildren()) {
      if (actor instanceof Label label && text.contentEquals(label.getText())) {
        label.validate();
        return label;
      }
      if (actor instanceof Group child) {
        Label found = findLabel(child, text);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }
}
