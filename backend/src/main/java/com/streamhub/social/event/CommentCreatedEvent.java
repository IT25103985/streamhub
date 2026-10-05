package com.streamhub.social.event;

import java.util.UUID;

/**
 * Domain event published when a comment is posted on a video.
 * Holds immutable domain primitives and identifiers only.
 */
public record CommentCreatedEvent(
        UUID videoId,
        String videoTitle,
        UUID actorUserId,
        String actorUsername,
        UUID recipientUserId,
        String commentPreview
) {}
