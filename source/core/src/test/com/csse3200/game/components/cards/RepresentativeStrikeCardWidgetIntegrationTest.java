package com.csse3200.game.components.cards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.play.CardPlayRequest;
import com.csse3200.game.cards.play.CardPlayResult;
import com.csse3200.game.cards.play.CardPlayService;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.cards.runtime.ResolvedCard;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ResourceService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Checkpoint E vertical slice for the representative Strike artwork and dynamic card face. */
@ExtendWith(GameExtension.class)
class RepresentativeStrikeCardWidgetIntegrationTest {
  private static final String STRIKE_ID = "strike";
  private static final String STRIKE_ART = "images/cards/strike.png";

  private Skin skin;
  private ResourceService resources;

  @BeforeEach
  void setUp() {
    skin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
    resources = new ResourceService();
    resources.loadTextures(new String[] {STRIKE_ART});
    resources.loadAll();
  }

  @AfterEach
  void tearDown() {
    resources.dispose();
    skin.dispose();
  }

  @Test
  void shouldCarryRealStrikeFromConfigThroughWidgetAndGameplay() {
    CardLibrary library = new CardLibrary(CardConfigLoader.loadCards());
    CardConfig strikeConfig = library.getCard(STRIKE_ID).orElseThrow();
    CardInstance base = new CardInstance("checkpoint-e-strike", STRIKE_ID, CardInstance.BASE_LEVEL);
    CardInstance upgraded =
        new CardInstance("checkpoint-e-strike-plus", STRIKE_ID, CardInstance.UPGRADED_LEVEL);
    BattleDeck deck = new BattleDeck(PlayerDeck.fromInstances(library, List.of(base, upgraded)));
    deck.drawCards(2);
    CardPlayService playService = new CardPlayService(library, deck, new EnergyComponent(2));

    ResolvedCard resolvedBase = playService.resolveInHand(base.instanceId()).orElseThrow();
    ResolvedCard resolvedUpgrade = playService.resolveInHand(upgraded.instanceId()).orElseThrow();
    CardWidget widget =
        new CardWidget(resolvedBase, CardWidgetAssets.fromManagedResources(skin, resources));

    TextureRegionDrawable baseArtwork = (TextureRegionDrawable) widget.displayedArtwork();
    Texture managedTexture = baseArtwork.getRegion().getTexture();
    assertEquals("Strike", widget.displayedName());
    assertEquals("1", widget.displayedCost());
    assertEquals("Attack  |  Common", widget.displayedMeta());
    assertEquals("Deal 6 damage.", widget.displayedDescription());
    assertEquals(CardWidget.CARD_WIDTH, widget.getPrefWidth());
    assertEquals(CardWidget.CARD_HEIGHT, widget.getPrefHeight());
    assertEquals(Scaling.fit, CardWidget.ARTWORK_SCALING);
    assertEquals(1024, managedTexture.getWidth());
    assertEquals(768, managedTexture.getHeight());
    assertEquals(Texture.TextureFilter.Nearest, managedTexture.getMinFilter());
    assertEquals(Texture.TextureFilter.Nearest, managedTexture.getMagFilter());

    widget.setCard(resolvedUpgrade);

    TextureRegionDrawable upgradedArtwork = (TextureRegionDrawable) widget.displayedArtwork();
    assertEquals("Strike+", widget.displayedName());
    assertEquals("1", widget.displayedCost());
    assertEquals("Deal 12 damage.", widget.displayedDescription());
    assertTrue(widget.displaysUpgradeMarker());
    assertSame(managedTexture, upgradedArtwork.getRegion().getTexture());
    assertEquals(strikeConfig.texturePath, resolvedUpgrade.texturePath());

    CardPlayResult result =
        playService.playCard(
            CardPlayRequest.singleEnemy(upgraded.instanceId(), "checkpoint-e-enemy"));

    assertTrue(result.success());
    assertEquals(upgraded.instanceId(), result.instanceId());
    assertEquals(1, result.energyCost());
    assertEquals(12, result.enemyEffects().getFirst().value());
    assertEquals(List.of(base), result.updatedHand());
    assertEquals(List.of(upgraded), result.updatedDiscardPile());
  }
}
