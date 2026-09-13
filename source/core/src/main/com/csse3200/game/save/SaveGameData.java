package com.csse3200.game.save;

/** Root DTO stored in each versioned save-slot JSON file. */
public class SaveGameData {
  public static final int CURRENT_SCHEMA_VERSION = 1;

  public int schemaVersion = CURRENT_SCHEMA_VERSION;
  public SaveSlotMetadata metadata = new SaveSlotMetadata();
  public PlayerSaveData player = new PlayerSaveData();
  public DeckSaveData deck = new DeckSaveData();
  public MapSaveData map = new MapSaveData();
  public ProgressSaveData progress = new ProgressSaveData();

  /** Required for JSON deserialisation. */
  public SaveGameData() {}

  public SaveGameData(
      PlayerSaveData player, DeckSaveData deck, MapSaveData map, ProgressSaveData progress) {
    this.player = player;
    this.deck = deck;
    this.map = map;
    this.progress = progress;
  }
}
