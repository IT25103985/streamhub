package com.streamhub.playlist.event;

import java.util.UUID;

/**
 * Domain event published when a video is successfully added to a playlist.
 * Carries only immutable primitive/String/UUID identifiers to decouple listener from JPA entities.
 */
public record PlaylistVideoAddedEvent(
        UUID playlistId,
        String playlistName,
        UUID videoId,
        String videoTitle,
        UUID actorUserId,
        String actorUsername,
        UUID recipientUserId
) {}
