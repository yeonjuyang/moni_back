package com.yang.moni.transaction;

import com.yang.moni.category.Category;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class TransactionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "ledger_id", nullable = false)
    private Long ledgerId;

    @Column(name = "transaction_type", nullable = false)
    private String transactionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "memo")
    private String memo;

    @Column(name = "note")
    private String note;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "from_asset_id")
    private Long fromAssetId;

    @Column(name = "to_asset_id")
    private Long toAssetId;

    @Column(name = "paid_by_user_id")
    private Long paidByUserId;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void update(String transactionType, Category category, Long amount, String memo,
                       LocalDate transactionDate, Long fromAssetId, Long toAssetId,
                       Long paidByUserId, String note) {
        this.transactionType = transactionType;
        this.category = category;
        this.amount = amount;
        this.memo = memo;
        this.transactionDate = transactionDate;
        this.fromAssetId = fromAssetId;
        this.toAssetId = toAssetId;
        this.paidByUserId = paidByUserId;
        this.note = note;
    }
}
