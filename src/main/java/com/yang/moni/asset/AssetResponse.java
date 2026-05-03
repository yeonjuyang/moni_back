package com.yang.moni.asset;

public record AssetResponse(
        Long assetId,
        String assetName,
        String assetType,
        Long balance,
        int sortOrder
) {}
