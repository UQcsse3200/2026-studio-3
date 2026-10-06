package com.csse3200.game.components.spritedisplay.clickable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.utils.ObjectMap;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.CardUpgradeConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.play.CardPlayRequest;
import com.csse3200.game.cards.play.CardPlayService;
import com.csse3200.game.cards.play.CardPlayTarget;
import com.csse3200.game.cards.play.integration.Team3CardPlayAdapter;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.cards.CardEffectHandler;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.enemy.EnemyBehaviourComponent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.DragNDropService;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class DragNDropTest {
  private static final String STRIKE_INSTANCE_ID = "strike-instance-1";

  @Test
  void selfTargetCardStillPlaysByClick() {
    InOutOnTrigger card =
        new InOutOnTrigger(
            ClickableRecord.builder("playCard")
                .text("Defend")
                .args("defend-instance-1", "player")
                .build());
    Entity battleUi = new Entity().addComponent(card);
    AtomicReference<String> played = new AtomicReference<>();
    battleUi
        .getEvents()
        .addListener(
            "playCard",
            (String instanceId, String targetId) -> played.set(instanceId + ":" + targetId));

    card.getBtn().fire(new ChangeListener.ChangeEvent());

    assertEquals("defend-instance-1:player", played.get());
  }

  @Test
  void aimedDragPlaysSelectedEnemyEvenWithoutDropActor() throws Exception {
    DragNDropService dragService = new DragNDropService();
    ServiceLocator.registerDragNDropService(dragService);
    RecordingAim aim = new RecordingAim("enemy-11");
    DragNDrop card =
        new DragNDrop(
            ClickableRecord.builder("playCard").text("Strike").args(STRIKE_INSTANCE_ID).build(),
            aim);
    Entity battleUi = new Entity().addComponent(card);
    AtomicReference<String> playedTarget = new AtomicReference<>();
    battleUi
        .getEvents()
        .addListener(
            "playCard",
            (String instanceId, String targetId) -> playedTarget.set(instanceId + ":" + targetId));

    DragAndDrop.Source source = getOnlySource(dragService.getDragAndDrop());
    DragAndDrop.Payload payload = source.dragStart(new InputEvent(), 0f, 0f, 0);
    source.dragStop(new InputEvent(), 0f, 0f, 0, payload, null);

    assertEquals(STRIKE_INSTANCE_ID + ":enemy-11", playedTarget.get());
  }

  @Test
  void aimedDragPlaysSelectedPlayerWithoutDropActor() throws Exception {
    DragNDropService dragService = new DragNDropService();
    ServiceLocator.registerDragNDropService(dragService);
    DragNDrop card =
        new DragNDrop(
            ClickableRecord.builder("playCard").text("Defend").args("defend-instance-1").build(),
            new RecordingAim("player"));
    Entity battleUi = new Entity().addComponent(card);
    AtomicReference<String> playedTarget = new AtomicReference<>();
    battleUi
        .getEvents()
        .addListener(
            "playCard",
            (String instanceId, String targetId) -> playedTarget.set(instanceId + ":" + targetId));

    DragAndDrop.Source source = getOnlySource(dragService.getDragAndDrop());
    DragAndDrop.Payload payload = source.dragStart(new InputEvent(), 0f, 0f, 0);
    source.dragStop(new InputEvent(), 0f, 0f, 0, payload, null);

    assertEquals("defend-instance-1:player", playedTarget.get());
  }

  @Test
  void aimedDragWithoutSelectionDoesNotPlayCard() throws Exception {
    DragNDropService dragService = new DragNDropService();
    ServiceLocator.registerDragNDropService(dragService);
    RecordingAim aim = new RecordingAim(null);
    DragNDrop card =
        new DragNDrop(
            ClickableRecord.builder("playCard").text("Strike").args(STRIKE_INSTANCE_ID).build(),
            aim);
    Entity battleUi = new Entity().addComponent(card);
    AtomicReference<String> playedTarget = new AtomicReference<>();
    battleUi
        .getEvents()
        .addListener(
            "playCard", (String instanceId, String targetId) -> playedTarget.set(targetId));

    DragAndDrop.Source source = getOnlySource(dragService.getDragAndDrop());
    DragAndDrop.Payload payload = source.dragStart(new InputEvent(), 0f, 0f, 0);
    source.dragStop(new InputEvent(), 0f, 0f, 0, payload, null);

    assertNull(playedTarget.get());
  }

  @Test
  void shouldSendCardInstanceAndTargetIdsToSourceEntityAfterDrop() throws Exception {
    DragNDropService dragService = new DragNDropService();
    ServiceLocator.registerDragNDropService(dragService);

    DragNDrop card =
        new DragNDrop(
            ClickableRecord.builder("playCard")
                .text("Strike")
                .args(STRIKE_INSTANCE_ID)
                .variant("drag")
                .build());
    Entity battleUi = new Entity().addComponent(card);
    AtomicReference<CardPlayRequest> received = new AtomicReference<>();
    battleUi
        .getEvents()
        .addListener(
            "playCard",
            (String instanceId, String targetId) ->
                received.set(
                    new CardPlayRequest(
                        instanceId, new CardPlayTarget(TargetType.SINGLE_ENEMY, targetId))));

    DragAndDrop.Source source = getOnlySource(dragService.getDragAndDrop());
    DragAndDrop.Payload payload = source.dragStart(new InputEvent(), 0f, 0f, 0);
    Actor targetActor = new Actor();
    targetActor.setUserObject("bone_crawler");
    DragAndDrop.Target target = targetFor(targetActor);

    source.dragStop(new InputEvent(), 0f, 0f, 0, payload, target);

    assertEquals(
        new CardPlayRequest(
            STRIKE_INSTANCE_ID, new CardPlayTarget(TargetType.SINGLE_ENEMY, "bone_crawler")),
        received.get());
  }

  @Test
  void shouldPlayOnlyDraggedUpgradedDuplicateThroughBattleAdapter() throws Exception {
    DragNDropService dragService = new DragNDropService();
    ServiceLocator.registerDragNDropService(dragService);

    CardConfig strike = upgradedStrike();
    CardLibrary library = new CardLibrary(List.of(strike));
    CardInstance base = new CardInstance("strike-base", "strike", 0);
    CardInstance upgraded = new CardInstance("strike-plus", "strike", 1);
    BattleDeck deck = new BattleDeck(PlayerDeck.fromInstances(library, List.of(base, upgraded)));
    deck.drawCards(2);

    EnergyComponent energy = new EnergyComponent(3);
    Entity player = new Entity().addComponent(new CombatStatsComponent(10, 0)).addComponent(energy);
    Entity enemy =
        new Entity()
            .addComponent(new CombatStatsComponent(12, 1))
            .addComponent(new EnemyBehaviourComponent("test"));
    CardPlayService playService = new CardPlayService(library, deck, energy);
    BattleController controller =
        new BattleController(
            player, List.of(enemy), new CardEffectHandler(Map.of("enemy-1", enemy)), playService);

    DragNDrop draggedCard =
        new DragNDrop(
            ClickableRecord.builder(Team3CardPlayAdapter.PLAY_CARD_EVENT)
                .text("Strike+")
                .args(upgraded.instanceId())
                .variant("drag")
                .build());
    Entity battleUi =
        new Entity()
            .addComponent(draggedCard)
            .addComponent(new Team3CardPlayAdapter(playService, controller));
    battleUi.create();
    controller.start();

    DragAndDrop.Source source = getOnlySource(dragService.getDragAndDrop());
    DragAndDrop.Payload payload = source.dragStart(new InputEvent(), 0f, 0f, 0);
    Actor targetActor = new Actor();
    targetActor.setUserObject("enemy-1");
    source.dragStop(new InputEvent(), 0f, 0f, 0, payload, targetFor(targetActor));

    assertEquals(1, energy.getCurrentEnergy());
    assertEquals(3, enemy.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(List.of(base), deck.getHand());
    assertEquals(List.of(upgraded), deck.getDiscardPile());
  }

  private static CardConfig upgradedStrike() {
    CardConfig strike = new CardConfig();
    strike.id = "strike";
    strike.name = "Strike";
    strike.description = "Deal damage";
    strike.cost = 1;
    strike.type = CardType.ATTACK;
    strike.rarity = Rarity.COMMON;
    strike.target = TargetType.SINGLE_ENEMY;
    strike.texturePath = "images/cards/strike.png";
    strike.effects = new EffectConfig[] {new EffectConfig(EffectType.DAMAGE, 6)};

    CardUpgradeConfig upgrade = new CardUpgradeConfig();
    upgrade.name = "Strike+";
    upgrade.description = "Deal 9 damage";
    upgrade.cost = 2;
    upgrade.rarity = Rarity.COMMON;
    upgrade.effects = new EffectConfig[] {new EffectConfig(EffectType.DAMAGE, 9)};
    strike.upgrade = upgrade;
    return strike;
  }

  private DragAndDrop.Target targetFor(Actor actor) {
    return new DragAndDrop.Target(actor) {
      @Override
      public boolean drag(
          DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
        return true;
      }

      @Override
      public void drop(
          DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {}
    };
  }

  private record RecordingAim(String targetId) implements AimSession {
    @Override
    public void begin(Vector2 cardPosition, Vector2 pointer) {}

    @Override
    public void update(Vector2 pointer) {}

    @Override
    public String release(Vector2 pointer) {
      return targetId;
    }

    @Override
    public void cancel() {}
  }

  @SuppressWarnings("unchecked")
  private DragAndDrop.Source getOnlySource(DragAndDrop dragAndDrop) throws Exception {
    Field field = DragAndDrop.class.getDeclaredField("sourceListeners");
    field.setAccessible(true);
    ObjectMap<DragAndDrop.Source, DragListener> sources =
        (ObjectMap<DragAndDrop.Source, DragListener>) field.get(dragAndDrop);
    return sources.keys().next();
  }
}
