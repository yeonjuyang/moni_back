package com.yang.moni.asset;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    List<Asset> findByLedgerIdAndActiveTrueOrderBySortOrderAsc(Long ledgerId);
    long countByLedgerIdAndActiveTrue(Long ledgerId);
    Optional<Asset> findByAssetIdAndLedgerIdAndActiveTrue(Long assetId, Long ledgerId);
}
