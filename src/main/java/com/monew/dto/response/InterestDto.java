package com.monew.dto.response;

import java.util.List;
import java.util.UUID;


public record InterestDto(
    UUID id,
    String name,
    List<String> keywords,
    Long subscriberCount,
    boolean subscribedByMe
) {

}