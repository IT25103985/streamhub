package com.streamhub.video.repository;

import com.streamhub.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
//repository querying videos with pagination and title search filters
public interface VideoRepository extends JpaRepository<Video, UUID> {
    @org.springframework.data.jpa.repository.Query("SELECT v FROM Video v WHERE (v.isHidden = false OR v.isHidden IS NULL) AND (UPPER(v.privacy) = 'PUBLIC' OR v.privacy IS NULL) ORDER BY v.viewCount DESC, v.createdAt ASC")
    List<Video> findPublicTrendingVideos(Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT v FROM Video v WHERE (v.isHidden = false OR v.isHidden IS NULL) AND (UPPER(v.privacy) = 'PUBLIC' OR v.privacy IS NULL) AND v.viewCount >= :minViews ORDER BY v.viewCount DESC, v.createdAt ASC")
    List<Video> findPublicTrendingVideosWithMinViews(@org.springframework.data.repository.query.Param("minViews") int minViews, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT v FROM Video v WHERE (v.isHidden = false OR v.isHidden IS NULL) AND (UPPER(v.privacy) = 'PUBLIC' OR v.privacy IS NULL) AND v.viewCount < :minViews ORDER BY v.viewCount DESC, v.createdAt ASC")
    List<Video> findPublicTrendingVideosBelowMinViews(@org.springframework.data.repository.query.Param("minViews") int minViews, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT v FROM Video v WHERE v.category.id = :categoryId AND (v.isHidden = false OR v.isHidden IS NULL) AND (UPPER(v.privacy) = 'PUBLIC' OR v.privacy IS NULL) ORDER BY v.viewCount DESC, v.createdAt ASC")
    List<Video> findPublicVideosByCategoryOrderByViewCountDesc(@org.springframework.data.repository.query.Param("categoryId") UUID categoryId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT v FROM Video v WHERE v.category.id = :categoryId AND (v.isHidden = false OR v.isHidden IS NULL) AND (UPPER(v.privacy) = 'PUBLIC' OR v.privacy IS NULL) AND v.viewCount >= :minViews ORDER BY v.viewCount DESC, v.createdAt ASC")
    List<Video> findPublicVideosByCategoryWithMinViews(@org.springframework.data.repository.query.Param("categoryId") UUID categoryId, @org.springframework.data.repository.query.Param("minViews") int minViews, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT v FROM Video v WHERE v.category.id = :categoryId AND (v.isHidden = false OR v.isHidden IS NULL) AND (UPPER(v.privacy) = 'PUBLIC' OR v.privacy IS NULL) AND v.viewCount < :minViews ORDER BY v.viewCount DESC, v.createdAt ASC")
    List<Video> findPublicVideosByCategoryBelowMinViews(@org.springframework.data.repository.query.Param("categoryId") UUID categoryId, @org.springframework.data.repository.query.Param("minViews") int minViews, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT v FROM Video v WHERE v.category.id IN :categoryIds AND (v.isHidden = false OR v.isHidden IS NULL) AND (UPPER(v.privacy) = 'PUBLIC' OR v.privacy IS NULL) AND v.viewCount >= :minViews ORDER BY v.viewCount DESC, v.createdAt ASC")
    List<Video> findPublicVideosByCategoriesWithMinViews(@org.springframework.data.repository.query.Param("categoryIds") java.util.Collection<UUID> categoryIds, @org.springframework.data.repository.query.Param("minViews") int minViews, Pageable pageable);

    List<Video> findByCategory_IdOrderByViewCountDesc(UUID categoryId, Pageable pageable);
    List<Video> findByOrderByViewCountDesc(Pageable pageable);
    List<Video> findByCategory_IdAndIsHiddenFalseOrderByViewCountDesc(UUID categoryId, Pageable pageable);
    List<Video> findByIsHiddenFalseOrderByViewCountDesc(Pageable pageable);

    long countByOwner_UserId(UUID ownerId);
    long countByOwner_UserIdAndIsHiddenFalse(UUID ownerId);

    org.springframework.data.domain.Page<Video> findByOwner_UserId(UUID ownerId, Pageable pageable);
    org.springframework.data.domain.Page<Video> findByOwner_UserIdAndIsHiddenFalse(UUID ownerId, Pageable pageable);
    org.springframework.data.domain.Page<Video> findByOwner_UserIdAndTitleContainingIgnoreCase(UUID ownerId, String title, Pageable pageable);
    org.springframework.data.domain.Page<Video> findByOwner_UserIdAndTitleContainingIgnoreCaseAndIsHiddenFalse(UUID ownerId, String title, Pageable pageable);
    
    org.springframework.data.domain.Page<Video> findByIsHiddenFalse(Pageable pageable);
    org.springframework.data.domain.Page<Video> findByCategory_Id(UUID categoryId, Pageable pageable);
    org.springframework.data.domain.Page<Video> findByCategory_IdAndIsHiddenFalse(UUID categoryId, Pageable pageable);
    org.springframework.data.domain.Page<Video> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    org.springframework.data.domain.Page<Video> findByTitleContainingIgnoreCaseAndIsHiddenFalse(String title, Pageable pageable);
    org.springframework.data.domain.Page<Video> findByOwner_UserIdAndCategory_Id(UUID ownerId, UUID categoryId, Pageable pageable);
    org.springframework.data.domain.Page<Video> findByOwner_UserIdAndCategory_IdAndIsHiddenFalse(UUID ownerId, UUID categoryId, Pageable pageable);
    org.springframework.data.domain.Page<Video> findByCategory_IdAndTitleContainingIgnoreCase(UUID categoryId, String title, Pageable pageable);
    org.springframework.data.domain.Page<Video> findByCategory_IdAndTitleContainingIgnoreCaseAndIsHiddenFalse(UUID categoryId, String title, Pageable pageable);
    org.springframework.data.domain.Page<Video> findByOwner_UserIdAndCategory_IdAndTitleContainingIgnoreCase(UUID ownerId, UUID categoryId, String title, Pageable pageable);
    org.springframework.data.domain.Page<Video> findByOwner_UserIdAndCategory_IdAndTitleContainingIgnoreCaseAndIsHiddenFalse(UUID ownerId, UUID categoryId, String title, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT v FROM Video v JOIN Follow f ON v.owner.userId = f.following.userId WHERE f.follower.userId = :followerId AND (v.isHidden = false OR v.isHidden IS NULL) AND (UPPER(v.privacy) = 'PUBLIC' OR v.privacy IS NULL) ORDER BY v.createdAt DESC")
    org.springframework.data.domain.Page<Video> findVideosFromFollowedUsers(@org.springframework.data.repository.query.Param("followerId") UUID followerId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT v FROM Video v WHERE (v.isHidden = false OR v.isHidden IS NULL) AND (UPPER(v.privacy) = 'PUBLIC' OR v.privacy IS NULL)")
    org.springframework.data.domain.Page<Video> findPublicVideos(Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT v FROM Video v WHERE v.category.id = :categoryId AND (v.isHidden = false OR v.isHidden IS NULL) AND (UPPER(v.privacy) = 'PUBLIC' OR v.privacy IS NULL)")
    org.springframework.data.domain.Page<Video> findPublicVideosByCategory(@org.springframework.data.repository.query.Param("categoryId") UUID categoryId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT v FROM Video v WHERE LOWER(v.title) LIKE LOWER(CONCAT('%', :title, '%')) AND (v.isHidden = false OR v.isHidden IS NULL) AND (UPPER(v.privacy) = 'PUBLIC' OR v.privacy IS NULL)")
    org.springframework.data.domain.Page<Video> findPublicVideosByTitle(@org.springframework.data.repository.query.Param("title") String title, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT v FROM Video v WHERE v.category.id = :categoryId AND LOWER(v.title) LIKE LOWER(CONCAT('%', :title, '%')) AND (v.isHidden = false OR v.isHidden IS NULL) AND (UPPER(v.privacy) = 'PUBLIC' OR v.privacy IS NULL)")
    org.springframework.data.domain.Page<Video> findPublicVideosByCategoryAndTitle(@org.springframework.data.repository.query.Param("categoryId") UUID categoryId, @org.springframework.data.repository.query.Param("title") String title, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT v FROM Video v WHERE v.owner.userId = :ownerId AND (v.isHidden = false OR v.isHidden IS NULL) AND (UPPER(v.privacy) = 'PUBLIC' OR v.privacy IS NULL)")
    org.springframework.data.domain.Page<Video> findPublicVideosByOwner(@org.springframework.data.repository.query.Param("ownerId") UUID ownerId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT v FROM Video v WHERE v.owner.userId = :ownerId AND v.category.id = :categoryId AND (v.isHidden = false OR v.isHidden IS NULL) AND (UPPER(v.privacy) = 'PUBLIC' OR v.privacy IS NULL)")
    org.springframework.data.domain.Page<Video> findPublicVideosByOwnerAndCategory(@org.springframework.data.repository.query.Param("ownerId") UUID ownerId, @org.springframework.data.repository.query.Param("categoryId") UUID categoryId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT v FROM Video v WHERE v.owner.userId = :ownerId AND LOWER(v.title) LIKE LOWER(CONCAT('%', :title, '%')) AND (v.isHidden = false OR v.isHidden IS NULL) AND (UPPER(v.privacy) = 'PUBLIC' OR v.privacy IS NULL)")
    org.springframework.data.domain.Page<Video> findPublicVideosByOwnerAndTitle(@org.springframework.data.repository.query.Param("ownerId") UUID ownerId, @org.springframework.data.repository.query.Param("title") String title, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT v FROM Video v WHERE v.owner.userId = :ownerId AND v.category.id = :categoryId AND LOWER(v.title) LIKE LOWER(CONCAT('%', :title, '%')) AND (v.isHidden = false OR v.isHidden IS NULL) AND (UPPER(v.privacy) = 'PUBLIC' OR v.privacy IS NULL)")
    org.springframework.data.domain.Page<Video> findPublicVideosByOwnerAndCategoryAndTitle(@org.springframework.data.repository.query.Param("ownerId") UUID ownerId, @org.springframework.data.repository.query.Param("categoryId") UUID categoryId, @org.springframework.data.repository.query.Param("title") String title, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Video v SET v.category = null WHERE v.category.id = :categoryId")
    void setCategoryNullByCategoryId(@org.springframework.data.repository.query.Param("categoryId") UUID categoryId);
}
