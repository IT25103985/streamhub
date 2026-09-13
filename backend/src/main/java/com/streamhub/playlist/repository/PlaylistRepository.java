package com.streamhub.playlist.repository;

import com.streamhub.common.entity.User;
import com.streamhub.playlist.entity.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
//repository querying playlists by owner privacy and visibility
public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {
    List<Playlist> findByOwnerOrderByCreatedAtDesc(User owner);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Playlist p WHERE (UPPER(p.privacy) = 'PUBLIC' OR p.privacy IS NULL) AND (p.isHidden = false OR p.isHidden IS NULL) ORDER BY p.createdAt DESC")
    List<Playlist> findPublicPlaylists();

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Playlist p WHERE p.owner.userId = :ownerId AND " +
           "NOT EXISTS (SELECT pv FROM PlaylistVideo pv WHERE pv.playlist = p AND pv.video.owner.userId != :ownerId)")
    List<Playlist> findByOwnerIdAndOnlyOwnVideos(@org.springframework.data.repository.query.Param("ownerId") UUID ownerId);

    //queries user playlists with optional own-videos filter and search query
    @org.springframework.data.jpa.repository.Query("SELECT p FROM Playlist p WHERE p.owner.userId = :ownerId " +
            "AND (:search IS NULL OR :search = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR (p.description IS NOT NULL AND LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))) " +
            "AND (:onlyOwnVideos = false OR NOT EXISTS (SELECT pv FROM PlaylistVideo pv WHERE pv.playlist = p AND pv.video.owner.userId != :ownerId)) " +
            "ORDER BY p.createdAt DESC")
    List<Playlist> findUserPlaylists(
            @org.springframework.data.repository.query.Param("ownerId") UUID ownerId,
            @org.springframework.data.repository.query.Param("onlyOwnVideos") boolean onlyOwnVideos,
            @org.springframework.data.repository.query.Param("search") String search
    );

    //searches public playlists by query and category filter with pagination
    @org.springframework.data.jpa.repository.Query(
            value = "SELECT p FROM Playlist p WHERE (UPPER(p.privacy) = 'PUBLIC' OR p.privacy IS NULL) AND (p.isHidden = false OR p.isHidden IS NULL) " +
                    "AND (:query IS NULL OR :query = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR (p.description IS NOT NULL AND LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))) " +
                    "AND (:categoryId IS NULL OR EXISTS (" +
                    "    SELECT pv FROM PlaylistVideo pv WHERE pv.playlist = p AND pv.video.category.id = :categoryId AND (pv.video.isHidden = false OR pv.video.isHidden IS NULL)" +
                    ")) ORDER BY p.createdAt DESC",
            countQuery = "SELECT COUNT(p) FROM Playlist p WHERE (UPPER(p.privacy) = 'PUBLIC' OR p.privacy IS NULL) AND (p.isHidden = false OR p.isHidden IS NULL) " +
                    "AND (:query IS NULL OR :query = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR (p.description IS NOT NULL AND LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))) " +
                    "AND (:categoryId IS NULL OR EXISTS (" +
                    "    SELECT pv FROM PlaylistVideo pv WHERE pv.playlist = p AND pv.video.category.id = :categoryId AND (pv.video.isHidden = false OR pv.video.isHidden IS NULL)" +
                    "))"
    )
    org.springframework.data.domain.Page<Playlist> searchPublicPlaylists(
            @org.springframework.data.repository.query.Param("query") String query,
            @org.springframework.data.repository.query.Param("categoryId") UUID categoryId,
            org.springframework.data.domain.Pageable pageable
    );
}
