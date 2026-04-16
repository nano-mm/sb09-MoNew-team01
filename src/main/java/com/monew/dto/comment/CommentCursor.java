package com.monew.dto.comment;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentCursor(
    UUID lastId,
    LocalDateTime lastCreatedAt,
    int lastLikeCount
) {}