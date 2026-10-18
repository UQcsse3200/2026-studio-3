package com.csse3200.game.maps;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.csse3200.game.extensions.GameExtension;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class MapIntroductionTest {
  private Skin skin;
  private Texture panelTexture;
  private Stage stage;
  private RunState runState;
  private MapIntroduction introduction;

  @BeforeEach
  void setUp() {
    skin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
    stage = new Stage(new FitViewport(1280f, 800f), mock(Batch.class));
    panelTexture = new Texture(Gdx.files.internal("images/ui/inventory-panel.png"));
    MapGraph graph =
        new MapGraph(
            Map.of(0, new MapNode(0, RoomType.START), 7, new MapNode(7, RoomType.COMBAT)), false);
    graph.connectNodes(0, 7);
    runState = new RunState();
    runState.startRun(graph, 0);
    introduction = new MapIntroduction(skin, runState, panelTexture);
    introduction.resize(stage.getWidth(), stage.getHeight());
    stage.addActor(introduction);
  }

  @AfterEach
  void tearDown() {
    stage.dispose();
    panelTexture.dispose();
    skin.dispose();
  }

  @Test
  void gotItCollapsesThePanelAndHelpReopensIt() {
    Actor panel = introduction.findActor("map-introduction-panel");
    Actor help = introduction.findActor("map-introduction-help");
    assertTrue(panel.isVisible());
    assertFalse(help.isVisible());
    assertNull(stage.getKeyboardFocus());
    assertNull(stage.getScrollFocus());

    introduction.findActor("map-introduction-close").fire(new ChangeEvent());
    assertFalse(panel.isVisible());
    assertTrue(help.isVisible());

    help.fire(new ChangeEvent());
    assertTrue(panel.isVisible());
    assertFalse(help.isVisible());
  }

  @Test
  void rebuildingTheMapKeepsTheIntroductionCollapsed() {
    introduction.remove();
    MapIntroduction returning = new MapIntroduction(skin, runState, panelTexture);
    stage.addActor(returning);

    assertFalse(returning.findActor("map-introduction-panel").isVisible());
    assertTrue(returning.findActor("map-introduction-help").isVisible());
    returning.findActor("map-introduction-help").fire(new ChangeEvent());
    assertTrue(returning.findActor("map-introduction-panel").isVisible());
  }

  @Test
  void mapNodesRemainClickableWhileTheIntroductionIsExpanded() {
    Actor node = new Actor();
    node.setBounds(600f, 300f, 60f, 60f);
    MapSelectionController controller = new MapSelectionController(runState.getMapGraph());
    node.addListener(
        new InputListener() {
          @Override
          public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            return controller.onNodeClicked(7);
          }
        });
    stage.getRoot().addActorAt(0, node);

    Actor target = stage.hit(620f, 320f, true);
    assertSame(node, target);
    InputEvent click = new InputEvent();
    click.setType(InputEvent.Type.touchDown);
    target.fire(click);

    assertEquals(7, runState.getMapGraph().getCurrentNode().getNodeId());
  }

  @Test
  void panelFitsTheLeftMarginAndStaysBelowTheHudAfterResize() {
    Table panel = introduction.findActor("map-introduction-panel");
    panel.validate();
    assertTrue(panel.getX() >= 0f);
    assertTrue(panel.getRight() <= 280f);
    assertTrue(panel.getY() >= 0f);
    assertTrue(panel.getTop() <= 700f);

    introduction.resize(1600f, 900f);
    assertTrue(panel.getY() >= 0f);
    assertTrue(panel.getTop() <= 800f);
    assertTrue(panel.getRight() <= 280f);
  }

  @Test
  void removingTheIntroductionAlsoRemovesItsControls() {
    Actor help = introduction.findActor("map-introduction-help");

    introduction.remove();

    assertNull(help.getStage());
    assertNull(stage.getRoot().findActor("map-introduction"));
    assertTrue(runState.isRunActive());
    assertTrue(new MapSelectionController(runState.getMapGraph()).isSelectable(7));
  }
}
