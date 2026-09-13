package com.csse3200.game.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SaveGameServiceTest {
  private static final Instant SAVE_TIME = Instant.parse("2026-09-09T10:15:30Z");

  private SaveGameRepository repository;
  private SaveGameSnapshotProvider snapshotProvider;
  private SaveGameService service;

  @BeforeEach
  void setUp() {
    repository = mock(SaveGameRepository.class);
    snapshotProvider = mock(SaveGameSnapshotProvider.class);
    service =
        new SaveGameService(repository, snapshotProvider, Clock.fixed(SAVE_TIME, ZoneOffset.UTC));
  }

  @Test
  void shouldCaptureAndSaveCurrentRun() {
    SaveGameData data = new SaveGameData();
    data.metadata.runLabel = "My run";
    data.progress.resumeScreen = "MAP";
    when(snapshotProvider.capture()).thenReturn(data);
    when(repository.save(eq(2), eq(data)))
        .thenAnswer(invocation -> SaveResult.success(data.metadata.copy()));

    SaveResult result = service.saveGame(2);

    assertTrue(result.success());
    assertEquals(2, data.metadata.slotId);
    assertEquals(SAVE_TIME.toEpochMilli(), data.metadata.savedAtEpochMillis);
    assertEquals(SaveGameData.CURRENT_SCHEMA_VERSION, data.schemaVersion);
    assertEquals("MAP", data.metadata.resumeScreen);
    verify(repository).save(2, data);
  }

  @Test
  void shouldSaveAlreadyCapturedData() {
    SaveGameData data = new SaveGameData();
    when(repository.save(1, data)).thenReturn(SaveResult.success(new SaveSlotMetadata()));

    SaveResult result = service.saveGame(1, data);

    assertTrue(result.success());
    verify(repository).save(1, data);
  }

  @Test
  void shouldRejectInvalidSlotBeforeCapturing() {
    SaveResult capturedResult = service.saveGame(0);
    SaveResult suppliedResult = service.saveGame(0, new SaveGameData());

    assertFalse(capturedResult.success());
    assertFalse(suppliedResult.success());
    assertEquals(SaveError.INVALID_SLOT, capturedResult.error());
    assertEquals(SaveError.INVALID_SLOT, suppliedResult.error());
    verify(snapshotProvider, never()).capture();
    verify(repository, never()).save(eq(0), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void shouldRejectMissingData() {
    SaveResult result = service.saveGame(1, null);

    assertFalse(result.success());
    assertEquals(SaveError.NO_SAVE_DATA, result.error());
    verify(repository, never()).save(eq(1), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void shouldReportCaptureFailure() {
    when(snapshotProvider.capture()).thenThrow(new IllegalStateException("unavailable"));

    SaveResult result = service.saveGame(1);

    assertFalse(result.success());
    assertEquals(SaveError.CAPTURE_FAILED, result.error());
    verify(repository, never()).save(eq(1), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void shouldRequireProviderForParameterlessSave() {
    SaveGameService providerless = new SaveGameService(repository);

    SaveResult result = providerless.saveGame(1);

    assertFalse(result.success());
    assertEquals(SaveError.NO_SAVE_DATA, result.error());
  }

  @Test
  void shouldLoadMatchingSlot() {
    SaveGameData data = new SaveGameData();
    data.metadata.slotId = 3;
    when(repository.load(3)).thenReturn(LoadResult.success(data));

    LoadResult result = service.loadGame(3);

    assertTrue(result.success());
    assertSame(data, result.data());
  }

  @Test
  void shouldRejectMismatchedSlotMetadata() {
    SaveGameData data = new SaveGameData();
    data.metadata.slotId = 2;
    when(repository.load(3)).thenReturn(LoadResult.success(data));

    LoadResult result = service.loadGame(3);

    assertFalse(result.success());
    assertEquals(SaveError.MALFORMED_SAVE, result.error());
  }

  @Test
  void shouldPreserveRepositoryLoadFailure() {
    LoadResult missing = LoadResult.failure(SaveError.SLOT_NOT_FOUND, "missing");
    when(repository.load(4)).thenReturn(missing);

    assertSame(missing, service.loadGame(4));
  }

  @Test
  void shouldDelegateListAndDelete() {
    SaveSlotListResult listResult =
        SaveSlotListResult.success(List.of(new SaveSlotMetadata(1, 10, "Run", 20, "MAP")));
    DeleteSaveResult deleteResult = DeleteSaveResult.succeeded();
    when(repository.listSaveSlots()).thenReturn(listResult);
    when(repository.delete(1)).thenReturn(deleteResult);

    assertSame(listResult, service.listSaveSlots());
    assertSame(deleteResult, service.deleteSave(1));
  }

  @Test
  void shouldValidateConstructorArguments() {
    Clock clock = Clock.systemUTC();
    assertThrows(
        IllegalArgumentException.class, () -> new SaveGameService(null, snapshotProvider, clock));
    assertThrows(
        IllegalArgumentException.class,
        () -> new SaveGameService(repository, snapshotProvider, null));
  }
}
