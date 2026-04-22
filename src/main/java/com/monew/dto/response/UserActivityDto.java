package com.monew.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserActivityDto(
    UUID id,
    String email,
    String nickname,
    Instant createdAt,
    List<SubscriptionDto> subscriptions,
    List<CommentResponse> comments,
    List<CommentLikeDto> commentLikes,
    List<ArticleViewDto> articleViews
    ) {

}
