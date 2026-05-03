package com.yang.moni.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetService {

    private final AssetRepository assetRepository;

    public List<AssetResponse> getByLedgerId(Long ledgerId) {
        return assetRepository.findByLedgerIdAndActiveTrueOrderBySortOrderAsc(ledgerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AssetResponse createAsset(Long ledgerId, AssetRequest request) {
        int nextOrder = (int) assetRepository.countByLedgerIdAndActiveTrue(ledgerId) + 1;
        Asset asset = Asset.builder()
                .ledgerId(ledgerId)
                .assetName(request.assetName())
                .assetType(request.assetType())
                .balance(request.balance())
                .sortOrder(nextOrder)
                .build();
        return toResponse(assetRepository.save(asset));
    }

    @Transactional
    public void reorderAssets(Long ledgerId, List<Long> orderedIds) {
        List<Asset> assets = assetRepository.findByLedgerIdAndActiveTrueOrderBySortOrderAsc(ledgerId);
        for (int i = 0; i < orderedIds.size(); i++) {
            final int order = i + 1;
            final Long id = orderedIds.get(i);
            assets.stream()
                    .filter(a -> a.getAssetId().equals(id))
                    .findFirst()
                    .ifPresent(a -> a.updateSortOrder(order));
        }
    }

    @Transactional
    public AssetResponse updateAsset(Long ledgerId, Long assetId, AssetRequest request) {
        Asset asset = assetRepository
                .findByAssetIdAndLedgerIdAndActiveTrue(assetId, ledgerId)
                .orElseThrow(() -> new NoSuchElementException("Asset not found"));
        asset.update(request.assetName(), request.assetType(), request.balance());
        return toResponse(asset);
    }

    @Transactional
    public void deleteAsset(Long ledgerId, Long assetId) {
        Asset asset = assetRepository
                .findByAssetIdAndLedgerIdAndActiveTrue(assetId, ledgerId)
                .orElseThrow(() -> new NoSuchElementException("Asset not found"));
        asset.deactivate();
    }

    private AssetResponse toResponse(Asset a) {
        return new AssetResponse(
                a.getAssetId(),
                a.getAssetName(),
                a.getAssetType(),
                a.getBalance(),
                a.getSortOrder()
        );
    }
}
