package com.streamhub.category.repository;

import com.streamhub.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
//repository for retrieving categories and checking duplicate names
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    List<Category> findByNameContainingIgnoreCase(String name);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT c FROM Category c JOIN Video v ON v.category.id = c.id WHERE (v.isHidden = false OR v.isHidden IS NULL) AND (UPPER(v.privacy) = 'PUBLIC' OR v.privacy IS NULL)")
    List<Category> findCategoriesWithPublicVideos();

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT c FROM Category c JOIN Video v ON v.category.id = c.id WHERE (v.isHidden = false OR v.isHidden IS NULL) AND (UPPER(v.privacy) = 'PUBLIC' OR v.privacy IS NULL) AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Category> findCategoriesWithPublicVideosAndSearch(@org.springframework.data.repository.query.Param("search") String search);
}
