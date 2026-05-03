package com.yang.moni.category;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/ledgers/{ledgerId}/categories")
    public List<CategoryResponse> getCategories(@PathVariable Long ledgerId) {
        return categoryService.getByLedgerId(ledgerId);
    }

    @PostMapping("/ledgers/{ledgerId}/categories")
    public ResponseEntity<CategoryResponse> createCategory(
            @PathVariable Long ledgerId,
            @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.createCategory(ledgerId, request));
    }

    @PutMapping("/ledgers/{ledgerId}/categories/{categoryId}")
    public CategoryResponse updateCategory(
            @PathVariable Long ledgerId,
            @PathVariable Long categoryId,
            @RequestBody CategoryRequest request) {
        return categoryService.updateCategory(ledgerId, categoryId, request);
    }

    @DeleteMapping("/ledgers/{ledgerId}/categories/{categoryId}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long ledgerId,
            @PathVariable Long categoryId) {
        categoryService.deleteCategory(ledgerId, categoryId);
        return ResponseEntity.noContent().build();
    }
}
