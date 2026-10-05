package com.streamhub.social.event;

import java.util.UUID;

/**
 * Domain event published when a video is liked by a user.
 * Holds immutable domain primitives and identifiers only.
 */
public record VideoLikedEvent(
        UUID videoId,
        String videoTitle,
        UUID actorUserId,
        String actorUsername,
        UUID recipientUserId
) {}
