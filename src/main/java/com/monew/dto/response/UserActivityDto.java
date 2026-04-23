package com.monew.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record UserActivityDto(
    UserDto user,
    List<InterestDto> subscriptions,
    List<CommentResponse> comments,
    List<CommentResponse> commentLikes,
    List<ArticleViewDto> articleViews
) {
}
