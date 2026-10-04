package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.components.Component;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.rendering.AnimationRenderComponent;

public class EnemyReleaseComponent extends Component {
    private static final float DURATION = 2;
    private float elapsed;
    // not sure about this colour yet.
    private static final Color RELEASE_COLOUR = new Color(1f, 0.95f, 0.8f, 1f);

    private AnimationRenderComponent animator;
    private final Color startingColour = new Color(Color.WHITE);
    private final Color currentColour = new Color();

    public boolean isFinished() {
        return elapsed >= DURATION;
    }

    @Override
    public void create() {
        super.create();
        animator = entity.getComponent(AnimationRenderComponent.class);

        // Captures enemy tint when the 'death' effect starts.
        if (animator != null && animator.getActiveTint() != null) {
            startingColour.set(animator.getActiveTint());
        }
    }

    @Override
    public void update() {
       elapsed = Math.min(
               elapsed + ServiceLocator.getTimeSource().getDeltaTime(), DURATION
       );

       if (animator != null) {
           float progress =  elapsed / DURATION;
           // using lerp to blend the colour over a duration instead of a cold switch
           currentColour.set(startingColour).lerp(RELEASE_COLOUR, progress);
           animator.setPersistentTint(currentColour);
       }
    }
}
