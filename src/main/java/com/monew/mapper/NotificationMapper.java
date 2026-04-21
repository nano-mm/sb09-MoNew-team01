package com.monew.mapper;

import com.monew.dto.response.NotificationDto;
import com.monew.entity.Notification;
import com.monew.entity.enums.ResourceType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NotificationMapper {

  @Mapping(target = "userId", source = "user.id")
  @Mapping(target = "confirmed", expression = "java(Boolean.TRUE.equals(notification.getConfirmed()))")
  @Mapping(target = "resourceType", expression = "java(toLowerCase(notification.getResourceType()))")
  NotificationDto toDto(Notification notification);

  default String toLowerCase(ResourceType resourceType) {
    return resourceType == null ? null : resourceType.name().toLowerCase();
  }
}

