package com.monew.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record UserActivityDto(
    UserDto user,
    List<InterestDto> subscribedInterests,
    List<CommentResponse> recentComments,
    List<CommentResponse> recentLikedComments,
    List<ArticleDto> recentViewedArticles
) {
}
