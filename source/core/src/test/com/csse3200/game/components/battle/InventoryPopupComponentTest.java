package com.csse3200.game.components.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rewards.ItemFormatting;
import com.csse3200.game.rewards.ItemType;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.PopupDisplay;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class InventoryPopupComponentTest {
  private Stage stage;
  private Entity inventoryEntity;

  @BeforeEach
  void setUp() {
    stage = new Stage(new FitViewport(1280f, 800f), mock(Batch.class));
    RenderService renderService = new RenderService();
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerEntityService(new EntityService());
    ResourceService resources = mock(ResourceService.class);
    Texture panelTexture = mock(Texture.class);
    when(panelTexture.getWidth()).thenReturn(960);
    when(panelTexture.getHeight()).thenReturn(660);
    when(resources.getAsset("images/ui/inventory-panel.png", Texture.class))
        .thenReturn(panelTexture);
    ServiceLocator.registerResourceService(resources);
  }

  @AfterEach
  void tearDown() {
    if (inventoryEntity != null) {
      inventoryEntity.dispose();
    }
    stage.dispose();
  }

  @Test
  void showsStackedEffectsWithoutMultiplyingPerUseOrLuckyCoinBonuses() {
    assertEquals(
        "+50% SHOP DISCOUNT",
        InventoryPopupComponent.describeItem(ItemType.MERCHANTS_FAVOR, 6).effect());
    assertEquals(
        "-6 HEALTH / TURN",
        InventoryPopupComponent.describeItem(ItemType.BLOOD_OATH_SIGIL, 3).effect());
    assertEquals(
        "+60 MAX HEALTH",
        InventoryPopupComponent.describeItem(ItemType.ETERNAL_VITALITY, 3).effect());
    assertEquals(
        "+10% GOLD (MAX +20)",
        InventoryPopupComponent.describeItem(ItemType.LUCKY_COIN, 3).effect());
    assertEquals(
        "+5 ARMOUR", InventoryPopupComponent.describeItem(ItemType.IRON_AEGIS, 3).effect());
  }

  @Test
  void curseRowShowsTypeCountAndStackedEffectWithoutUseAction() {
    RunState runState = new RunState();
    runState.getOrCreatePlayerState().addOwnedItem(ItemType.BLOOD_OATH_SIGIL);
    runState.getOrCreatePlayerState().addOwnedItem(ItemType.BLOOD_OATH_SIGIL);
    PopupDisplay popup = mock(PopupDisplay.class);
    Table content = new Table();
    when(popup.getContentTable()).thenReturn(content);
    InventoryPopupComponent inventory =
        new InventoryPopupComponent(runState, popup, null, () -> false);
    inventoryEntity = new Entity().addComponent(inventory);
    inventoryEntity.create();
    inventory.open();

    List<String> labels = labelsIn(content);
    assertTrue(labels.contains("2 Items | 1 Type"));
    assertTrue(labels.contains("x2"));
    assertTrue(labels.contains("-4 HEALTH / TURN"));
    assertTrue(labels.contains("CURSE"));
    assertTrue(labels.stream().noneMatch("USE"::equals));
  }

  @Test
  void inventoryRowsHaveFixedSizesAndListHeightMatchesVisibleRows() {
    RunState runState = new RunState();
    runState.getOrCreatePlayerState().addOwnedItem(ItemType.MERCHANTS_FAVOR);
    runState.getOrCreatePlayerState().addOwnedItem(ItemType.BLOOD_OATH_SIGIL);
    PopupDisplay popup = mock(PopupDisplay.class);
    Table content = new Table();
    when(popup.getContentTable()).thenReturn(content);

    InventoryPopupComponent inventory =
        new InventoryPopupComponent(runState, popup, null, () -> false);
    inventoryEntity = new Entity().addComponent(inventory);
    inventoryEntity.create();
    inventory.open();

    verify(popup).setMinSize(590f, 0f);
    ScrollPane scroll = assertInstanceOf(ScrollPane.class, content.getCells().get(3).getActor());
    assertEquals(530f, content.getCells().get(3).getPrefWidth(), 0.01f);
    assertEquals(200f, content.getCells().get(3).getPrefHeight(), 0.01f);
    Table rows = assertInstanceOf(Table.class, scroll.getWidget());
    assertEquals(518f, rows.getCells().get(0).getPrefWidth(), 0.01f);
    assertEquals(94f, rows.getCells().get(0).getPrefHeight(), 0.01f);
    Table frame = assertInstanceOf(Table.class, rows.getCells().first().getActor());
    Table row = assertInstanceOf(Table.class, frame.getCells().first().getActor());
    assertEquals(112f, row.getCells().get(2).getPrefWidth(), 0.01f);
  }

  @Test
  void popupStaysNarrowAndOnlyGrowsUntilThreeVisibleRows() {
    RunState runState = new RunState();
    runState.getOrCreatePlayerState().addOwnedItem(ItemType.MERCHANTS_FAVOR);
    PopupDisplay popup = new PopupDisplay("");
    InventoryPopupComponent inventory =
        new InventoryPopupComponent(runState, popup, null, () -> false);
    inventoryEntity = new Entity().addComponent(popup).addComponent(inventory);
    inventoryEntity.create();

    inventory.open();
    float width = popup.getWindowWidth();
    float height = popup.getWindowHeight();
    assertEquals(590f, width, 0.01f);
    assertTrue(height < 400f);

    for (ItemType item : ItemType.values()) {
      runState.getOrCreatePlayerState().addOwnedItem(item);
    }
    inventory.open();
    assertEquals(width, popup.getWindowWidth(), 0.01f);
    assertTrue(popup.getWindowHeight() > height);
    assertTrue(popup.getWindowHeight() < 600f);
  }

  @Test
  void inventoryWindowKeepsItsLogicalSizeAfterWindowResize() {
    RunState runState = new RunState();
    runState.getOrCreatePlayerState().addOwnedItem(ItemType.MERCHANTS_FAVOR);
    PopupDisplay popup = new PopupDisplay("");
    InventoryPopupComponent inventory =
        new InventoryPopupComponent(runState, popup, null, () -> false);
    inventoryEntity = new Entity().addComponent(popup).addComponent(inventory);
    inventoryEntity.create();
    inventory.open();
    float width = popup.getWindowWidth();
    float height = popup.getWindowHeight();

    stage.getViewport().update(1920, 1080, true);
    inventory.open();

    assertEquals(590f, popup.getWindowWidth(), 0.01f);
    assertEquals(width, popup.getWindowWidth(), 0.01f);
    assertEquals(height, popup.getWindowHeight(), 0.01f);
  }

  @Test
  void compactTextFitsItsFixedColumns() {
    GlyphLayout nameLayout = new GlyphLayout();
    GlyphLayout effectLayout = new GlyphLayout();
    GlyphLayout statusLayout = new GlyphLayout();
    for (ItemType item : ItemType.values()) {
      nameLayout.setText(
          UIComponent.getSharedSkin().getFont("font"), ItemFormatting.formatItemName(item));
      effectLayout.setText(
          UIComponent.getSharedSkin().getFont("font_small"),
          InventoryPopupComponent.describeItem(item, 3).effect());
      assertTrue(nameLayout.width <= 308f, item + " name is too wide: " + nameLayout.width);
      assertTrue(effectLayout.width <= 308f, item + " effect is too wide: " + effectLayout.width);
    }
    statusLayout.setText(UIComponent.getSharedSkin().getFont("font_small"), "PASSIVE");
    assertTrue(statusLayout.width <= 108f);
  }

  @Test
  void useButtonHasItsOwnDarkStyleInsteadOfTheSharedRoundedStyle() {
    RunState runState = new RunState();
    runState.getOrCreatePlayerState().addOwnedItem(ItemType.IRON_AEGIS);
    PopupDisplay popup = mock(PopupDisplay.class);
    Table content = new Table();
    when(popup.getContentTable()).thenReturn(content);
    InventoryPopupComponent inventory =
        new InventoryPopupComponent(runState, popup, null, () -> false);
    inventoryEntity = new Entity().addComponent(inventory);
    inventoryEntity.create();
    inventory.open();

    ScrollPane scroll = assertInstanceOf(ScrollPane.class, content.getCells().get(3).getActor());
    Table rows = assertInstanceOf(Table.class, scroll.getWidget());
    Table frame = assertInstanceOf(Table.class, rows.getCells().first().getActor());
    Table row = assertInstanceOf(Table.class, frame.getCells().first().getActor());
    Table actionColumn = assertInstanceOf(Table.class, row.getCells().get(2).getActor());
    Table control = assertInstanceOf(Table.class, actionColumn.getCells().get(1).getActor());
    TextButton button = assertInstanceOf(TextButton.class, control.getCells().first().getActor());
    assertEquals("USE", button.getText().toString());
    assertTrue(button.isDisabled());
    assertNotSame(button.getStyle().fontColor, button.getStyle().disabledFontColor);
    assertNotSame(
        UIComponent.getSharedSkin().get("default", TextButton.TextButtonStyle.class).up,
        button.getStyle().up);
  }

  @Test
  void usingLastAegisImmediatelyRemovesItsRowAndGrantsArmour() {
    RunState runState = new RunState();
    runState.getOrCreatePlayerState().addOwnedItem(ItemType.IRON_AEGIS);
    Entity player = new Entity().addComponent(new CombatStatsComponent(100, 5));
    PopupDisplay popup = mock(PopupDisplay.class);
    Table content = new Table();
    when(popup.getContentTable()).thenReturn(content);
    InventoryPopupComponent inventory =
        new InventoryPopupComponent(runState, popup, player, () -> true);
    inventoryEntity = new Entity().addComponent(inventory);
    inventoryEntity.create();
    inventory.open();

    ScrollPane scroll = assertInstanceOf(ScrollPane.class, content.getCells().get(3).getActor());
    Table rows = assertInstanceOf(Table.class, scroll.getWidget());
    Table frame = assertInstanceOf(Table.class, rows.getCells().first().getActor());
    Table row = assertInstanceOf(Table.class, frame.getCells().first().getActor());
    Table actionColumn = assertInstanceOf(Table.class, row.getCells().get(2).getActor());
    Table control = assertInstanceOf(Table.class, actionColumn.getCells().get(1).getActor());
    TextButton button = assertInstanceOf(TextButton.class, control.getCells().first().getActor());
    button.fire(new ChangeListener.ChangeEvent());

    assertEquals(5, player.getComponent(CombatStatsComponent.class).getArmour());
    assertEquals(0, runState.getOrCreatePlayerState().getOwnedItemCount(ItemType.IRON_AEGIS));
    assertInstanceOf(Label.class, content.getCells().get(3).getActor());
  }

  @Test
  void crestFeedbackShowsZeroWhenCurseCancelsTheStrengthGain() {
    RunState runState = new RunState();
    runState.getOrCreatePlayerState().addOwnedItem(ItemType.WARRIORS_CREST);
    CombatStatsComponent stats = new CombatStatsComponent(100, 5);
    stats.applyStatusEffect("STRENGTH_PENALTY", 1, 0);
    Entity player = new Entity().addComponent(stats);
    PopupDisplay popup = mock(PopupDisplay.class);
    Table content = new Table();
    when(popup.getContentTable()).thenReturn(content);
    InventoryPopupComponent inventory =
        new InventoryPopupComponent(runState, popup, player, () -> true);
    inventoryEntity = new Entity().addComponent(inventory);
    inventoryEntity.create();
    inventory.open();

    clickFirstUseButton(content);

    assertEquals(0, runState.getOrCreatePlayerState().getOwnedItemCount(ItemType.WARRIORS_CREST));
    assertNull(stats.getStatusEffect("STRENGTH"));
    assertEquals("+0 STRENGTH", feedbackDetail());
  }

  @Test
  void healingFeedbackShowsHealthActuallyRestored() {
    RunState runState = new RunState();
    runState.getOrCreatePlayerState().addOwnedItem(ItemType.AMBROSIA_FLASK);
    CombatStatsComponent stats = new CombatStatsComponent(95, 5, 100);
    Entity player = new Entity().addComponent(stats);
    PopupDisplay popup = mock(PopupDisplay.class);
    Table content = new Table();
    when(popup.getContentTable()).thenReturn(content);
    InventoryPopupComponent inventory =
        new InventoryPopupComponent(runState, popup, player, () -> true);
    inventoryEntity = new Entity().addComponent(inventory);
    inventoryEntity.create();
    inventory.open();

    clickFirstUseButton(content);

    assertEquals(100, stats.getHealth());
    assertEquals("+5 HEALTH", feedbackDetail());
  }

  private void clickFirstUseButton(Table content) {
    ScrollPane scroll = assertInstanceOf(ScrollPane.class, content.getCells().get(3).getActor());
    Table rows = assertInstanceOf(Table.class, scroll.getWidget());
    Table frame = assertInstanceOf(Table.class, rows.getCells().first().getActor());
    Table row = assertInstanceOf(Table.class, frame.getCells().first().getActor());
    Table actionColumn = assertInstanceOf(Table.class, row.getCells().get(2).getActor());
    Table control = assertInstanceOf(Table.class, actionColumn.getCells().get(1).getActor());
    TextButton button = assertInstanceOf(TextButton.class, control.getCells().first().getActor());
    button.fire(new ChangeListener.ChangeEvent());
  }

  private String feedbackDetail() {
    Table border = assertInstanceOf(Table.class, stage.getActors().peek());
    Table message = assertInstanceOf(Table.class, border.getCells().first().getActor());
    Table text = assertInstanceOf(Table.class, message.getCells().first().getActor());
    Label detail = assertInstanceOf(Label.class, text.getCells().get(1).getActor());
    return detail.getText().toString();
  }

  private List<String> labelsIn(Actor actor) {
    List<String> labels = new ArrayList<>();
    collectLabels(actor, labels);
    return labels;
  }

  private void collectLabels(Actor actor, List<String> labels) {
    if (actor instanceof Label label) {
      labels.add(label.getText().toString());
    }
    if (actor instanceof Group group) {
      for (Actor child : group.getChildren()) {
        collectLabels(child, labels);
      }
    }
  }
}
