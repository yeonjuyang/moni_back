package com.yang.moni.asset;

public record AssetRequest(
        String assetName,
        String assetType,
        Long balance
) {}
