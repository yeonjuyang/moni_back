package com.yang.moni.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long> {
    List<TransactionRecord> findByLedgerIdOrderByTransactionDateDesc(Long ledgerId);
}
