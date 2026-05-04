package com.monew.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;

public record InterestSearchRequest(
    String keyword,
    CursorRequest cursorRequest,
    UUID userId
) {


  public String getOrderByOrDefault() {
    return cursorRequest.orderBy() == null ? "name" : cursorRequest.orderBy();
  }

  public String getDirectionOrDefault() { return cursorRequest.direction() == null ? "ASC" : cursorRequest.direction();}

  public int getSizeOrDefault() {
    return cursorRequest.limit() == null ? 10 : cursorRequest.limit();
  }
}