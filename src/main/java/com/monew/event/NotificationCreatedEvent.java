package com.monew.event;

import com.monew.domain.model.enums.ResourceType;
import java.util.UUID;

public record NotificationCreatedEvent(UUID userId, String content, ResourceType resourceType, UUID resourceId) {

}

