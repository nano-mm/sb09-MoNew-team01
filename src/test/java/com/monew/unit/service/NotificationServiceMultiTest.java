package com.monew.unit.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monew.dto.request.NotificationCreateCommand;
import com.monew.entity.User;
import com.monew.entity.enums.ResourceType;
import com.monew.service.NotificationService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

class NotificationServiceMultiTest {

  ApplicationEventPublisher eventPublisher;
  java.util.List<Object> publishedEvents = new java.util.ArrayList<>();

  @Mock
  com.monew.repository.UserRepository userRepository;

  @Mock
  com.monew.repository.NotificationRepository notificationRepository;

  @Mock
  com.monew.mapper.NotificationMapper notificationMapper;

  NotificationService notificationService;

  private UUID userId;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    userId = UUID.randomUUID();
    User user = User.of("m@test", "m", "p");
    org.springframework.test.util.ReflectionTestUtils.setField(user, "id", userId);
    when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
    // create a simple event publisher that records published events
    eventPublisher = new ApplicationEventPublisher() {
      @Override
      public void publishEvent(Object event) {
        publishedEvents.add(event);
      }

      @Override
      public void publishEvent(org.springframework.context.ApplicationEvent event) {
        publishedEvents.add(event);
      }
    };

    // instantiate service with recording publisher
    notificationService = new NotificationService(notificationRepository, userRepository, notificationMapper, eventPublisher);
  }

  @Test
  void createNotifications_publishesEvents_forEachCommand() {
    NotificationCreateCommand c1 = new NotificationCreateCommand(userId, "a", ResourceType.INTEREST, UUID.randomUUID());
    NotificationCreateCommand c2 = new NotificationCreateCommand(userId, "b", ResourceType.INTEREST, UUID.randomUUID());

    // sanity check: ensure service.eventPublisher is the same instance
    Object injected = org.springframework.test.util.ReflectionTestUtils.getField(notificationService, "eventPublisher");
    org.junit.jupiter.api.Assertions.assertSame(eventPublisher, injected);

    // invoke createNotification twice to ensure events are published
    notificationService.createNotification(userId, "a", ResourceType.INTEREST, c1.resourceId());
    notificationService.createNotification(userId, "b", ResourceType.INTEREST, c2.resourceId());

    // expect two events published
    org.junit.jupiter.api.Assertions.assertEquals(2, publishedEvents.size());
  }
}









