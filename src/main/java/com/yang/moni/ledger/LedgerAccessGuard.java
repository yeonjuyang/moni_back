package com.yang.moni.ledger;

import com.yang.moni.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;

@Component
@RequiredArgsConstructor
public class LedgerAccessGuard {

    private final LedgerRepository ledgerRepository;
    private final LedgerMemberRepository ledgerMemberRepository;

    public void requireMember(Long ledgerId) {
        boolean active = ledgerRepository.findById(ledgerId)
                .map(Ledger::isActive)
                .orElse(false);
        if (!active || !ledgerMemberRepository.existsByLedgerIdAndUserId(ledgerId, CurrentUser.id())) {
            throw new NoSuchElementException("Ledger not found: " + ledgerId);
        }
    }
}

