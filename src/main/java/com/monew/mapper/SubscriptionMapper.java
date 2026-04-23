package com.monew.mapper;

import com.monew.dto.response.SubscriptionDto;
import com.monew.entity.Subscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SubscriptionMapper {

  @Mapping(source = "id", target = "id")
  @Mapping(source = "interest.id", target = "interestId")
  @Mapping(source = "interest.name", target = "interestName")
  @Mapping(source = "interest.keywords", target = "interestKeywords")
  @Mapping(source = "interest.subscriberCount", target = "interestSubscriberCount")
  @Mapping(source = "createdAt", target = "createdAt")
  SubscriptionDto toDto(Subscription subscription);
}
