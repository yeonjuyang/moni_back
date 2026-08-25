package com.yang.moni.asset;

import com.yang.moni.ledger.LedgerAccessGuard;
import com.yang.moni.transaction.TransactionRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetService {

    private final AssetRepository assetRepository;
    private final TransactionRecordRepository transactionRepository;
    private final LedgerAccessGuard accessGuard;

    public List<AssetResponse> getByLedgerId(Long ledgerId) {
        accessGuard.requireMember(ledgerId);
        List<Asset> assets = assetRepository.findByLedgerIdAndActiveTrueOrderBySortOrderAsc(ledgerId);
        if (assets.isEmpty()) return List.of();

        List<Long> ids = assets.stream().map(Asset::getAssetId).toList();
        Map<Long, Long> incomeMap = sumToMap(transactionRepository.sumIncomeGroupByAsset(ids));
        Map<Long, Long> expenseMap = sumToMap(transactionRepository.sumExpenseGroupByAsset(ids));

        return assets.stream().map(a -> {
            long current = a.getBalance()
                    + incomeMap.getOrDefault(a.getAssetId(), 0L)
                    - expenseMap.getOrDefault(a.getAssetId(), 0L);
            return new AssetResponse(a.getAssetId(), a.getAssetName(), a.getAssetType(), current, a.getSortOrder());
        }).toList();
    }

    @Transactional
    public AssetResponse createAsset(Long ledgerId, AssetRequest request) {
        accessGuard.requireMember(ledgerId);
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
        accessGuard.requireMember(ledgerId);
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
        accessGuard.requireMember(ledgerId);
        Asset asset = assetRepository
                .findByAssetIdAndLedgerIdAndActiveTrue(assetId, ledgerId)
                .orElseThrow(() -> new NoSuchElementException("Asset not found"));

        // request.balance()는 목표 현재 잔액 → 초기 잔액으로 역산하여 저장
        List<Long> ids = List.of(assetId);
        long income = sumToMap(transactionRepository.sumIncomeGroupByAsset(ids)).getOrDefault(assetId, 0L);
        long expense = sumToMap(transactionRepository.sumExpenseGroupByAsset(ids)).getOrDefault(assetId, 0L);
        long initialBalance = request.balance() - income + expense;

        asset.update(request.assetName(), request.assetType(), initialBalance);
        return new AssetResponse(asset.getAssetId(), asset.getAssetName(), asset.getAssetType(), request.balance(), asset.getSortOrder());
    }

    @Transactional
    public void deleteAsset(Long ledgerId, Long assetId) {
        accessGuard.requireMember(ledgerId);
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

    private Map<Long, Long> sumToMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
    }
}
