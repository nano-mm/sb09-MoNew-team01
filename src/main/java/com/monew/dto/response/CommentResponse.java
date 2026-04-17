package com.monew.dto.response;


import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponse(
    UUID commentId,
    UUID userId,
    String content,
    int likeCount,
    LocalDateTime createdAt
) {

}
