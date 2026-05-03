package com.yang.moni.transaction;

import com.yang.moni.category.Category;
import com.yang.moni.category.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRecordRepository repository;
    private final CategoryRepository categoryRepository;

    public List<TransactionResponse> getByLedgerId(Long ledgerId) {
        return repository.findByLedgerIdOrderByTransactionDateDesc(ledgerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TransactionResponse createTransaction(Long ledgerId, TransactionRequest request) {
        Category category = null;
        if (request.categoryName() != null) {
            category = categoryRepository
                    .findByLedgerIdAndCategoryName(ledgerId, request.categoryName())
                    .orElse(null);
        }

        TransactionRecord record = TransactionRecord.builder()
                .ledgerId(ledgerId)
                .transactionType(request.transactionType())
                .category(category)
                .amount(request.amount())
                .memo(request.memo())
                .transactionDate(request.transactionDate())
                .fromAssetId(request.fromAssetId())
                .toAssetId(request.toAssetId())
                .paidByUserId(1L) // TODO: replace with JWT claim after auth is implemented
                .createdByUserId(1L) // TODO: replace with JWT claim after auth is implemented
                .build();

        return toResponse(repository.save(record));
    }

    private TransactionResponse toResponse(TransactionRecord t) {
        return new TransactionResponse(
                t.getTransactionId(),
                t.getMemo(),
                t.getAmount(),
                t.getTransactionType(),
                t.getTransactionDate(),
                t.getCategory() != null ? t.getCategory().getCategoryName() : null
        );
    }
}
