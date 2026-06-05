package com.yang.moni.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LedgerMemberRepository extends JpaRepository<LedgerMember, Long> {
    List<LedgerMember> findByUserId(Long userId);
    boolean existsByLedgerIdAndUserId(Long ledgerId, Long userId);
    Optional<LedgerMember> findByLedgerIdAndUserId(Long ledgerId, Long userId);
}
