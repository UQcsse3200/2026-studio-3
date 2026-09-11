package com.csse3200.game.entities.configs;

/** Difficulty tier of an enemy, determining both stat magnitude and behaviour complexity. */
public enum EnemyTier {
  /** modest statistics and a single behaviour pattern. */
  NORMAL,
  /** higher statistics and at least one special mechanic. */
  ELITE,
  /** highest level with multiphase behaviour. */
  BOSS
}
