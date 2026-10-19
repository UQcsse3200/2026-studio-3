package com.csse3200.game.shop;

import com.csse3200.game.encounters.results.AcquisitionResult;

/** Purchase acknowledgement with already-granted card data; acquisition is null on failure. */
public record ShopPurchaseReceipt(
    PurchaseResult result, int chargedGold, long stockRevision, AcquisitionResult acquisition) {}
