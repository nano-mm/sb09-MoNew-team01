package com.monew.service;

import com.monew.dto.comment.CommentCursor;
import com.monew.dto.comment.CommentSortType;
import com.monew.dto.response.CommentResponse;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.entity.Comment;
import com.monew.entity.CommentLike;
import com.monew.exception.CommentNotFoundException;
import com.monew.exception.DuplicateLikeException;
import com.monew.exception.ForbiddenException;
import com.monew.exception.LikeNotFoundException;
import com.monew.repository.CommentLikeRepository;
import com.monew.repository.CommentRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monew.entity.Article;
import com.monew.entity.User;
import com.monew.exception.ArticleNotFoundException;
import com.monew.exception.user.UserNotFoundException;
import com.monew.repository.article.ArticleRepository;
import com.monew.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class CommentService {  // 클래스 레벨 @Transactional 제거

  private final CommentRepository commentRepository;
  private final CommentLikeRepository commentLikeRepository;
  private final ArticleRepository articleRepository;
  private final UserRepository userRepository;

  @Transactional
  public UUID createComment(UUID userId, UUID articleId, String content) {
    Article article = articleRepository.findById(articleId)
        .orElseThrow(ArticleNotFoundException::new);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다"));
    Comment comment = Comment.create(article, user, content);
    commentRepository.save(comment);
    return comment.getId();
  }

  @Transactional
  public void updateComment(UUID userId, UUID commentId, String content) {
    Comment comment = getActiveComment(commentId);
    if (!comment.getUserId().equals(userId)) {
      throw new ForbiddenException();
    }
    comment.updateContent(content);
  }

  @Transactional
  public void deleteComment(UUID userId, UUID commentId) {
    Comment comment = getActiveComment(commentId);
    if (!comment.getUserId().equals(userId)) {
      throw new ForbiddenException();
    }
    comment.softDelete();
  }

  @Transactional
  public void likeComment(UUID userId, UUID commentId) {
    Comment comment = getActiveComment(commentId);
    if (commentLikeRepository.existsByComment_IdAndUser_Id(commentId, userId)) {
      throw new DuplicateLikeException();
    }

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다"));

    commentLikeRepository.save(new CommentLike(comment, user));
    comment.increaseLikeCount();
  }

  @Transactional
  public void unlikeComment(UUID userId, UUID commentId) {
    Comment comment = getActiveComment(commentId);
    int deleted = commentLikeRepository.deleteByComment_IdAndUser_Id(commentId, userId);
    if (deleted == 0) {
      throw new LikeNotFoundException();
    }
    comment.decreaseLikeCount();
  }

  @Transactional(readOnly = true)  // 조회 메서드에만 readOnly
  public CursorPageResponseDto<CommentResponse> getComments(
      UUID articleId,
      CommentSortType sortType,
      String rawCursor,
      int size
  ) {
    CommentCursor cursor = parseCursor(sortType, rawCursor);

    List<Comment> comments = commentRepository.findByArticleIdWithCursor(
        articleId.toString(), sortType, cursor, size
    );

    List<CommentResponse> content = comments.stream()
        .map(c -> new CommentResponse(
            c.getId(),
            c.getArticleId(),
            c.getUserId(),
            c.getUser().getNickname(),
            c.getContent(),
            c.getLikeCount(),
            commentLikeRepository.existsByComment_IdAndUser_Id(
                c.getId(), userId
            ),
            c.getCreatedAt()
        ))
        .toList();

    boolean hasNext = comments.size() == size;
    String nextCursor = null;
    Instant nextAfter = null;

    if (hasNext) {
      Comment last = comments.get(comments.size() - 1);
      if (sortType == CommentSortType.LIKE_COUNT) {
        nextCursor = last.getId() + "," + last.getLikeCount();
      } else {
        nextAfter = last.getCreatedAt();
        nextCursor = last.getId() + "," + nextAfter;
      }
    }

    return CursorPageResponseDto.<CommentResponse>builder()
        .content(content)
        .nextCursor(nextCursor)
        .nextAfter(nextAfter)
        .size(content.size())
        .totalElements((long) content.size())
        .hasNext(hasNext)
        .build();
  }

  private CommentCursor parseCursor(CommentSortType sortType, String rawCursor) {
    if (rawCursor == null) return null;

    String[] parts = rawCursor.split(",", -1);
    if (parts.length != 2) {
      throw new IllegalArgumentException("잘못된 cursor 형식: " + rawCursor);
    }

    try {
      UUID lastId = UUID.fromString(parts[0]);
      return switch (sortType) {
        case CREATED_AT -> new CommentCursor(lastId, Instant.parse(parts[1]), 0);
        case LIKE_COUNT -> new CommentCursor(lastId, null, Integer.parseInt(parts[1]));
      };
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("잘못된 cursor 형식: " + rawCursor, e);
    }
  }

  private Comment getActiveComment(UUID commentId) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(CommentNotFoundException::new);
    if (comment.isDeleted()) {
      throw new CommentNotFoundException();
    }
    return comment;
  }
}