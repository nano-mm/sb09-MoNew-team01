package com.monew.dto.comment;

import java.time.Instant;
import java.util.UUID;

public record CommentCursor(
    UUID lastId,
    Instant lastCreatedAt,
    int lastLikeCount
) {}