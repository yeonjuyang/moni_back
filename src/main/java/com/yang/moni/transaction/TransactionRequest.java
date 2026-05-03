package com.yang.moni.transaction;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record TransactionRequest(
        String transactionType,
        Long amount,
        String memo,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate transactionDate,
        String categoryName,
        Long fromAssetId,
        Long toAssetId
) {}
