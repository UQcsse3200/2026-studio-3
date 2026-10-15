package com.csse3200.game.tutorial;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/** Read-only tutorial probes. The battle and animation owners retain all timing and rules. */
public record BattleTutorialObservation(
    IntSupplier enemyHealth, IntSupplier playerHealth, BooleanSupplier visualsSettled) {}
