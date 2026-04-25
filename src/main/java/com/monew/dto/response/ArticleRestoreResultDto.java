package com.monew.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ArticleRestoreResultDto(
        Instant restoreDate,
        List<UUID> restoreArticlesIds,
        Long restoredArticleCount
    )
{

}
