package com.csse3200.game.components.battle;

import com.csse3200.game.components.Component;
import com.csse3200.game.services.ServiceLocator;

public class EnemyReleaseComponent extends Component {
    private static final float DURATION = 2;
    private float elapsed;


    @Override
    public void update() {
        elapsed += ServiceLocator.getTimeSource().getDeltaTime();
    }

    public boolean isFinished() {
        return elapsed >= DURATION;
    }
}
