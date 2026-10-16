package com.csse3200.game.components.combat;

/** Identifies the source category of damage for combat feedback. */
public enum DamageType {
    /** Standard damage that can be reduced by block and armour. */
    NORMAL,

    /** Damage that bypasses block and armour. */
    PIERCE,

    /** Ongoing poison damage that bypasses block and armour. */
    POISON
}