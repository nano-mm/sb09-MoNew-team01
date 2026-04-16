package com.monew.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.monew.dto.response.NotificationDto;
import com.monew.entity.Notification;
import com.monew.entity.enums.ResourceType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
class NotificationMapperTest {

  @Autowired
  private NotificationMapper notificationMapper;

  @Test
  void notificationToNotificationDto_mapping() {
    Notification notification = Notification.builder()
        .userId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
        .content("[경제]와 관련된 기사가 7건 등록되었습니다.")
        .resourceType(ResourceType.INTEREST)
        .resourceId(UUID.fromString("550e8400-e29b-41d4-a716-446655440012"))
        .confirmed(false)
        .build();

    Instant createdAt = Instant.parse("2026-04-16T01:25:08.156719Z");
    Instant updatedAt = Instant.parse("2026-04-16T01:25:08.156719Z");
    ReflectionTestUtils.setField(notification, "createdAt", createdAt);
    ReflectionTestUtils.setField(notification, "updatedAt", updatedAt);

    NotificationDto dto = notificationMapper.toDto(notification);

    assertThat(dto.id()).isNull();
    assertThat(dto.createdAt()).isEqualTo(createdAt);
    assertThat(dto.updatedAt()).isEqualTo(updatedAt);
    assertThat(dto.confirmed()).isFalse();
    assertThat(dto.userId()).isEqualTo(notification.getUserId());
    assertThat(dto.content()).isEqualTo(notification.getContent());
    assertThat(dto.resourceType()).isEqualTo("interest");
    assertThat(dto.resourceId()).isEqualTo(notification.getResourceId());
  }
}
