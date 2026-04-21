package com.monew.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

  @Mock
  private NotificationRepository notificationRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private NotificationMapper notificationMapper;

  @InjectMocks
  private NotificationServiceImpl notificationService;

  private UUID userId;
  private User user;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    user = User.builder()
        .email("test@monew.com")
        .nickname("tester")
        .password("password")
        .build();
  }

  @Test
  void getNotifications_다음페이지가_있으면_cursor와_after를_반환한다() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    Notification first = notification(UUID.fromString("00000000-0000-0000-0000-000000000001"),
        Instant.parse("2026-04-19T12:00:03Z"));
    Notification second = notification(UUID.fromString("00000000-0000-0000-0000-000000000002"),
        Instant.parse("2026-04-19T12:00:02Z"));
    Notification extra = notification(UUID.fromString("00000000-0000-0000-0000-000000000003"),
        Instant.parse("2026-04-19T12:00:01Z"));

    when(notificationRepository.findByUserIdWithCursor(userId, null, 3))
        .thenReturn(List.of(first, second, extra));
    when(notificationRepository.countByUser_IdAndConfirmedFalse(userId)).thenReturn(7L);

    NotificationDto firstDto = notificationDto(first.getId(), userId);
    NotificationDto secondDto = notificationDto(second.getId(), userId);
    when(notificationMapper.toDto(first)).thenReturn(firstDto);
    when(notificationMapper.toDto(second)).thenReturn(secondDto);

    CursorPageResponseDto<NotificationDto> response = notificationService.getNotifications(userId, null, null, 2);

    assertThat(response.content()).containsExactly(firstDto, secondDto);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.nextCursor()).isEqualTo(second.getId().toString());
    assertThat(response.nextAfter()).isEqualTo(second.getCreatedAt());
    assertThat(response.size()).isEqualTo(2);
    assertThat(response.totalElements()).isEqualTo(7L);
  }

  @Test
  void getNotifications_마지막페이지면_nextCursor와_nextAfter가_null이다() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    Notification first = notification(UUID.randomUUID(), Instant.parse("2026-04-19T12:00:03Z"));
    when(notificationRepository.findByUserIdWithCursor(userId, null, 3))
        .thenReturn(List.of(first));
    when(notificationRepository.countByUser_IdAndConfirmedFalse(userId)).thenReturn(1L);

    NotificationDto dto = notificationDto(first.getId(), userId);
    when(notificationMapper.toDto(first)).thenReturn(dto);

    CursorPageResponseDto<NotificationDto> response = notificationService.getNotifications(userId, null, null, 2);

    assertThat(response.content()).containsExactly(dto);
    assertThat(response.hasNext()).isFalse();
    assertThat(response.nextCursor()).isNull();
    assertThat(response.nextAfter()).isNull();
  }

  @Test
  void getNotifications_cursor형식이_잘못되면_INVALID_INPUT을_던진다() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> notificationService.getNotifications(userId, "invalid-uuid", null, 10))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_INPUT);
  }

  @Test
  void getNotifications_cursor알림이_없으면_INVALID_INPUT을_던진다() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    UUID cursorId = UUID.randomUUID();
    when(notificationRepository.findByIdAndUser_IdAndConfirmedFalse(cursorId, userId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> notificationService.getNotifications(userId, cursorId.toString(), null, 10))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_INPUT);
  }

  @Test
  void getNotifications_cursor가_있으면_cursor알림_createdAt_기준으로_조회한다() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    UUID cursorId = UUID.randomUUID();
    Instant cursorCreatedAt = Instant.parse("2026-04-19T12:00:00Z");
    Notification cursorNotification = notification(cursorId, cursorCreatedAt);

    when(notificationRepository.findByIdAndUser_IdAndConfirmedFalse(cursorId, userId))
        .thenReturn(Optional.of(cursorNotification));
    when(notificationRepository.findByUserIdWithCursor(userId, cursorCreatedAt, 2))
        .thenReturn(List.of());
    when(notificationRepository.countByUser_IdAndConfirmedFalse(userId)).thenReturn(0L);

    CursorPageResponseDto<NotificationDto> response = notificationService.getNotifications(
        userId,
        cursorId.toString(),
        null,
        1
    );

    assertThat(response.content()).isEmpty();
    verify(notificationRepository).findByUserIdWithCursor(userId, cursorCreatedAt, 2);
  }

  @Test
  void getNotifications_사용자가_없으면_USER_NOT_FOUND를_던진다() {
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> notificationService.getNotifications(userId, null, null, 10))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.USER_NOT_FOUND);
  }

  @Test
  void confirmNotification_미확인알림이_있으면_confirm을_호출한다() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    UUID notificationId = UUID.randomUUID();
    Notification notification = notification(notificationId, Instant.now());

    when(notificationRepository.findByIdAndUser_IdAndConfirmedFalse(notificationId, userId))
        .thenReturn(Optional.of(notification));

    notificationService.confirmNotification(userId, notificationId);

    assertThat(notification.getConfirmed()).isTrue();
  }

  @Test
  void confirmNotification_미확인알림이_없으면_confirm을_호출하지_않는다() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    UUID notificationId = UUID.randomUUID();

    when(notificationRepository.findByIdAndUser_IdAndConfirmedFalse(notificationId, userId))
        .thenReturn(Optional.empty());

    notificationService.confirmNotification(userId, notificationId);

    verify(notificationRepository).findByIdAndUser_IdAndConfirmedFalse(notificationId, userId);
  }

  @Test
  void confirmAllNotifications_미확인알림이_없으면_confirm을_호출하지_않는다() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(notificationRepository.findAllByUser_IdAndConfirmedFalse(userId)).thenReturn(List.of());

    notificationService.confirmAllNotifications(userId);

    verify(notificationRepository).findAllByUser_IdAndConfirmedFalse(userId);
  }

  @Test
  void confirmAllNotifications_미확인알림이_있으면_전체_confirm을_호출한다() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    Notification first = notification(UUID.randomUUID(), Instant.now());
    Notification second = notification(UUID.randomUUID(), Instant.now());
    when(notificationRepository.findAllByUser_IdAndConfirmedFalse(userId))
        .thenReturn(List.of(first, second));

    notificationService.confirmAllNotifications(userId);

    assertThat(first.getConfirmed()).isTrue();
    assertThat(second.getConfirmed()).isTrue();
  }

  @Test
  void confirmAllNotifications_사용자가_없으면_USER_NOT_FOUND를_던진다() {
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> notificationService.confirmAllNotifications(userId))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.USER_NOT_FOUND);

    verify(notificationRepository, never()).findAllByUser_IdAndConfirmedFalse(any());
  }

  @Test
  void createNotification_사용자가_있으면_알림을_저장한다() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    UUID resourceId = UUID.randomUUID();

    notificationService.createNotification(userId, "새 기사 알림", ResourceType.INTEREST, resourceId);

    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationRepository).save(captor.capture());

    Notification saved = captor.getValue();
    assertThat(saved.getUser()).isEqualTo(user);
    assertThat(saved.getContent()).isEqualTo("새 기사 알림");
    assertThat(saved.getResourceType()).isEqualTo(ResourceType.INTEREST);
    assertThat(saved.getResourceId()).isEqualTo(resourceId);
    assertThat(saved.getConfirmed()).isFalse();
  }

  @Test
  void createNotification_사용자가_없으면_USER_NOT_FOUND를_던진다() {
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> notificationService.createNotification(
        userId,
        "새 기사 알림",
        ResourceType.INTEREST,
        UUID.randomUUID()
    ))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.USER_NOT_FOUND);

    verify(notificationRepository, never()).save(any());
  }

  @Test
  void createNotifications_요청이_있으면_다건_저장하고_반영건수를_반환한다() {
    UUID userId2 = UUID.randomUUID();
    User user2 = User.builder()
        .email("test2@monew.com")
        .nickname("tester2")
        .password("password")
        .build();

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.findById(userId2)).thenReturn(Optional.of(user2));

    List<NotificationCreateCommand> commands = List.of(
        new NotificationCreateCommand(userId, "알림1", ResourceType.INTEREST, UUID.randomUUID()),
        new NotificationCreateCommand(userId2, "알림2", ResourceType.COMMENT, UUID.randomUUID())
    );

    int created = notificationService.createNotifications(commands);

    verify(notificationRepository).saveAll(any());
    assertThat(created).isEqualTo(2);
  }

  @Test
  void createNotifications_요청이_비어있으면_저장하지_않고_0을_반환한다() {
    int created = notificationService.createNotifications(List.of());

    assertThat(created).isZero();
    verify(notificationRepository, never()).saveAll(any());
  }

  private Notification notification(UUID notificationId, Instant createdAt) {
    Notification notification = Notification.of(user, "content", com.monew.entity.enums.ResourceType.INTEREST, UUID.randomUUID());
    ReflectionTestUtils.setField(notification, "id", notificationId);
    ReflectionTestUtils.setField(notification, "createdAt", createdAt);
    return notification;
  }

  private NotificationDto notificationDto(UUID notificationId, UUID uid) {
    return new NotificationDto(
        notificationId,
        Instant.parse("2026-04-19T12:00:00Z"),
        Instant.parse("2026-04-19T12:00:00Z"),
        false,
        uid,
        "content",
        "interest",
        UUID.randomUUID()
    );
  }
}

