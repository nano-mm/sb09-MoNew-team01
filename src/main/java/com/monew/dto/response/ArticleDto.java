package com.monew.dto.response;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ArticleDto(
    UUID id,
    String source,
    String sourceUrl,
    String title,
    Instant publishDate,
    String summary,
    Long commentCount,
    Long viewCount,
    Boolean viewedByMe
) {
}