package com.csse3200.game.bestiary;

import com.csse3200.game.entities.configs.EnemyTier;
import java.util.List;

/** Temporary data source used while persistent bestiary progress is being integrated. */
public class MockBestiaryDataSource implements BestiaryDataSource {
  private static final List<BestiaryEntry> ENTRIES =
      List.of(
          new BestiaryEntry(
              "lesser_shade",
              "Lesser Shade",
              EnemyTier.NORMAL,
              "A restless shadow that lashes out at travellers who stray too far from the path.",
              "images/enemies/lesser_shade.png",
              24,
              6,
              0,
              BestiaryUnlockState.DEFEATED),
          new BestiaryEntry(
              "bone_crawler",
              "Bone Crawler",
              EnemyTier.NORMAL,
              "A skittering creature protected by plates of scavenged bone.",
              "images/enemies/bone_crawler.png",
              30,
              5,
              2,
              BestiaryUnlockState.LOCKED),
          new BestiaryEntry(
              "dark_acolyte",
              "Dark Acolyte",
              EnemyTier.NORMAL,
              "A devoted servant of the dark, dangerous when allowed to gather strength.",
              "images/enemies/dark_acolyte.png",
              34,
              7,
              1,
              BestiaryUnlockState.ENCOUNTERED),
          new BestiaryEntry(
              "void_knight",
              "Void Knight",
              EnemyTier.ELITE,
              "An armoured champion whose defence is as threatening as its blade.",
              "images/enemies/void_knight.png",
              72,
              10,
              5,
              BestiaryUnlockState.DEFEATED),
          new BestiaryEntry(
              "mock_boss",
              "Unrevealed Boss",
              EnemyTier.BOSS,
              "Temporary mock entry used to develop the Boss category.",
              "images/enemies/default.png",
              120,
              15,
              8,
              BestiaryUnlockState.LOCKED));

  @Override
  public List<BestiaryEntry> getEntries() {
    return ENTRIES;
  }
}
