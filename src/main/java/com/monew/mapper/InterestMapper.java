package com.monew.mapper;

import com.monew.dto.response.InterestDto;
import com.monew.domain.model.Interest;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InterestMapper {
  public static InterestDto toDto(Interest interest, boolean subscribed) {
    return new InterestDto(
        interest.getId(),
        interest.getName(),
        interest.getKeywords(),
        interest.getSubscriberCount(),
        subscribed
    );
  }
}
