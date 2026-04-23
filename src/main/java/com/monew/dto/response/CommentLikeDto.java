package com.monew.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CommentLikeDto(
    UUID id,
    Instant createdAt,
    UUID commentId,
    UUID articleId,
    String articleTitle,
    UUID commentUserId,
    String commentUserNickname,
    String commentContent,
    Long commentLikeCount,
    Instant commentCreatedAt
) {

}
