package com.monew.dto.request;

import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

public record CursorRequest(
    String cursor,
    Instant after,
    @Min(1)
    Integer limit,
    String orderBy,
    String direction
) {
}
