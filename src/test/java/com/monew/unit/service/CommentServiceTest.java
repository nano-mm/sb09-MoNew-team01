package com.monew.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.monew.dto.comment.CommentSortType;
import com.monew.dto.request.CommentResponseDto;
import com.monew.dto.response.CommentDto;
import com.monew.dto.response.CommentLikeResponse;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.entity.Article;
import com.monew.entity.Comment;
import com.monew.entity.CommentLike;
import com.monew.entity.User;
import com.monew.exception.CommentNotFoundException;
import com.monew.exception.DuplicateLikeException;
import com.monew.exception.ForbiddenException;
import com.monew.exception.LikeNotFoundException;
import com.monew.mapper.CommentMapper;
import com.monew.repository.CommentLikeRepository;
import com.monew.repository.CommentRepository;
import com.monew.repository.UserRepository;
import com.monew.repository.article.ArticleRepository;
import com.monew.service.CommentService;
import com.monew.service.UserActivityReadModelService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentServiceTest {

  @InjectMocks
  private CommentService commentService;

  @Mock private CommentRepository commentRepository;
  @Mock private CommentLikeRepository commentLikeRepository;
  @Mock private ArticleRepository articleRepository;
  @Mock private UserRepository userRepository;
  @Mock private CommentMapper commentMapper;
  @Mock private UserActivityReadModelService userActivityReadModelService;

  private User user;
  private Article article;
  private UUID userId;
  private UUID articleId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    articleId = UUID.randomUUID();
    user = mock(User.class);
    article = mock(Article.class);
    
    lenient().when(user.getId()).thenReturn(userId);
    lenient().when(user.getNickname()).thenReturn("tester");
    lenient().when(article.getId()).thenReturn(articleId);
  }

  @Nested
  @DisplayName("댓글 생성")
  class CreateComment {
    @Test
    @DisplayName("성공적으로 댓글을 생성한다")
    void success() {
      // given
      String content = "댓글 내용";
      when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      
      CommentDto responseDto = new CommentDto(UUID.randomUUID(), articleId, userId, "tester", content, 0, false, Instant.now());
      when(commentMapper.toResponse(any(Comment.class))).thenReturn(responseDto);

      // when
      CommentDto result = commentService.createComment(userId, articleId, content);

      // then
      assertThat(result).isEqualTo(responseDto);
      verify(commentRepository, times(1)).saveAndFlush(any(Comment.class));
    }

    @Test
    @DisplayName("존재하지 않는 게시글에 댓글을 달면 예외가 발생한다")
    void articleNotFound() {
      when(articleRepository.findById(articleId)).thenReturn(Optional.empty());
      assertThatThrownBy(() -> commentService.createComment(userId, articleId, "content"))
          .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    @DisplayName("존재하지 않는 유저가 댓글을 달면 예외가 발생한다")
    void userNotFound() {
      when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
      when(userRepository.findById(userId)).thenReturn(Optional.empty());
      assertThatThrownBy(() -> commentService.createComment(userId, articleId, "content"))
          .isInstanceOf(com.monew.exception.user.UserNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("댓글 수정")
  class UpdateComment {
    @Test
    @DisplayName("본인의 댓글을 수정할 수 있다")
    void success() {
      // given
      UUID commentId = UUID.randomUUID();
      Comment comment = Comment.create(article, user, "old content");
      ReflectionTestUtils.setField(comment, "id", commentId);
      
      when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      // when
      commentService.updateComment(userId, commentId, "new content");

      // then
      assertThat(comment.getContent()).isEqualTo("new content");
    }

    @Test
    @DisplayName("댓글 수정 시 유저를 찾을 수 없으면 예외가 발생한다")
    void userNotFound() {
      UUID commentId = UUID.randomUUID();
      Comment comment = Comment.create(article, user, "content");
      when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> commentService.updateComment(userId, commentId, "new content"))
          .isInstanceOf(com.monew.exception.user.UserNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("댓글 삭제")
  class DeleteComment {
    @Test
    @DisplayName("댓글을 논리 삭제한다")
    void softDelete() {
      // given
      UUID commentId = UUID.randomUUID();
      Comment comment = Comment.create(article, user, "content");
      when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

      // when
      commentService.deleteComment(commentId);

      // then
      assertThat(comment.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("댓글을 물리 삭제한다")
    void hardDelete() {
      // given
      UUID commentId = UUID.randomUUID();
      Comment comment = Comment.create(article, user, "content");
      when(commentRepository.findByIdIncludeDeleted(commentId)).thenReturn(Optional.of(comment));

      // when
      commentService.hardDeleteComment(commentId);

      // then
      verify(commentRepository, times(1)).delete(comment);
    }

    @Test
    @DisplayName("존재하지 않는 댓글 물리 삭제 시 예외 발생")
    void hardDeleteNotFound() {
      UUID commentId = UUID.randomUUID();
      when(commentRepository.findByIdIncludeDeleted(commentId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> commentService.hardDeleteComment(commentId))
          .isInstanceOf(CommentNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("댓글 좋아요")
  class LikeComment {
    @Test
    @DisplayName("댓글에 좋아요를 누른다")
    void success() {
      // given
      UUID commentId = UUID.randomUUID();
      Comment comment = spy(Comment.create(article, user, "content"));
      ReflectionTestUtils.setField(comment, "id", commentId);
      
      when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
      when(commentLikeRepository.existsByComment_IdAndUser_Id(commentId, userId)).thenReturn(false);
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      // when
      CommentLikeResponse result = commentService.likeComment(userId, commentId);

      // then
      assertThat(result.commentId()).isEqualTo(commentId);
      verify(comment, times(1)).increaseLikeCount();
      verify(commentLikeRepository, times(1)).save(any(CommentLike.class));
    }

    @Test
    @DisplayName("좋아요 시 유저를 찾을 수 없으면 예외가 발생한다")
    void userNotFound() {
      UUID commentId = UUID.randomUUID();
      Comment comment = Comment.create(article, user, "content");
      when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
      when(commentLikeRepository.existsByComment_IdAndUser_Id(commentId, userId)).thenReturn(false);
      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> commentService.likeComment(userId, commentId))
          .isInstanceOf(com.monew.exception.user.UserNotFoundException.class);
    }

    @Test
    @DisplayName("이미 좋아요를 누른 경우 예외가 발생한다")
    void duplicateLike() {
      // given
      UUID commentId = UUID.randomUUID();
      Comment comment = Comment.create(article, user, "content");
      when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
      when(commentLikeRepository.existsByComment_IdAndUser_Id(commentId, userId)).thenReturn(true);

      // when & then
      assertThatThrownBy(() -> commentService.likeComment(userId, commentId))
          .isInstanceOf(DuplicateLikeException.class);
    }
  }

  @Nested
  @DisplayName("댓글 목록 조회")
  class GetComments {
    @Test
    @DisplayName("댓글 목록을 페이징하여 조회한다 (최신순)")
    void success() {
      // given
      CommentResponseDto request = new CommentResponseDto(articleId, CommentSortType.CREATED_AT, "desc", null, null, 10);
      Comment comment = Comment.create(article, user, "content");
      ReflectionTestUtils.setField(comment, "id", UUID.randomUUID());
      ReflectionTestUtils.setField(comment, "createdAt", Instant.now());
      
      when(commentRepository.findByArticleIdWithCursor(any(), any(), any(), anyInt()))
          .thenReturn(List.of(comment));
      when(userRepository.findAllById(any())).thenReturn(List.of(user));

      // when
      CursorPageResponseDto<CommentDto> result = commentService.getComments(request, userId);

      // then
      assertThat(result.content()).hasSize(1);
      assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("잘못된 커서 형식이 전달되면 예외가 발생한다")
    void invalidCursorFormat() {
      // parts.length != 2
      CommentResponseDto request = new CommentResponseDto(articleId, CommentSortType.CREATED_AT, "desc", "invalid_cursor", null, 10);
      assertThatThrownBy(() -> commentService.getComments(request, userId))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("잘못된 cursor 형식");

      // UUID parse error
      CommentResponseDto request2 = new CommentResponseDto(articleId, CommentSortType.CREATED_AT, "desc", "not-uuid,2024-01-01T00:00:00Z", null, 10);
      assertThatThrownBy(() -> commentService.getComments(request2, userId))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("좋아요순으로 정렬하여 조회하고 내가 좋아요를 누른 댓글이 표시된다")
    void likeCountOrderAndLikedByMe() {
      // given
      CommentResponseDto request = new CommentResponseDto(articleId, CommentSortType.LIKE_COUNT, "desc", null, null, 1);
      Comment comment1 = Comment.create(article, user, "content1");
      Comment comment2 = Comment.create(article, user, "content2");
      ReflectionTestUtils.setField(comment1, "id", UUID.randomUUID());
      ReflectionTestUtils.setField(comment1, "likeCount", 10);
      ReflectionTestUtils.setField(comment2, "id", UUID.randomUUID());
      ReflectionTestUtils.setField(comment2, "likeCount", 5);
      
      when(commentRepository.findByArticleIdWithCursor(any(), any(), any(), anyInt()))
          .thenReturn(List.of(comment1, comment2));
      when(commentLikeRepository.findCommentIdsByUserIdAndCommentIdIn(eq(userId), any()))
          .thenReturn(List.of(comment1.getId()));
      when(userRepository.findAllById(any())).thenReturn(List.of(user));

      // when
      CursorPageResponseDto<CommentDto> result = commentService.getComments(request, userId);

      // then
      assertThat(result.content()).hasSize(1);
      assertThat(result.hasNext()).isTrue();
      assertThat(result.content().get(0).likedByMe()).isTrue();
      assertThat(result.nextCursor()).contains("10"); // LIKE_COUNT 커서 확인
    }
  }

  @Test
  @DisplayName("댓글 좋아요 취소")
  void unlikeComment() {
    // given
    UUID commentId = UUID.randomUUID();
    Comment comment = spy(Comment.create(article, user, "content"));
    ReflectionTestUtils.setField(comment, "id", commentId);
    ReflectionTestUtils.setField(comment, "likeCount", 1);
    
    when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
    when(commentLikeRepository.deleteByComment_IdAndUser_Id(commentId, userId)).thenReturn(1);

    // when
    commentService.unlikeComment(userId, commentId);

    // then
    verify(comment, times(1)).decreaseLikeCount();
    assertThat(comment.getLikeCount()).isEqualTo(0);
  }

  @Test
  @DisplayName("존재하지 않는 좋아요를 취소하려고 하면 예외가 발생한다")
  void unlikeNotFound() {
    UUID commentId = UUID.randomUUID();
    Comment comment = Comment.create(article, user, "content");
    when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
    when(commentLikeRepository.deleteByComment_IdAndUser_Id(commentId, userId)).thenReturn(0);

    assertThatThrownBy(() -> commentService.unlikeComment(userId, commentId))
        .isInstanceOf(LikeNotFoundException.class);
  }
}
