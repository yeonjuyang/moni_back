package com.yang.moni.transaction;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/ledgers/{ledgerId}/transactions")
    public List<TransactionResponse> getTransactions(@PathVariable Long ledgerId) {
        return transactionService.getByLedgerId(ledgerId);
    }

    @PostMapping("/ledgers/{ledgerId}/transactions")
    public ResponseEntity<TransactionResponse> createTransaction(
            @PathVariable Long ledgerId,
            @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.createTransaction(ledgerId, request));
    }
}
