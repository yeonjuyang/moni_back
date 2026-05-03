package com.yang.moni.category;

public record CategoryRequest(
        String categoryName,
        String categoryType,
        String iconName,
        String iconColor
) {}
