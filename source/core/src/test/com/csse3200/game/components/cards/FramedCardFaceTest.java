package com.csse3200.game.components.cards;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.runtime.CardResolver;
import com.csse3200.game.cards.runtime.ResolvedCard;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FramedCardFaceTest {
  private Skin skin;
  private CardWidgetAssets assets;
  private CardLibrary library;

  @BeforeEach
  void setUp() {
    skin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
    Drawable art = skin.newDrawable("white", Color.NAVY);
    assets = CardWidgetAssets.fromSkin(skin, path -> art);
    library = new CardLibrary(CardConfigLoader.loadCards());
  }

  @AfterEach
  void tearDown() {
    skin.dispose();
  }

  @Test
  void sameRendererAcceptsFutureRareFrameLayoutWithoutRaritySpecificCode() {
    ResolvedCard rare =
        new CardResolver().resolveBasePreview(library.getCard("poison_mark").orElseThrow(), "rare");
    Drawable futureFrame = skin.newDrawable("white", Color.GOLD);
    CardFrameLayout common = CardFrameLayout.COMMON;
    CardFrameLayout futureLayout =
        new CardFrameLayout(
            "images/cards/future_rare_frame.png",
            common.artwork(),
            common.cost(),
            common.name(),
            common.description(),
            common.type(),
            common.target(),
            common.upgrade(),
            common.costScale(),
            common.nameScale(),
            common.descriptionScale(),
            common.typeScale(),
            common.targetScale(),
            common.artworkAboveFrame());
    FramedCardFace face = new FramedCardFace(assets, futureFrame, futureLayout);
    face.setCard(rare);
    face.setSize(90f, 130f);
    face.validate();

    assertEquals(Rarity.RARE, face.getCard().rarity());
    assertSame(futureFrame, face.displayedFrame());
    assertSame(futureLayout, face.frameLayout());
    assertEquals(rare.name(), face.displayedName());
    assertEquals(rare.description(), face.displayedDescription());
    assertEquals("Skill", face.displayedType());
    assertEquals("One Enemy", face.displayedTarget());
    assertEquals(Touchable.disabled, face.getTouchable());
    assertTrue(face.getListeners().isEmpty());
  }

  @Test
  void keywordGlyphsAreYellowWithoutChangingSharedSkinFontOrRawDescription() {
    ResolvedCard card =
        new CardResolver().resolveBasePreview(library.getCard("defend").orElseThrow(), "highlight");
    boolean originalMarkup = skin.get("small", Label.LabelStyle.class).font.getData().markupEnabled;
    FramedCardFace face =
        new FramedCardFace(assets, assets.authoredFrameFor(Rarity.COMMON), CardFrameLayout.COMMON);
    face.setCard(card);
    face.setSize(225f, 456f);
    face.validate();

    Label description = null;
    for (Actor actor : face.getChildren()) {
      if (actor instanceof Label label && label.getText().toString().contains("[YELLOW]")) {
        description = label;
      }
    }
    assertNotNull(description);
    assertEquals(
        CardWidgetAssets.formatDescription("Gain 5 block."), description.getText().toString());
    assertEquals("Gain 5 block.", face.displayedDescription());
    assertEquals("Gain 5 block.", card.description());
    assertEquals(
        originalMarkup, skin.get("small", Label.LabelStyle.class).font.getData().markupEnabled);
    assertNotSame(
        skin.get("small", Label.LabelStyle.class).font.getData(),
        description.getStyle().font.getData());
    Batch batch = mock(Batch.class);
    List<Float> colours = new ArrayList<>();
    doAnswer(
            invocation -> {
              float[] vertices = invocation.getArgument(1);
              int offset = invocation.getArgument(2);
              int count = invocation.getArgument(3);
              for (int index = offset + 2; index < offset + count; index += 20) {
                colours.add(vertices[index]);
              }
              return null;
            })
        .when(batch)
        .draw(any(Texture.class), any(float[].class), anyInt(), anyInt());
    description.draw(batch, 1f);
    assertTrue(
        colours.contains(Color.YELLOW.toFloatBits()), "keyword glyphs must emit yellow vertices");
    assertTrue(
        colours.contains(new Color(0.10f, 0.065f, 0.045f, 1f).toFloatBits()),
        "surrounding text must retain its normal colour");
  }

  @Test
  void lastDescriptionLineEmitsExactlySameGlyphSizeAsEarlierLinesAtEveryCardScale() {
    ResolvedCard base =
        new CardResolver().resolveBasePreview(library.getCard("strike").orElseThrow(), "text");
    String paragraph = "MMMM MMMM\nMMMM MMMM\nMMMM";
    ResolvedCard card =
        new ResolvedCard(
            base.instanceId(),
            base.cardId(),
            base.name(),
            paragraph,
            base.cost(),
            base.type(),
            base.rarity(),
            base.target(),
            base.effects(),
            base.texturePath(),
            false);
    for (float[] size : new float[][] {{225f, 456f}, {90f, 130f}, {150f, 214f}}) {
      FramedCardFace face =
          new FramedCardFace(
              assets, assets.authoredFrameFor(Rarity.COMMON), CardFrameLayout.COMMON);
      face.setCard(card);
      face.setSize(size[0], size[1]);
      face.validate();
      Label description = null;
      for (Actor child : face.getChildren()) {
        if (child instanceof Label label
            && CardWidgetAssets.formatDescription(paragraph).contentEquals(label.getText())) {
          description = label;
        }
      }
      assertNotNull(description);
      assertEquals(CardWidgetAssets.formatDescription(paragraph), description.getText().toString());
      assertTrue(description.getGlyphLayout().runs.size >= 3);
      Batch batch = mock(Batch.class);
      List<Float> heights = new ArrayList<>();
      float glyphU = description.getStyle().font.getData().getGlyph('M').u;
      float glyphV = description.getStyle().font.getData().getGlyph('M').v;
      doAnswer(
              invocation -> {
                float[] vertices = invocation.getArgument(1);
                int offset = invocation.getArgument(2);
                int count = invocation.getArgument(3);
                for (int i = offset; i < offset + count; i += 20) {
                  if (Math.abs(vertices[i + 3] - glyphU) < 0.0001f
                      && Math.abs(vertices[i + 4] - glyphV) < 0.0001f) {
                    heights.add(vertices[i + 6] - vertices[i + 1]);
                  }
                }
                return null;
              })
          .when(batch)
          .draw(any(Texture.class), any(float[].class), anyInt(), anyInt());
      description.draw(batch, 1f);
      assertEquals(20, heights.size());
      for (float height : heights) {
        assertEquals(heights.getFirst(), height, 0.0001f);
      }
      assertEquals(description.getFontScaleX(), description.getFontScaleY());
      assertTrue(description.getPrefHeight() <= description.getHeight() + 0.01f);
    }
  }
}
