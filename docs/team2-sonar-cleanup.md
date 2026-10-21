# Team 2 SonarCloud cleanup

Baseline: main commit `80a33a33a3d9b1f9f47c6bbc7830002fce26c4ef`, analyzed on 21 October 2026 at 10:25:35 Australia/Brisbane. The project had 153 unresolved issues. The Team 2 feature audit identified 82 core issues and 3 related issues in shared files.

This branch addresses the code associated with 79 of those 85 candidates. Actual resolved counts and any newly reported findings must be confirmed by the next SonarCloud analysis. The remaining 6 are deliberately retained by agreement for this cleanup; no rules or issue statuses were suppressed.

## Changes

- Use floating-point operands for the two Shop drawing calculations reported as Low reliability issues, and clarify the related numeric conversions.
- Apply Java 21 clamp operations, separate declarations, remove unused private presentation code and no-op hover handling, and preserve validation while simplifying test lambdas.
- Split five broad UI/tutorial tests into independent behaviors using shared setup. All modified test files retain their original assertion counts; the suite gains six independently executed test cases.
- Expand nested UI expressions while retaining branch priority, selection states, text, colors and offsets. Preserve Shop validation outcomes and purchase revisions. Share the identical pause/hide card-interaction cleanup.

## Retained findings

Locations below refer to the baseline commit. All six have Medium software-quality impact in SonarCloud. Retention concerns the risk or cost of changing the code, rather than a High severity classification from SonarCloud.

| Rule and issue | Location under `source/core/src/main/com/csse3200/game/` | Reason |
| --- | --- | --- |
| [S2589 / AaFZuVLFcFK5dytthtlY](https://sonarcloud.io/project/issues?id=UQcsse3200_2026-studio-3&issues=AaFZuVLFcFK5dytthtlY&open=AaFZuVLFcFK5dytthtlY) | `encounters/results/AcquisitionPresentation.java:45` | `view.show` receives callbacks that can change the active presentation before an exception. Preserve the identity guard pending focused reentry analysis. |
| [S2589 / AaFZuVLFcFK5dytthtlX](https://sonarcloud.io/project/issues?id=UQcsse3200_2026-studio-3&issues=AaFZuVLFcFK5dytthtlX&open=AaFZuVLFcFK5dytthtlX) | `encounters/results/AcquisitionPresentation.java:68` | `view.hide` can invoke callbacks that dispose the owner. Preserve the guard against completing a disposed encounter. |
| [S2589 / AaFZuVLscFK5dytthtla](https://sonarcloud.io/project/issues?id=UQcsse3200_2026-studio-3&issues=AaFZuVLscFK5dytthtla&open=AaFZuVLscFK5dytthtla) | `screens/TutorialVictoryScreen.java:161` | Entity updates or rendering may indirectly affect screen lifecycle. Preserve the post-update disposal check pending focused verification. |
| [S107 / AaEUd82ZmffRHwxU7zOL](https://sonarcloud.io/project/issues?id=UQcsse3200_2026-studio-3&issues=AaEUd82ZmffRHwxU7zOL&open=AaEUd82ZmffRHwxU7zOL) | `screens/CampfireScreen.java:261` | The private drawing helper has eight used parameters. Reorganizing coordinates or configuration has limited benefit for this cleanup. |
| [S107 / AaEGkrW3xnlc6LaECC6F](https://sonarcloud.io/project/issues?id=UQcsse3200_2026-studio-3&issues=AaEGkrW3xnlc6LaECC6F&open=AaEGkrW3xnlc6LaECC6F) | `components/chance/FusionSceneAssets.java:275` | The private outline helper has eight used parameters. Avoid adding per-frame allocations or changing drawing structure just to satisfy the threshold. |
| [S110 / AaFZuVLscFK5dytthtlb](https://sonarcloud.io/project/issues?id=UQcsse3200_2026-studio-3&issues=AaFZuVLscFK5dytthtlb&open=AaFZuVLscFK5dytthtlb) | `screens/TutorialVictoryScreen.java:103` | The anonymous TextButton inherits through LibGDX's UI hierarchy. Retain the working draw-time font scaling rather than replace the button implementation. |

The three S2589 findings are potential analyzer limitations requiring verification, not established false positives. Existing acquisition tests cover repeated dismissal, stale callbacks, cleanup failures and disposal; those semantics remain intact.

## Validation

From `E:/UQ/CSSE3200/2026-studio-3/source` in PowerShell:

```powershell
.\gradlew.bat -g C:/gradle-cache test :desktop:classes formatCheck
.\gradlew.bat -g C:/gradle-cache desktop:run
```

- Full suite: **2,711 tests across 336 suites**, zero failures, errors or skipped tests.
- Desktop compilation, formatting and `git diff --check` pass.
- Desktop smoke check reached `MAIN_MENU`; the test run was stopped after startup. This is not a full manual gameplay acceptance run.
- Before changes, switching from the older feature checkout left stale compiled classes after package case changes. A baseline `clean test :desktop:classes formatCheck` passed all 2,705 original tests; no Enemy/AI sources were changed to resolve that build-cache issue.
- Deferred acquisition methods, victory rendering guards and both eight-parameter methods were checked against the baseline and remain unchanged. The anonymous TextButton is retained.

SonarCloud analysis is configured for pull requests to main. The platform's issue totals are not confirmed by local compilation or tests alone.
