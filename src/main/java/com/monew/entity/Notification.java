package com.monew.entity;

import com.monew.entity.base.BaseUpdatableEntity;
import com.monew.entity.enums.ResourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "notifications")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Notification extends BaseUpdatableEntity {

  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "content")
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(name = "resourceType")
  private ResourceType resourceType;

  @Column(name = "resource_id")
  private UUID resourceId;

  @Column(name = "confirmed")
  private Boolean confirmed;

  public static Notification of(
      UUID userId,
      String content,
      ResourceType resourceType,
      UUID resourceId
  ) {
    return Notification.builder()
        .userId(userId)
        .content(content)
        .resourceType(resourceType)
        .resourceId(resourceId)
        .confirmed(false)
        .build();
  }

  public void confirm() {
    this.confirmed = true;
  }
}
