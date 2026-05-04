package com.yang.moni.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long> {
    List<TransactionRecord> findByLedgerIdOrderByTransactionDateDesc(Long ledgerId);
    Optional<TransactionRecord> findByTransactionIdAndLedgerId(Long transactionId, Long ledgerId);
}
