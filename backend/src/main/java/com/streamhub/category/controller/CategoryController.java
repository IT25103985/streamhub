package com.streamhub.category.controller;

import com.streamhub.category.dto.CategoryDto;
import com.streamhub.category.dto.CreateCategoryDto;
import com.streamhub.category.dto.UpdateCategoryDto;
import com.streamhub.category.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
//manages video categories for browsing and classification
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getAllCategories(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(categoryService.getAllCategories(search));
    }

    @GetMapping("/{id}")
    //looks up single category metadata by identifier
    public ResponseEntity<CategoryDto> getCategoryById(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CreateCategoryDto dto) {
        CategoryDto createdCategory = categoryService.createCategory(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<CategoryDto> updateCategory(@PathVariable UUID id, @Valid @RequestBody UpdateCategoryDto dto) {
        CategoryDto updatedCategory = categoryService.updateCategory(id, dto);
        return ResponseEntity.ok(updatedCategory);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
