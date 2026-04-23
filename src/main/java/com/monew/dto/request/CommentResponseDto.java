package com.monew.dto.request;

import com.monew.dto.comment.CommentSortType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CommentResponseDto(
    @NotNull UUID articleId,
    @NotNull CommentSortType orderBy,
    @NotBlank String direction,
    String cursor,
    Instant after,
    @NotNull @Min(1) Integer limit
) {
  public CommentResponseDto {
    if (direction != null) {
      direction = direction.toLowerCase();

      if (!direction.equals("asc") && !direction.equals("desc")) {
        throw new IllegalArgumentException("direction은 'asc' 또는 'desc'여야 합니다.");
      }
    }

    if (limit == null) {
      limit = 50;
    }
  }
}