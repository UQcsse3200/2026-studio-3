# Shop refresh and acquisition integration

Implementation based on main `d06674fb`, for Team 2 feature #439 and personal task #441.
This is a backend and interface handoff, not completed UI/manual acceptance.

## Confirmed behaviour

- Each Shop encounter starts with its existing three-card stock and the player's existing discount.
- Its first successful refresh is free. Later successful refreshes cost 10, 20, 40, 80 gold, etc.
- Zixuan confirmed the first paid fee as 10 gold. `ShopRefreshPolicy.standard()` supplies this production policy; tests using alternative fees exercise configuration only.
- Every successful refresh replaces all three offers, including their base prices and stock.
- Refreshed stock does not receive the player's discount. The permanent discount items remain owned, and a different new Shop still starts with that discount.
- Refresh fees are not discounted. Purchases apply the effective batch discount exactly once.
- A failed generation or recovered payment failure preserves stock, gold, successful refresh count and the current batch's discount.
- Failed rollback is reported explicitly and blocks further transactions; it is not reported as a clean failure.
- Fee overflow disables further refreshes rather than wrapping to a cheap/free fee.
- Repeated cards across refresh batches are allowed. Each batch contains three distinct card IDs, as in the current generator.
- Refresh counts belong to the live Shop encounter. Saving/restoring stock and refresh counts is not implemented; the existing Shop has no stock persistence either. Agree any stronger save/load or per-run limit before claiming it.
- Current Shop goods are cards. Item/effect result display is supported, but this does not add purchasable item stock or an item-purchase engine.

## Main integration and configuration

`EncounterGameArea.displayShop()` enables the backend with the existing card generator and
`ShopRefreshPolicy.standard()`. Both free and paid refreshes are configured; Jun does not need
to supply a fee. No refresh button is added here; Jun owns it.

The explicit pending-fee policy is retained for custom/test encounters. Only those encounters
can confirm their paid fee once, without restoring an already-used free refresh:

```java
shopEncounter.setFirstPaidRefreshFee(approvedFee);
```

Alternatively, a newly constructed Shop can enable an approved policy directly:

```java
shopEncounter.enableRefresh(new ShopRefreshPolicy(approvedFee), generator::createShop);
```

The factory must only generate stock; it must not mutate player/deck/map state or navigate.
Shared gateway adapters implement `chargeServiceFee()` without granting a card. Older custom
adapters default to failure and must implement this operation if paid refresh is required.

## Jun: UI contract

Read immutable data through `ShopEncounter.snapshot()`. Each offer contains its original price,
actual price and stock. The snapshot also supplies gold, effective discount, the next refresh fee,
successful refresh count, completion state and a revision.

```java
ShopSnapshot displayed = shopEncounter.snapshot();
ShopRefreshResult refreshed = shopEncounter.refresh(displayed.revision(), actionId);
ShopPurchaseReceipt bought = shopEncounter.purchase(offerId, displayed.revision(), actionId);
ShopSnapshot current = shopEncounter.snapshot();
```

- Generate one unique action ID per user action; retrying that request uses the SAME ID and revision.
- A second intentional action uses a new ID and the latest displayed revision.
- Replays return the cached receipt without another charge/reward. Reused IDs with different arguments or command types are rejected.
- A successful purchase or refresh changes the revision. Old quotes/product clicks are rejected even if a new batch reuses a card ID.
- Rebuild all product widgets after a successful refresh. Clear the old `purchasedItemIds` presentation set: the same card may be offered again in a new batch.
- Do not call the existing `ShopDisplay.refresh()` expecting new stock. That method only updates labels and availability.
- The existing display's pricing/discount reads now use the effective Shop discount, rather than blindly applying the permanent discount to refreshed stock.
- A missing next-fee value means the price is not configured, has overflowed, or transactions are unavailable. Do not display this as another free refresh.
- Keep disabled/busy/error feedback and rebuild from the returned current snapshot. A failed attempt is retryable with a new action ID after the cause is resolved.
- The legacy `purchase(itemId)` remains compatible, but new UI must use the versioned command to receive duplicate/stale protection and acquisition receipts.

## Yihan and Guoqing: result contract

`AcquisitionResult` is immutable, already-applied result data. It has one stable result ID and
a list of entries. Entry kinds are `CARD`, `ITEM`, `POSITIVE_EFFECT`, `NEGATIVE_EFFECT`,
`GOLD` and `HP`. The existing six-argument `Entry` constructor is unchanged.
Each includes a stable ID, quantity, name, description and optional effect duration in turns.
For goods, quantity is a positive count; for effects, it is a positive absolute magnitude and
the effect kind carries the sign. For `GOLD` and `HP`, quantity is the nonzero signed actual
change and duration must be null. Null effect duration means no turn duration is being declared;
it does not automatically mean permanent. Descriptions must explain the real effect, target and duration.
Zero-effect/no-reward Events can retain their existing Outcome view instead of inventing a gain.

For successful Shop purchases, `ShopPurchaseReceipt.acquisition()` is prepared only AFTER the
card has been added and payment committed. Failed purchases have no acquisition result.
The receipt also contains the actual charged price and stock revision.

Yihan continues owning Event reward/effect execution. After applying the result exactly once,
create this data with a stable Event-result ID. Do not generate a new ID each time the UI rebuilds.
Actual effect lists, targets and duration semantics still require Yihan's confirmation.

### Mixed resource and item results

