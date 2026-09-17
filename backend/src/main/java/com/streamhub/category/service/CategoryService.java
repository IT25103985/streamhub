package com.streamhub.category.service;

import com.streamhub.category.dto.CategoryDto;
import com.streamhub.category.dto.CreateCategoryDto;
import com.streamhub.category.dto.UpdateCategoryDto;
import com.streamhub.category.entity.Category;
import com.streamhub.category.repository.CategoryRepository;
import com.streamhub.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
//business logic for creating editing and fetching video categories
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final com.streamhub.video.repository.VideoRepository videoRepository;

    public CategoryService(CategoryRepository categoryRepository, com.streamhub.video.repository.VideoRepository videoRepository) {
        this.categoryRepository = categoryRepository;
        this.videoRepository = videoRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories(String search) {
        List<Category> categories;
        if (search != null && !search.trim().isEmpty()) {
            categories = categoryRepository.findByNameContainingIgnoreCase(search.trim());
        } else {
            categories = categoryRepository.findAll();
        }
        
        return categories.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return mapToDto(category);
    }

    @Transactional
    //creates new category and ensures unique name
    public CategoryDto createCategory(CreateCategoryDto dto) {
        if (categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("Category with this name already exists");
        }

        Category category = new Category(dto.getName(), dto.getDescription());
        Category savedCategory = categoryRepository.save(category);
        return mapToDto(savedCategory);
    }

    @Transactional
    public CategoryDto updateCategory(UUID id, UpdateCategoryDto dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (!category.getName().equalsIgnoreCase(dto.getName()) && 
            categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("Category with this name already exists");
        }

        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        
        Category updatedCategory = categoryRepository.save(category);
        return mapToDto(updatedCategory);
    }

    @Transactional
    public void deleteCategory(UUID id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        videoRepository.setCategoryNullByCategoryId(id);
        categoryRepository.deleteById(id);
    }

    private CategoryDto mapToDto(Category category) {
        return new CategoryDto(category.getId(), category.getName(), category.getDescription());
    }

    @Transactional(readOnly = true)
    public long countAll() {
        return categoryRepository.count();
    }
}
