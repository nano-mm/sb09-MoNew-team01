package com.monew.unit.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.monew.dto.response.NotificationDto;
import com.monew.entity.Notification;
import com.monew.entity.User;
import com.monew.entity.enums.ResourceType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

class NotificationMapperTest {

  private final com.monew.mapper.NotificationMapper notificationMapper = Mappers.getMapper(com.monew.mapper.NotificationMapper.class);

  @Test
  void toDto_알림엔티티를_명세형식으로_매핑한다() {
    UUID userId = UUID.randomUUID();
    UUID notificationId = UUID.randomUUID();
    UUID resourceId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2026-04-19T11:00:00Z");
    Instant updatedAt = Instant.parse("2026-04-19T11:05:00Z");

    User user = User.of("test@monew.com", "tester", "password");
    ReflectionTestUtils.setField(user, "id", userId);

    Notification notification = Notification.of(user, "알림 내용", ResourceType.INTEREST, resourceId);
    notification.confirm();
    ReflectionTestUtils.setField(notification, "id", notificationId);
    ReflectionTestUtils.setField(notification, "createdAt", createdAt);
    ReflectionTestUtils.setField(notification, "updatedAt", updatedAt);

    NotificationDto dto = notificationMapper.toDto(notification);

    assertThat(dto.id()).isEqualTo(notificationId);
    assertThat(dto.userId()).isEqualTo(userId);
    assertThat(dto.content()).isEqualTo("알림 내용");
    assertThat(dto.resourceType()).isEqualTo("interest");
    assertThat(dto.resourceId()).isEqualTo(resourceId);
    assertThat(dto.confirmed()).isTrue();
    assertThat(dto.createdAt()).isEqualTo(createdAt);
    assertThat(dto.updatedAt()).isEqualTo(updatedAt);
  }
}

