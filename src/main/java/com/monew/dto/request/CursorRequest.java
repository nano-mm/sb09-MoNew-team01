package com.monew.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

public record CursorRequest(
    String cursor,
    Instant after,
    @NotNull
    Integer limit,
    @NotNull
    String orderBy,
    @NotNull
    String direction
) {
}
