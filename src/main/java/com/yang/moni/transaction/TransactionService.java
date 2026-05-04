package com.yang.moni.transaction;

import com.yang.moni.category.Category;
import com.yang.moni.category.CategoryRepository;
import com.yang.moni.user.User;
import com.yang.moni.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRecordRepository repository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public List<TransactionResponse> getByLedgerId(Long ledgerId) {
        Map<Long, String> userMap = userRepository.findByActiveTrue().stream()
                .collect(Collectors.toMap(User::getUserId, User::getNickname));
        return repository.findByLedgerIdOrderByTransactionDateDesc(ledgerId)
                .stream()
                .map(t -> toResponse(t, userMap))
                .toList();
    }

    @Transactional
    public TransactionResponse createTransaction(Long ledgerId, TransactionRequest request) {
        Category category = resolveCategory(ledgerId, request.categoryName());
        Long paidBy = request.paidByUserId() != null ? request.paidByUserId() : 1L;

        TransactionRecord record = TransactionRecord.builder()
                .ledgerId(ledgerId)
                .transactionType(request.transactionType())
                .category(category)
                .amount(request.amount())
                .memo(request.memo())
                .note(request.note())
                .transactionDate(request.transactionDate())
                .fromAssetId(request.fromAssetId())
                .toAssetId(request.toAssetId())
                .paidByUserId(paidBy)
                .createdByUserId(1L) // TODO: replace with JWT claim
                .build();

        return toResponseSingle(repository.save(record));
    }

    @Transactional
    public TransactionResponse updateTransaction(Long ledgerId, Long transactionId, TransactionRequest request) {
        TransactionRecord record = repository.findByTransactionIdAndLedgerId(transactionId, ledgerId)
                .orElseThrow(() -> new NoSuchElementException("Transaction not found"));
        Category category = resolveCategory(ledgerId, request.categoryName());
        Long paidBy = request.paidByUserId() != null ? request.paidByUserId() : record.getPaidByUserId();

        record.update(request.transactionType(), category, request.amount(), request.memo(),
                request.transactionDate(), request.fromAssetId(), request.toAssetId(), paidBy, request.note());
        return toResponseSingle(record);
    }

    @Transactional
    public void deleteTransaction(Long ledgerId, Long transactionId) {
        TransactionRecord record = repository.findByTransactionIdAndLedgerId(transactionId, ledgerId)
                .orElseThrow(() -> new NoSuchElementException("Transaction not found"));
        repository.delete(record);
    }

    private Category resolveCategory(Long ledgerId, String categoryName) {
        if (categoryName == null) return null;
        return categoryRepository.findByLedgerIdAndCategoryName(ledgerId, categoryName).orElse(null);
    }

    private TransactionResponse toResponseSingle(TransactionRecord t) {
        String nickname = null;
        if (t.getPaidByUserId() != null) {
            nickname = userRepository.findById(t.getPaidByUserId())
                    .map(User::getNickname)
                    .orElse(null);
        }
        return buildResponse(t, nickname);
    }

    private TransactionResponse toResponse(TransactionRecord t, Map<Long, String> userMap) {
        String nickname = t.getPaidByUserId() != null
                ? userMap.get(t.getPaidByUserId())
                : null;
        return buildResponse(t, nickname);
    }

    private TransactionResponse buildResponse(TransactionRecord t, String nickname) {
        return new TransactionResponse(
                t.getTransactionId(),
                t.getMemo(),
                t.getAmount(),
                t.getTransactionType(),
                t.getTransactionDate(),
                t.getCategory() != null ? t.getCategory().getCategoryName() : null,
                t.getPaidByUserId(),
                nickname,
                t.getNote()
        );
    }
}
