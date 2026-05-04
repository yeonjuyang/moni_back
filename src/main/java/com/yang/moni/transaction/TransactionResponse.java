package com.yang.moni.transaction;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record TransactionResponse(
        Long id,
        String memo,
        Long amount,
        String transactionType,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate transactionDate,
        String categoryName,
        Long paidByUserId,
        String paidByUserNickname,
        String note
) {}
