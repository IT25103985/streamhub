package com.streamhub.social.event;

import java.util.UUID;

/**
 * Domain event published when a comment is liked by a user.
 * Holds immutable domain primitives and identifiers only.
 */
public record CommentLikedEvent(
        UUID commentId,
        String videoTitle,
        UUID actorUserId,
        String actorUsername,
        UUID recipientUserId
) {}
