package com.csse3200.game.components.chance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import org.junit.jupiter.api.Test;

class EncounterInformationShadeTest {
  @Test
  void fadesSmoothlyThroughAllSevenStops() {
    float[] expected = {.88f, .70f, .56f, .44f, .32f, .18f, 0f};
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], EncounterInformationShade.opacity(i / 6f), .0001f);
    }
    for (int i = 1; i <= 100; i++) {
      assertTrue(
          EncounterInformationShade.opacity(i / 100f)
              <= EncounterInformationShade.opacity((i - 1) / 100f));
    }
  }

  @Test
  void ellipticalShadeFadesDownAndRightWithoutADarkLowerLeft() {
    assertEquals(0f, EncounterInformationShade.spatialOpacity(0, 0, 300, 36));
    assertEquals(.88f, EncounterInformationShade.spatialOpacity(0, 36, 300, 36), .0001f);
    assertEquals(0f, EncounterInformationShade.spatialOpacity(0, 300, 300, 36));
    assertEquals(0f, EncounterInformationShade.spatialOpacity(1, 100, 300, 36));
    float upperLeft = EncounterInformationShade.spatialOpacity(.1f, 80, 300, 36);
    assertTrue(upperLeft > EncounterInformationShade.spatialOpacity(.6f, 80, 300, 36));
    assertTrue(upperLeft > EncounterInformationShade.spatialOpacity(.1f, 220, 300, 36));
    assertTrue(EncounterInformationShade.spatialOpacity(.1f, 280, 300, 36) < .10f);
    assertTrue(EncounterInformationShade.spatialOpacity(.45f, 130, 300, 36) > .50f);
  }

  @Test
  void hasNoLayoutInsetsAndRestoresBatchTint() {
    Table information = mock(Table.class);
    when(information.getPrefHeight()).thenReturn(200f);
    EncounterInformationShade shade =
        new EncounterInformationShade(information, mock(Drawable.class));
    assertEquals(0f, shade.getMinWidth());
    assertEquals(0f, shade.getMinHeight());
    assertEquals(0f, shade.getLeftWidth());
    assertEquals(0f, shade.getTopHeight());
    Batch batch = mock(Batch.class);
    when(batch.getColor()).thenReturn(new Color(1, 1, 1, .5f));
    when(batch.getPackedColor()).thenReturn(123f);
    shade.draw(batch, 0, 0, 1280, 800);
    verify(batch).setPackedColor(123f);
  }
}
