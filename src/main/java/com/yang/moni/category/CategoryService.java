package com.yang.moni.category;

import com.yang.moni.ledger.LedgerAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final LedgerAccessGuard accessGuard;

    public List<CategoryResponse> getByLedgerId(Long ledgerId) {
        accessGuard.requireMember(ledgerId);
        return categoryRepository.findByLedgerIdAndActiveTrueOrderBySortOrderAsc(ledgerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(Long ledgerId, CategoryRequest request) {
        accessGuard.requireMember(ledgerId);
        int nextOrder = (int) categoryRepository.countByLedgerIdAndActiveTrue(ledgerId) + 1;

        Category category = Category.builder()
                .ledgerId(ledgerId)
                .categoryName(request.categoryName())
                .categoryType(request.categoryType())
                .iconName(request.iconName())
                .iconColor(request.iconColor())
                .sortOrder(nextOrder)
                .build();

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(Long ledgerId, Long categoryId, CategoryRequest request) {
        accessGuard.requireMember(ledgerId);
        Category category = categoryRepository
                .findByCategoryIdAndLedgerIdAndActiveTrue(categoryId, ledgerId)
                .orElseThrow(() -> new NoSuchElementException("Category not found"));

        category.update(request.categoryName(), request.iconName(), request.iconColor());
        return toResponse(category);
    }

    @Transactional
    public void deleteCategory(Long ledgerId, Long categoryId) {
        accessGuard.requireMember(ledgerId);
        Category category = categoryRepository
                .findByCategoryIdAndLedgerIdAndActiveTrue(categoryId, ledgerId)
                .orElseThrow(() -> new NoSuchElementException("Category not found"));

        category.deactivate();
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(
                c.getCategoryId(),
                c.getCategoryName(),
                c.getCategoryType(),
                c.getIconName(),
                c.getIconColor(),
                c.getSortOrder()
        );
    }
}
