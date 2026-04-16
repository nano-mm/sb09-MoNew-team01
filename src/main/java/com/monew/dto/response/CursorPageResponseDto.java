package com.monew.dto.response;

import java.time.Instant;
import java.util.List;
import lombok.Builder;

@Builder
public record CursorPageResponseDto<T>(
    List<T> content,
    String nextCursor,
    Instant nextAfter,
    Integer size,
    Long totalElements,
    Boolean hasNext
) {
}