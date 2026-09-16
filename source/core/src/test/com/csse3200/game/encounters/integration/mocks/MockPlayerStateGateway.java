package com.csse3200.game.encounters.integration.mocks;

import com.csse3200.game.encounters.integration.PlayerStateGateway;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Controllable player-state mock shared by encounter integration tests. */
public final class MockPlayerStateGateway implements PlayerStateGateway {
  private int health;
  private final int maxHealth;
  private int currency;
  private boolean failNextCurrencyUpdate;
  private boolean failCurrencyUpdates;
  private boolean rejectNextHealthUpdate;
  private final List<String> mutations = new ArrayList<>();

  public MockPlayerStateGateway(int health, int currency) {
    this(health, Math.max(health, 100), currency);
  }

  public MockPlayerStateGateway(int health, int maxHealth, int currency) {
    this.health = health;
    this.maxHealth = maxHealth;
    this.currency = currency;
  }

  @Override
  public int getHealth() {
    return health;
  }

  @Override
  public int getMaxHealth() {
    return maxHealth;
  }

  @Override
  public void setHealth(int health) {
    if (rejectNextHealthUpdate) {
      rejectNextHealthUpdate = false;
      return;
    }
    this.health = Math.max(0, Math.min(maxHealth, health));
  }

  @Override
  public void applyDirectHealthChange(int amount) {
    mutations.add("health");
    if (rejectNextHealthUpdate) {
      rejectNextHealthUpdate = false;
      return;
    }
    if (amount > 0) {
      health = Math.min(maxHealth, health + amount);
    } else if (amount < 0 && health > 0) {
      health = Math.max(0, health + amount);
    }
  }

  @Override
  public int getCurrency() {
    return currency;
  }

  @Override
  public void setCurrency(int currency) {
    mutations.add("currency");
    if (failCurrencyUpdates || failNextCurrencyUpdate) {
      failNextCurrencyUpdate = false;
      throw new IllegalStateException("Simulated currency update failure");
    }
    this.currency = Math.max(0, currency);
  }

  public void failNextCurrencyUpdate() {
    failNextCurrencyUpdate = true;
  }

  public void failAllCurrencyUpdates() {
    failCurrencyUpdates = true;
  }

  public void rejectNextHealthUpdate() {
    rejectNextHealthUpdate = true;
  }

  public List<String> getMutations() {
    return Collections.unmodifiableList(mutations);
  }
}
