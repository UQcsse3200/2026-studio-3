package com.csse3200.game.encounters.integration.mocks;

import com.csse3200.game.encounters.integration.PlayerStateGateway;

/** Controllable player-state mock shared by encounter integration tests. */
public final class MockPlayerStateGateway implements PlayerStateGateway {
  private int health;
  private int currency;
  private boolean failNextCurrencyUpdate;
  private boolean failCurrencyUpdates;
  private boolean rejectNextHealthUpdate;

  public MockPlayerStateGateway(int health, int currency) {
    this.health = health;
    this.currency = currency;
  }

  @Override
  public int getHealth() {
    return health;
  }

  @Override
  public void setHealth(int health) {
    if (rejectNextHealthUpdate) {
      rejectNextHealthUpdate = false;
      return;
    }
    this.health = Math.max(0, health);
  }

  @Override
  public int getCurrency() {
    return currency;
  }

  @Override
  public void setCurrency(int currency) {
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
}
