package com.yang.moni.ledger;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    @GetMapping("/ledgers")
    public ResponseEntity<List<LedgerResponse>> getMyLedgers() {
        return ResponseEntity.ok(ledgerService.getMyLedgers());
    }

    @GetMapping("/ledgers/{id}")
    public ResponseEntity<LedgerResponse> getLedger(@PathVariable Long id) {
        return ResponseEntity.ok(ledgerService.getLedger(id));
    }

    @PutMapping("/ledgers/{id}")
    public ResponseEntity<LedgerResponse> updateLedger(@PathVariable Long id,
                                                        @RequestBody LedgerRequest request) {
        return ResponseEntity.ok(ledgerService.updateLedger(id, request));
    }

    @PostMapping("/ledgers")
    public ResponseEntity<LedgerResponse> createLedger(@RequestBody LedgerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ledgerService.createLedger(request));
    }

    @DeleteMapping("/ledgers/{id}")
    public ResponseEntity<Void> deleteLedger(@PathVariable Long id) {
        ledgerService.deleteLedger(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/ledgers/{id}/invite")
    public ResponseEntity<LedgerResponse> generateInviteCode(@PathVariable Long id) {
        return ResponseEntity.ok(ledgerService.generateInviteCode(id));
    }

    @PostMapping("/ledgers/join")
    public ResponseEntity<LedgerResponse> joinLedger(@RequestBody LedgerJoinRequest request) {
        return ResponseEntity.ok(ledgerService.joinLedger(request.inviteCode()));
    }
}
