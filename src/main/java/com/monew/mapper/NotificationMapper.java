package com.monew.mapper;

import com.monew.dto.response.NotificationDto;
import com.monew.entity.Notification;
import com.monew.entity.enums.ResourceType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NotificationMapper {

  @Mapping(target = "confirmed", source = "confirmed", qualifiedByName = "toBoolean")
  @Mapping(target = "resourceType", source = "resourceType", qualifiedByName = "resourceTypeToString")
  NotificationDto toDto(Notification notification);

  @SuppressWarnings("unused")
  @Named("toBoolean")
  default boolean toBoolean(Boolean confirmed) {
    return Boolean.TRUE.equals(confirmed);
  }

  @SuppressWarnings("unused")
  @Named("resourceTypeToString")
  default String resourceTypeToString(ResourceType resourceType) {
    return resourceType == null ? null : resourceType.name().toLowerCase();
  }
}


