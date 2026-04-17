package com.monew.dto.request;

import java.time.Instant;

public record InterestSearchRequest(
    String keyword,
    String sort,     // name or subscriberCount
    String cursor,
    Instant after,
    Integer size,
    String userId
) {

  public String getSortOrDefault() {
    return sort == null ? "name" : sort;
  }

  public int getSizeOrDefault() {
    return size == null ? 10 : size;
  }
}