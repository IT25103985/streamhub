package com.streamhub.preferences.repository;

import com.streamhub.common.entity.User;
import com.streamhub.preferences.entity.UserCategoryPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
//repository querying and persisting user onboarding category preferences
public interface UserCategoryPreferenceRepository extends JpaRepository<UserCategoryPreference, UUID> {

    List<UserCategoryPreference> findByUser(User user);

    List<UserCategoryPreference> findByUser_UserId(UUID userId);

    @Query("SELECT ucp.category.id FROM UserCategoryPreference ucp WHERE ucp.user = :user")
    List<UUID> findCategoryIdsByUser(@Param("user") User user);

    void deleteByUser(User user);

    long countByUser(User user);
}
