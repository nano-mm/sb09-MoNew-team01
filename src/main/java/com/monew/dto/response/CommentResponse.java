package com.monew.dto.response;


import java.util.UUID;
import java.time.Instant;

public record CommentResponse(
    UUID id,
    UUID articleId,
    UUID userId,
    String userNickname,
    String content,
    long likeCount,
    boolean likedByMe,
    Instant createdAt
) {}
