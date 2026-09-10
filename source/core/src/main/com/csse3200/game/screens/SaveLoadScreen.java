package com.csse3200.game.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.GdxGame;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.save.SaveLoadPanel;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.maps.NodeState;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.save.DeckSaveData;
import com.csse3200.game.save.JsonSaveGameRepository;
import com.csse3200.game.save.MapNodeSaveData;
import com.csse3200.game.save.MapSaveData;
import com.csse3200.game.save.PlayerSaveData;
import com.csse3200.game.save.ProgressSaveData;
import com.csse3200.game.save.SaveGameData;
import com.csse3200.game.save.SaveGameRestoreService;
import com.csse3200.game.save.SaveGameService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Demo save/load screen reachable from the main menu while final menu placement is pending. */
public class SaveLoadScreen extends ScreenAdapter {
  private static final Logger logger = LoggerFactory.getLogger(SaveLoadScreen.class);
  private static final List<Integer> DEMO_SLOT_IDS = List.of(1, 2, 3);
  private static final String STRIKE = "strike";
  private static final String DEFEND = "defend";
  private static final String BANDAGE = "bandage";

  private final GdxGame game;
  private final Renderer renderer;

  public SaveLoadScreen(GdxGame game) {
    this.game = game;

    logger.debug("Initialising save/load screen services");
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerResourceService(new ResourceService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());
    ServiceLocator.registerTimeSource(new GameTime());

    renderer = RenderFactory.createRenderer();
    createUI();
  }

  private void createUI() {
    Stage stage = ServiceLocator.getRenderService().getStage();

    Entity demoPlayer =
        new Entity()
            .addComponent(new CombatStatsComponent(75, 5, 100))
            .addComponent(new InventoryComponent(120));
    PlayerDeck demoDeck = new PlayerDeck(List.of(STRIKE, DEFEND));

    SaveGameService saveGameService =
        new SaveGameService(new JsonSaveGameRepository(), this::createDemoSaveData);
    SaveGameRestoreService restoreService =
        new SaveGameRestoreService(demoPlayer, demoDeck, game.getRunState());

    Entity ui = new Entity();
    ui.addComponent(new InputDecorator(stage, 10))
        .addComponent(
            new SaveLoadPanel(
                saveGameService,
                DEMO_SLOT_IDS,
                restoreService,
                () -> game.setScreen(GdxGame.ScreenType.MAIN_MENU)));
    ServiceLocator.getEntityService().register(ui);
  }

  private SaveGameData createDemoSaveData() {
    SaveGameData data = new SaveGameData();
    data.player = new PlayerSaveData(75, 100, 120, 0);
    data.deck = new DeckSaveData(List.of(STRIKE, DEFEND, BANDAGE));
    data.map =
        new MapSaveData(
            List.of(
                new MapNodeSaveData(
                    0, RoomType.COMBAT.name(), NodeState.COMPLETED.name(), List.of(1)),
                new MapNodeSaveData(
                    1, RoomType.SHOP.name(), NodeState.CURRENT.name(), List.of(0, 2)),
                new MapNodeSaveData(
                    2, RoomType.EVENT.name(), NodeState.AVAILABLE.name(), List.of(1))),
            1,
            null);
    data.progress = new ProgressSaveData(List.of(), "", "MAP");
    data.metadata.runLabel = "Sprint 2 Demo Run";
    return data;
  }

  @Override
  public void render(float delta) {
    ServiceLocator.getEntityService().update();
    renderer.render();
  }

  @Override
  public void resize(int width, int height) {
    renderer.resize(width, height);
  }

  @Override
  public void dispose() {
    logger.debug("Disposing save/load screen");
    renderer.dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.clear();
  }
}
