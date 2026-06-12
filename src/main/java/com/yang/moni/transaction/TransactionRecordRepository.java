package com.yang.moni.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long> {
    List<TransactionRecord> findByLedgerIdOrderByTransactionDateDesc(Long ledgerId);
    Optional<TransactionRecord> findByTransactionIdAndLedgerId(Long transactionId, Long ledgerId);

    @Query("SELECT t.toAssetId, SUM(t.amount) FROM TransactionRecord t WHERE t.toAssetId IN :assetIds GROUP BY t.toAssetId")
    List<Object[]> sumIncomeGroupByAsset(@Param("assetIds") List<Long> assetIds);

    @Query("SELECT t.fromAssetId, SUM(t.amount) FROM TransactionRecord t WHERE t.fromAssetId IN :assetIds GROUP BY t.fromAssetId")
    List<Object[]> sumExpenseGroupByAsset(@Param("assetIds") List<Long> assetIds);
}