Use `Entry.gold(actualGoldDelta)` and `Entry.hp(actualHpDelta)` for settled resource changes.
The factories supply stable resource IDs (`gold` and `hp`), not Event-result IDs.
Calculate each delta from the resource balance after settlement minus its balance before
settlement. Report actual changes after health caps or other game rules, not the requested
reward/damage. Positive deltas mean a gain/heal; negative deltas mean a loss/damage.
Omit zero changes: the factories reject zero. If every change is zero, use the existing Outcome view.

For a choice that has already granted 50 gold and one Curse Item:

```java
AcquisitionResult result = new AcquisitionResult(
    eventResultId,
    List.of(
        AcquisitionResult.Entry.gold(50),
        new AcquisitionResult.Entry(
            AcquisitionResult.Kind.ITEM, curseItemId, 1, curseItemName, curseDescription, null)));
presentation.show(result, dismissal -> {
  // Resume/finish the Event; settlement already happened before show().
});
```

Use the actual Team 7 item ID, name and description supplied by Yihan. A Curse Item acquisition
is an `ITEM`; add a `NEGATIVE_EFFECT` only when a separate effect was actually applied.
Do not duplicate the same gold/health change as both a resource entry and a generic effect.
One settled choice has one stable result ID and one display containing all its entries.
Retries and view rebuilds reuse that result ID; a later intentional choice gets a new result ID.

Guoqing's view needs to handle both new kinds. Check `entry.isResourceChange()` and read
`entry.delta()` to display a signed gold/HP amount. Calling `delta()` on goods/effects throws
`IllegalStateException`; their existing `quantity()` and duration semantics are unchanged.
The result model and presentation controller do not change gold, HP, items or statuses.
Yihan owns settlement and Event adaptation; Guoqing owns the resource visuals and mixed-result layout.
This extension does not implement those adapters or claim completed in-game integration.

Guoqing implements the small presentation boundary:

```java
public interface AcquisitionView {
  void show(AcquisitionResult result, Runnable onContinue, Runnable onClose);
  void hide();
}
```

The owning screen/encounter creates ONE `AcquisitionPresentation` and calls:

```java
presentation.show(result, dismissal -> {
  // Owner decides whether CONTINUE/CLOSE resumes shopping or completes an Event.
  // Do not grant the rewards again.
});
```

This controller rejects overlapping/duplicate results, handles Continue/Close only once,
invalidates old callbacks, and cancels navigation callbacks on disposal. Dispose it with
`presentation.close()` before destroying the owning screen. `hide()` must remove listeners
and actors without applying gameplay effects; callbacks run on the game/UI thread.

Continue and Close are reported separately. Their exact Event navigation policy is NOT chosen
here; agree it with Yihan/Guoqing. Opening the display does not itself complete a Map node.
No event effects or acquisition UI have been added while their owners' replies are pending.

## Verification

Focused tests cover fees, initial-only discounts, duplicate/stale commands, invalid generation,
payment rollback, detached snapshots, simultaneous input and result-view callback cleanup.
Real Card Library/PlayerDeck/PlayerRunState integration verifies purchased cards and permanent
discount items survive refresh and player reconstruction. Existing Shop, transaction, display
and Map encounter regressions are included.

Manual acceptance remains pending: Jun's refresh UI, Guoqing's result display, Yihan's real
effects, different window sizes and the complete Map -> Shop/Event -> Map flow.

### Local verification - 12 October 2026

- Java 21: core and desktop compilation passed.
- Full core suite after confirming the 10-gold fee: 2,076 tests, zero failures/errors/skips; 35 tests in the four new test classes.
- `spotlessCheck` passed after applying the existing project formatter.
- `git diff --check` passed.
- Command: `gradlew.bat spotlessApply :core:test :desktop:compileJava spotlessCheck --offline --console plain --max-workers=2`.
- No interactive playtest of the new UI: those components have not been implemented by their owners yet.
- Implementation branch: `Feature-2-Sprint4-ShopRefresh-Zixuan`; review target: `Feature-2-Sprint4`.
- Track publication, teammate review and the remaining UI/Event integration in #441. The local checks above do not claim completed in-game acceptance.

### Pre-review verification - 13 October 2026

- Java 21 fresh run: 2,086 core tests, zero failures/errors/skips.
- Desktop compilation and `spotlessCheck` passed.
- Command: `gradlew.bat :core:test :desktop:compileJava spotlessCheck --offline --console plain --max-workers=2 --rerun-tasks`.
- Verification covers this implementation branch based on main `d06674fb`; final UI/Event integration and in-game acceptance remain pending.

### Local resource-result verification - 14 October 2026

- Synced the local personal branch to Team 2 branch `Feature-2-Sprint4` at `316f88ea` before extending the result model.
- Added `GOLD` and `HP`, signed settled deltas, resource factories and validation without changing the existing constructor or goods/effect rules.
- Added 10 tests for signed/extreme amounts, invalid zero/duration values, legacy compatibility, immutable mixed results and one-shot presentation callbacks.
- Java 21 fresh run: 2,138 core tests, zero failures/errors/skips. The result model and presentation test classes contain 20 passing tests in total.
- Desktop compilation, `spotlessCheck` and `git diff --check` passed.
- Command: `gradlew.bat spotlessApply :core:test :desktop:compileJava spotlessCheck --offline --console plain --max-workers=2 --rerun-tasks`.
- Verification was performed locally before publication. Publication to `Feature-2-Sprint4` does not imply teammate review or completed UI/Event acceptance.
- Yihan's real Event adapter and Guoqing's mixed-resource display are not implemented here. No interactive acceptance of that flow is claimed.
