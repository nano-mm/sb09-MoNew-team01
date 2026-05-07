package com.monew.dto.request;

import com.monew.domain.model.enums.ResourceType;
import java.util.UUID;

public record NotificationCreateCommand(
    UUID userId,
    String content,
    ResourceType resourceType,
    UUID resourceId
) {
}


