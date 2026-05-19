package com.yang.moni.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LedgerRepository extends JpaRepository<Ledger, Long> {
    List<Ledger> findByCreatedByAndActiveTrue(Long createdBy);
    Optional<Ledger> findByInviteCode(String inviteCode);
}
