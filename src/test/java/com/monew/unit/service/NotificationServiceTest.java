package com.monew.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monew.dto.response.CursorPageResponseDto;
import com.monew.dto.response.NotificationDto;
import com.monew.dto.request.NotificationCreateCommand;
import com.monew.entity.Notification;
import com.monew.entity.User;
import com.monew.entity.enums.ResourceType;
import com.monew.exception.BaseException;
import com.monew.exception.ErrorCode;
import com.monew.mapper.NotificationMapper;
import com.monew.repository.NotificationRepository;
import com.monew.repository.UserRepository;
import com.monew.service.NotificationService;
import com.monew.event.NotificationCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

  @Mock
  private NotificationRepository notificationRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private NotificationMapper notificationMapper;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private NotificationService notificationService;

  private UUID userId;
  private User user;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    user = User.of("test@monew.com", "tester", "password");
    ReflectionTestUtils.setField(user, "id", userId);
  }

  @Test
  void getNotifications_returnsPagedResponse_whenHasNext() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    Notification first = Notification.of(user, "a", ResourceType.INTEREST, UUID.randomUUID());
    Notification second = Notification.of(user, "b", ResourceType.INTEREST, UUID.randomUUID());
    Notification extra = Notification.of(user, "c", ResourceType.INTEREST, UUID.randomUUID());

    ReflectionTestUtils.setField(first, "id", UUID.fromString("00000000-0000-0000-0000-000000000001"));
    ReflectionTestUtils.setField(second, "id", UUID.fromString("00000000-0000-0000-0000-000000000002"));

    when(notificationRepository.findByUserIdWithCursor(userId, null, 3))
        .thenReturn(List.of(first, second, extra));
    when(notificationRepository.countByUser_IdAndConfirmedFalse(userId)).thenReturn(5L);

    NotificationDto firstDto = new NotificationDto(first.getId(), Instant.now(), Instant.now(), false, userId, "a", "interest", UUID.randomUUID());
    NotificationDto secondDto = new NotificationDto(second.getId(), Instant.now(), Instant.now(), false, userId, "b", "interest", UUID.randomUUID());
    when(notificationMapper.toDto(first)).thenReturn(firstDto);
    when(notificationMapper.toDto(second)).thenReturn(secondDto);

    CursorPageResponseDto<NotificationDto> response = notificationService.getNotifications(userId, null, null, 2);

    assertThat(response.content()).containsExactly(firstDto, secondDto);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.totalElements()).isEqualTo(5L);
  }

  @Test
  void getNotifications_throwsInvalidInput_forBadCursor() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> notificationService.getNotifications(userId, "not-uuid", null, 10))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_INPUT);
  }

  @Test
  void getNotifications_throwsUserNotFound_whenUserMissing() {
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> notificationService.getNotifications(userId, null, null, 10))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.USER_NOT_FOUND);
  }

  @Test
  void confirmNotification_confirms_whenExists() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    UUID notificationId = UUID.randomUUID();
    Notification notification = Notification.of(user, "x", ResourceType.INTEREST, UUID.randomUUID());
    when(notificationRepository.findByIdAndUser_IdAndConfirmedFalse(notificationId, userId))
        .thenReturn(Optional.of(notification));

    notificationService.confirmNotification(userId, notificationId);

    assertThat(notification.getConfirmed()).isTrue();
  }

  @Test
  void confirmNotification_noOp_whenNotFound() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    UUID notificationId = UUID.randomUUID();
    when(notificationRepository.findByIdAndUser_IdAndConfirmedFalse(notificationId, userId))
        .thenReturn(Optional.empty());

    notificationService.confirmNotification(userId, notificationId);

    verify(notificationRepository).findByIdAndUser_IdAndConfirmedFalse(notificationId, userId);
  }

  @Test
  void createNotification_saves_whenUserExists() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    UUID resourceId = UUID.randomUUID();
    notificationService.createNotification(userId, "hello", ResourceType.INTEREST, resourceId);

    ArgumentCaptor<Object> evtCaptor = ArgumentCaptor.forClass(Object.class);
    verify(eventPublisher).publishEvent(evtCaptor.capture());
    Object published = evtCaptor.getValue();
    assertThat(published).isInstanceOf(NotificationCreatedEvent.class);
    NotificationCreatedEvent evt = (NotificationCreatedEvent) published;
    assertThat(evt.userId()).isEqualTo(userId);
    assertThat(evt.content()).isEqualTo("hello");
    assertThat(evt.resourceType()).isEqualTo(ResourceType.INTEREST);
    assertThat(evt.resourceId()).isEqualTo(resourceId);
  }

  @Test
  void createNotification_throws_whenUserMissing() {
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> notificationService.createNotification(userId, "hi", ResourceType.INTEREST, UUID.randomUUID()))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.USER_NOT_FOUND);
  }

  @Test
  void createNotification_throws_whenInvalidInput() {
    assertThatThrownBy(() -> notificationService.createNotification(null, "", ResourceType.INTEREST, null))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_INPUT);
  }

  @Test
  void createNotifications_returnsZero_whenEmpty() {
    int created = notificationService.createNotifications(List.of());
    assertThat(created).isZero();
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void createNotifications_publishes_events_forCommands() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    NotificationCreateCommand c1 = new NotificationCreateCommand(userId, "a", ResourceType.INTEREST, UUID.randomUUID());
    NotificationCreateCommand c2 = new NotificationCreateCommand(userId, "b", ResourceType.INTEREST, UUID.randomUUID());

    int created = notificationService.createNotifications(List.of(c1, c2));

    assertThat(created).isEqualTo(2);
    ArgumentCaptor<Object> evtCaptor = ArgumentCaptor.forClass(Object.class);
    verify(eventPublisher, org.mockito.Mockito.times(2)).publishEvent(evtCaptor.capture());
    List<Object> published = evtCaptor.getAllValues();
    assertThat(published).hasSize(2);
    for (Object o : published) {
      assertThat(o).isInstanceOf(NotificationCreatedEvent.class);
    }
  }

  @Test
  void confirmAllNotifications_confirms_whenExists() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    Notification n1 = Notification.of(user, "x", ResourceType.INTEREST, UUID.randomUUID());
    Notification n2 = Notification.of(user, "y", ResourceType.INTEREST, UUID.randomUUID());
    when(notificationRepository.findAllByUser_IdAndConfirmedFalse(userId)).thenReturn(List.of(n1, n2));

    notificationService.confirmAllNotifications(userId);

    assertThat(n1.getConfirmed()).isTrue();
    assertThat(n2.getConfirmed()).isTrue();
    verify(notificationRepository).findAllByUser_IdAndConfirmedFalse(userId);
  }

  @Test
  void confirmAllNotifications_noOp_whenNoNotifications() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(notificationRepository.findByUserIdAndConfirmedFalse(userId)).thenReturn(List.of());

    notificationService.confirmAllNotifications(userId);

    verify(notificationRepository, never()).saveAll(anyList());
  }

  @Test
  void deleteOldConfirmedNotifications_delegatesToRepository() {
    when(notificationRepository.deleteByConfirmedIsTrueAndCreatedAtBefore(any(Instant.class))).thenReturn(3L);
    long deleted = notificationService.deleteOldConfirmedNotifications();
    assertThat(deleted).isEqualTo(3L);
    verify(notificationRepository).deleteByConfirmedIsTrueAndCreatedAtBefore(any(Instant.class));
  }
}
