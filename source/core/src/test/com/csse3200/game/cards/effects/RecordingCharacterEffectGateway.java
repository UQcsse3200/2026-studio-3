package com.csse3200.game.cards.effects;

import java.util.ArrayList;
import java.util.List;

/** In-memory character boundary used to verify effect execution without Team 7 dependencies. */
class RecordingCharacterEffectGateway implements CharacterEffectGateway {
  int damage;
  int block;
  int healing;
  int poison;
  int poisonDuration;
  int vulnerable;
  int vulnerableDuration;
  int strength;
  final List<String> events = new ArrayList<>();

  @Override
  public void damage(int amount) {
    damage += amount;
    events.add("DAMAGE:" + amount);
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
  public void applyPoison(int stacks, int duration) {
    poison += stacks;
    poisonDuration = duration;
    events.add("POISON:" + stacks + ":" + duration);
  }

  @Override
  public void applyVulnerable(int stacks, int duration) {
    vulnerable += stacks;
    vulnerableDuration = duration;
    events.add("VULNERABLE:" + stacks + ":" + duration);
  }

  @Override
  public void gainStrength(int amount) {
    strength += amount;
    events.add("STRENGTH:" + amount);
  }
}
