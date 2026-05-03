package com.yang.moni.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByLedgerIdAndCategoryName(Long ledgerId, String categoryName);
    List<Category> findByLedgerIdAndActiveTrueOrderBySortOrderAsc(Long ledgerId);
    Optional<Category> findByCategoryIdAndLedgerIdAndActiveTrue(Long categoryId, Long ledgerId);
    long countByLedgerIdAndActiveTrue(Long ledgerId);
}
