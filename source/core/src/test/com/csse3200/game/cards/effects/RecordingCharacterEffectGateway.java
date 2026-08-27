package com.csse3200.game.cards.effects;

import java.util.ArrayList;
import java.util.List;

/** In-memory character boundary used to verify effect execution without Team 7 dependencies. */
class RecordingCharacterEffectGateway implements CharacterEffectGateway {
  private final int strengthModifier;
  int block;
  int healing;
  int strength;
  final List<String> events = new ArrayList<>();

  RecordingCharacterEffectGateway() {
    this(0);
  }

  RecordingCharacterEffectGateway(int strengthModifier) {
    this.strengthModifier = strengthModifier;
  }

  @Override
  public int getStrengthModifier() {
    return strengthModifier;
  }

  @Override
  public void gainBlock(int amount) {
    block += amount;
    events.add("BLOCK:" + amount);
  }

  @Override
  public void heal(int amount) {
    healing += amount;
    events.add("HEAL:" + amount);
  }

  @Override
  public void gainStrength(int amount) {
    strength += amount;
    events.add("STRENGTH:" + amount);
  }
}
