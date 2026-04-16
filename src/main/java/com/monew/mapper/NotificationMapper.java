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

  @Mapping(target = "confirmed", expression = "java(Boolean.TRUE.equals(notification.getConfirmed()))")
  @Mapping(target = "resourceType", source = "resourceType", qualifiedByName = "resourceTypeToString")
  NotificationDto toDto(Notification notification);

  @Named("resourceTypeToString")
  default String resourceTypeToString(ResourceType resourceType) {
    return resourceType == null ? null : resourceType.name().toLowerCase();
  }
}

