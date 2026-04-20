package com.monew.dto.response;


import java.util.UUID;

public record CommentResponse(
    UUID id,
    UUID articleId,
    UUID userId,
    String userNickname,
    String content,
    long likeCount,
    boolean likedByMe,
    java.time.Instant createdAt
) {}
