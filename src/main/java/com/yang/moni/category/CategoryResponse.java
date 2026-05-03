package com.yang.moni.category;

public record CategoryResponse(
        Long categoryId,
        String categoryName,
        String categoryType,
        String iconName,
        String iconColor,
        int sortOrder
) {}
