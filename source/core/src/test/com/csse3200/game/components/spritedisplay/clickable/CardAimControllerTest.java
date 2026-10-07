package com.csse3200.game.components.spritedisplay.clickable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.play.CardPlayService;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CardAimControllerTest {
  private final Rectangle battlefield = new Rectangle(0, 400, 1280, 220);
  private Graphics previousGraphics;
  private Stage stage;
  private OrthographicCamera camera;
  private CardAimController aim;

  @BeforeEach
  void setUp() {
    previousGraphics = Gdx.graphics;
    Gdx.graphics = mock(Graphics.class);
    when(Gdx.graphics.getWidth()).thenReturn(1280);
    when(Gdx.graphics.getHeight()).thenReturn(800);
    stage = new Stage(new FitViewport(1280, 800), mock(Batch.class));
    stage.getViewport().update(1280, 800, true);
    camera = new OrthographicCamera(1280, 800);
    camera.position.set(640, 400, 0);
    camera.update();
  }

  @AfterEach
  void tearDown() {
    if (aim != null) aim.dispose();
    stage.dispose();
    Gdx.graphics = previousGraphics;
  }

  @Test
  void selfCardReleasedInEmptyBattlefieldSelectsPlayer() {
    aim = controller(TargetType.SELF, Map.of("player", target(120, 350)));
    aim.begin(new Vector2(640, 180), new Vector2(600, 450));
    assertEquals("player", aim.release(new Vector2(600, 450)));
  }

  @Test
  void allEnemyCardReleasedInEmptyBattlefieldNeedsNoEnemySelection() {
    aim = controller(TargetType.ALL_ENEMIES, Map.of("enemy", target(900, 350)));
    aim.begin(new Vector2(640, 180), new Vector2(450, 450));
    assertEquals("allEnemies", aim.release(new Vector2(450, 450)));
  }

  @Test
  void battlefieldCardsCancelOverHandControlsAndOutsideStage() {
    for (TargetType type : new TargetType[] {TargetType.SELF, TargetType.ALL_ENEMIES}) {
      aim = controller(type, Map.of("player", target(120, 350), "enemy", target(900, 350)));
      for (Vector2 pointer :
          new Vector2[] {
            new Vector2(640, 180),
            new Vector2(640, 350),
            new Vector2(640, 750),
            new Vector2(-1, 450),
            new Vector2(1281, 450)
          }) {
        aim.begin(new Vector2(640, 180), pointer);
        assertNull(aim.release(pointer));
      }
      aim.dispose();
      aim = null;
    }
  }

  @Test
  void singleEnemyCardStillRequiresLivingEnemyNearPointer() {
    aim = controller(TargetType.SINGLE_ENEMY, Map.of("enemy", target(900, 350)));
    aim.begin(new Vector2(640, 180), new Vector2(450, 450));
    assertNull(aim.release(new Vector2(450, 450)));
    aim.begin(new Vector2(640, 180), new Vector2(920, 400));
    assertEquals("enemy", aim.release(new Vector2(920, 400)));
  }

  @Test
  void allEnemyCardCannotPlayWhenEveryEnemyIsDead() {
    Entity enemy = target(900, 350);
    enemy.getComponent(CombatStatsComponent.class).setHealth(0);
    aim = controller(TargetType.ALL_ENEMIES, Map.of("enemy", enemy));
    aim.begin(new Vector2(640, 180), new Vector2(450, 450));
    assertNull(aim.release(new Vector2(450, 450)));
  }

  @Test
  void cancelledSessionCannotPlayOnRelease() {
    aim = controller(TargetType.SELF, Map.of("player", target(120, 350)));
    aim.begin(new Vector2(640, 180), new Vector2(450, 450));
    aim.cancel();
    assertNull(aim.release(new Vector2(450, 450)));
  }

  @Test
  void emptyBattlefieldDropAppliesSelfEffectAndSpendsEnergyOnce() throws Exception {
    DropBattle battle = dropBattle(TargetType.SELF, 3);
    drop(battle.source(), -190, 270);
    assertEquals(4, battle.player().getComponent(CombatStatsComponent.class).getBlock());
    assertEquals(2, battle.energy().getCurrentEnergy());
    assertEquals(0, battle.deck().getHand().size());
    assertEquals(1, battle.deck().getDiscardPile().size());
    assertEquals(10, battle.enemies().get(0).getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void emptyBattlefieldDropDamagesEveryLivingEnemyAndSpendsEnergyOnce() throws Exception {
    DropBattle battle = dropBattle(TargetType.ALL_ENEMIES, 3);
    drop(battle.source(), -190, 270);
    assertEquals(6, battle.enemies().get(0).getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(6, battle.enemies().get(1).getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(2, battle.energy().getCurrentEnergy());
    assertEquals(0, battle.deck().getHand().size());
    assertEquals(1, battle.deck().getDiscardPile().size());
  }

  @Test
  void dropBackIntoHandDoesNotSpendEnergyOrApplyEffects() throws Exception {
    DropBattle battle = dropBattle(TargetType.ALL_ENEMIES, 3);
    drop(battle.source(), 0, 0);
    assertEquals(3, battle.energy().getCurrentEnergy());
    assertEquals(1, battle.deck().getHand().size());
    assertEquals(10, battle.enemies().get(0).getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(10, battle.enemies().get(1).getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void unaffordableBattlefieldDropKeepsCardAndDoesNotApplyEffects() throws Exception {
    DropBattle battle = dropBattle(TargetType.SELF, 0);
    drop(battle.source(), -190, 270);
    assertEquals(0, battle.energy().getCurrentEnergy());
    assertEquals(1, battle.deck().getHand().size());
    assertEquals(0, battle.player().getComponent(CombatStatsComponent.class).getBlock());
  }

  @SuppressWarnings("unchecked")
  private DropBattle dropBattle(TargetType type, int availableEnergy) throws Exception {
    DragNDropService dragService = new DragNDropService();
    ServiceLocator.registerDragNDropService(dragService);
    CardConfig config = new CardConfig();
    config.id = "test-card";
    config.name = "Test card";
    config.description = "Apply effect";
    config.cost = 1;
    config.type = type == TargetType.SELF ? CardType.SKILL : CardType.ATTACK;
    config.rarity = Rarity.COMMON;
    config.target = type;
    config.texturePath = "images/cards/strike.png";
    config.effects =
        new EffectConfig[] {
          new EffectConfig(type == TargetType.SELF ? EffectType.BLOCK : EffectType.DAMAGE, 4)
        };
    CardLibrary library = new CardLibrary(List.of(config));
    BattleDeck deck =
        new BattleDeck(
            PlayerDeck.fromInstances(
                library, List.of(new CardInstance("test-instance", config.id, 0))));
    deck.drawCards(1);
    EnergyComponent energy = new EnergyComponent(3);
    Entity player = target(120, 350).addComponent(energy);
    Entity first = target(900, 350).addComponent(new EnemyBehaviourComponent("test"));
    Entity second = target(1050, 350).addComponent(new EnemyBehaviourComponent("test"));
    Map<String, Entity> enemies = Map.of("enemy-1", first, "enemy-2", second);
    aim = controller(type, type == TargetType.SELF ? Map.of("player", player) : enemies);
    CardPlayService service = new CardPlayService(library, deck, energy);
    BattleController battle =
        new BattleController(
            player, List.of(first, second), new CardEffectHandler(enemies), service);
    DragNDrop card =
        new DragNDrop(
            ClickableRecord.builder("playCard").text("Test card").args("test-instance").build(),
            aim);
    card.getBtn().setPosition(640, 180);
    Entity ui =
        new Entity().addComponent(card).addComponent(new Team3CardPlayAdapter(service, battle));
    ui.create();
    battle.start();
    energy.spendEnergy(3 - availableEnergy);
    Field field = DragAndDrop.class.getDeclaredField("sourceListeners");
    field.setAccessible(true);
    ObjectMap<DragAndDrop.Source, DragListener> sources =
        (ObjectMap<DragAndDrop.Source, DragListener>) field.get(dragService.getDragAndDrop());
    return new DropBattle(player, List.of(first, second), energy, deck, sources.keys().next());
  }

  private void drop(DragAndDrop.Source source, float x, float y) {
    DragAndDrop.Payload payload = source.dragStart(new InputEvent(), 0, 0, 0);
    source.drag(new InputEvent(), x, y, 0);
    source.dragStop(new InputEvent(), x, y, 0, payload, null);
  }

  private record DropBattle(
      Entity player,
      List<Entity> enemies,
      EnergyComponent energy,
      BattleDeck deck,
      DragAndDrop.Source source) {}

  private CardAimController controller(TargetType type, Map<String, Entity> targets) {
    return new CardAimController(stage, camera, targets, type, () -> battlefield);
  }

  private Entity target(float x, float y) {
    Entity entity = new Entity().addComponent(new CombatStatsComponent(10, 0));
    entity.setPosition(x, y);
    entity.setScale(80, 100);
    return entity;
  }
}
