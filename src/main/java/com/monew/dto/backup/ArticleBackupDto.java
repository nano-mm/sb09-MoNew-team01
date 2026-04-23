package com.monew.dto.backup;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder
public record ArticleBackupDto(
    String title,
    String summary,
    String sourceUrl,
    String source,
    Instant publishDate,
    Boolean isDeleted,
    Map<String, List<String>> interestKeywords
) {
}
