package com.monew.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CommentActivityDto(
    UUID id,
    UUID articleId,
    String articleTitle,
    UUID userId,
    String userNickname,
    long likeCount,
    Instant createdAt
) {

}