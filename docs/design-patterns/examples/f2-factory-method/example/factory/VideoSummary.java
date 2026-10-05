package example.factory;

import java.time.LocalDateTime;

/**
 * Lightweight value object representing a video in a category feed.
 */
public record VideoSummary(
        String id,
        String title,
        String categoryId,
        LocalDateTime createdAt,
        int views
) {}
