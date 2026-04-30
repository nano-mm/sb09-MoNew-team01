package com.monew.unit.listener;

import com.monew.event.NotificationCreatedEvent;
import com.monew.entity.User;
import com.monew.entity.enums.ResourceType;
import com.monew.listener.NotificationEventListener;
import com.monew.repository.NotificationRepository;
import com.monew.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationEventListenerTest {

  @Mock
  NotificationRepository notificationRepository;

  @Mock
  UserRepository userRepository;

  @InjectMocks
  NotificationEventListener listener;

  private UUID userId;
  private User user;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    userId = UUID.randomUUID();
    user = User.of("u@test", "u", "pwd");

    org.springframework.test.util.ReflectionTestUtils.setField(user, "id", userId);
  }

  @Test
  void handleNotificationCreated_savesNotification_whenUserExists() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    NotificationCreatedEvent evt = new NotificationCreatedEvent(userId, "hello", ResourceType.INTEREST, UUID.randomUUID());

    listener.handleNotificationCreated(evt);

    ArgumentCaptor<com.monew.entity.Notification> captor = ArgumentCaptor.forClass(com.monew.entity.Notification.class);
    verify(notificationRepository).save(captor.capture());
    com.monew.entity.Notification saved = captor.getValue();
    assertThat(saved.getUser().getId()).isEqualTo(userId);
    assertThat(saved.getContent()).isEqualTo("hello");
    assertThat(saved.getResourceType()).isEqualTo(ResourceType.INTEREST);
  }

  @Test
  void handleNotificationCreated_noSave_whenUserMissing() {
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    NotificationCreatedEvent evt = new NotificationCreatedEvent(userId, "hello", ResourceType.INTEREST, UUID.randomUUID());

    listener.handleNotificationCreated(evt);

    org.mockito.Mockito.verify(notificationRepository, org.mockito.Mockito.never()).save(org.mockito.Mockito.any());
  }
}

