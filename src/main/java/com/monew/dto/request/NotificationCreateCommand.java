package com.monew.dto.request;

import com.monew.entity.enums.ResourceType;
import java.util.UUID;

public record NotificationCreateCommand(
    UUID userId,
    String content,
    ResourceType resourceType,
    UUID resourceId
) {
}


