package com.streamhub.social.event;

import java.util.UUID;

/**
 * Domain event published when a reply is posted to an existing comment.
 * Holds immutable domain primitives and identifiers only.
 */
public record CommentRepliedEvent(
        UUID videoId,
        String videoTitle,
        UUID actorUserId,
        String actorUsername,
        UUID recipientUserId,
        String commentPreview
) {}
