package com.monew.dto.backup;

import java.time.Instant;
import lombok.Builder;
import java.util.Set;

@Builder
public record ArticleBackupDto(
    String title,
    String summary,
    String sourceUrl,
    String source,
    Instant publishDate,
    Boolean isDeleted,
    Set<String> interestNames
) {
}
