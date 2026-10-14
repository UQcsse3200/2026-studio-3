package com.csse3200.game.screens;

import static org.junit.jupiter.api.Assertions.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.csse3200.game.extensions.GameExtension;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CampfireUpgradeEntranceTest {
  @Test
  void bothEntrancesUseTheSameCallbackAndRespectSceneTransitions() {
    Skin skin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
    try {
      AtomicInteger opens = new AtomicInteger();
      AtomicBoolean blocked = new AtomicBoolean();
      var entrance = new CampfireUpgradeEntrance(skin, opens::incrementAndGet, blocked::get);
      Actor altar = entrance.findActor("upgrade-rune-altar");
      TextButton button = entrance.findActor("workbench-upgrade-button");
      assertEquals("Upgrade", button.getText().toString());
      assertEquals(entrance.getWidth() / 2, button.getX() + button.getWidth() / 2);
      assertSame(altar, altar.hit(altar.getWidth() / 2, altar.getHeight() / 2, true));
      assertSame(altar, altar.hit(altar.getWidth() * .08f, altar.getHeight() / 2, true));
      assertNull(altar.hit(0, 0, true));
      click(altar);
      click(altar);
      button.fire(new ChangeEvent());
      assertEquals(0, opens.get());
      entrance.act(.15f);
      assertEquals(0, opens.get());
      entrance.act(.08f);
      assertEquals(1, opens.get());
      entrance.act(1f);
      assertEquals(1, opens.get());
      button.fire(new ChangeEvent());
      assertEquals(2, opens.get());
      blocked.set(true);
      click(altar);
      entrance.act(.3f);
      button.fire(new ChangeEvent());
      assertEquals(2, opens.get());
    } finally {
      skin.dispose();
    }
  }

  private static void click(Actor actor) {
    for (var listener : actor.getListeners()) {
      if (listener instanceof ClickListener click) click.clicked(new InputEvent(), 0, 0);
    }
  }
}
