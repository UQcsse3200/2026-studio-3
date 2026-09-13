package com.csse3200.game.components.gamearea;

import java.util.ArrayList;
import java.util.List;

public class CombatBackgroundConfigs {
    public List<CombatBackgroundConfig> backgrounds = new ArrayList<>();

    /**
     * Finds a background by its unique id.
     *
     * @param id The background ID.
     * @return The matching configuration, or null if it doesn't exist.
     */
    public CombatBackgroundConfig get(String id) {
      if (id == null || backgrounds == null) return null;

      for (CombatBackgroundConfig background: backgrounds) {
           if (id.equals(background.id)) {
               return background;
           }
       }
       return null;
    }
}

