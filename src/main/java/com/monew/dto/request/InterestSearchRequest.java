package com.monew.dto.request;

import java.time.Instant;

public record InterestSearchRequest(
    String keyword,
    String orderBy,
    String direction,
    String cursor,
    Instant after,
    Integer size,
    String userId
) {

  public String getOrderByOrDefault() {
    return orderBy == null ? "name" : orderBy;
  }

  public String getDirectionOrDefault() { return direction == null ? "ASC" : direction;}

  public int getSizeOrDefault() {
    return size == null ? 10 : size;
  }
}