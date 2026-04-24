package com.monew.service;

import com.monew.dto.comment.CommentCursor;
import com.monew.dto.comment.CommentSortType;
import com.monew.dto.request.CommentResponseDto;
import com.monew.dto.response.CommentDto;
import com.monew.dto.response.CommentLikeResponse;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.entity.Comment;
import com.monew.entity.CommentLike;
import com.monew.exception.CommentNotFoundException;
import com.monew.exception.DuplicateLikeException;
import com.monew.exception.ForbiddenException;
import com.monew.exception.LikeNotFoundException;
import com.monew.mapper.CommentMapper;
import com.monew.repository.CommentLikeRepository;
import com.monew.repository.CommentRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monew.entity.Article;
import com.monew.entity.User;
import com.monew.exception.user.UserNotFoundException;
import com.monew.repository.article.ArticleRepository;
import com.monew.repository.UserRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class CommentService {

  private final CommentRepository commentRepository;
  private final CommentLikeRepository commentLikeRepository;
  private final ArticleRepository articleRepository;
  private final UserRepository userRepository;
  private final CommentMapper commentMapper;

  public CommentDto createComment(UUID userId, UUID articleId, String content) {
    Article article = articleRepository.findById(articleId)
        .orElseThrow(() -> new NoSuchElementException("해당 Article을 찾을 수 없습니다"));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다"));
    Comment comment = Comment.create(article, user, content);
    commentRepository.saveAndFlush(comment);

    articleRepository.incrementCommentCount(article.getId());

    return commentMapper.toResponse(comment);

  }

  public CommentDto updateComment(UUID userId, UUID commentId, String content) {
    Comment comment = getActiveComment(commentId);

    if(userRepository.findById(userId).isEmpty()) {
      throw new UserNotFoundException("사용자를 찾을 수 없습니다");
    }

    if (!comment.isOwnedBy(userId)) {
      throw new ForbiddenException();
    }
    comment.updateContent(content);
    return commentMapper.toResponse(comment);
  }

  public void deleteComment(UUID commentId) {
    Comment comment = getActiveComment(commentId);
    comment.softDelete(Instant.now());
    articleRepository.decrementCommentCount(comment.getArticleId());
  }

  public void hardDeleteComment(UUID userId, UUID commentId) {
    Comment comment = commentRepository.findByIdIncludeDeleted(commentId)
        .orElseThrow(CommentNotFoundException::new);
    if (!comment.isOwnedBy(userId)) {
      throw new ForbiddenException();
    }
    commentRepository.delete(comment);  // cascade로 likes도 자동 삭제
  }

  public CommentLikeResponse likeComment(UUID userId, UUID commentId) {
    Comment comment = getActiveComment(commentId);
    if (commentLikeRepository.existsByComment_IdAndUser_Id(commentId, userId)) {
      throw new DuplicateLikeException();
    }
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다"));
    CommentLike commentLike = new CommentLike(comment, user);
    commentLikeRepository.save(commentLike);
    comment.increaseLikeCount();

    return new CommentLikeResponse(
        commentLike.getId(),
        userId,
        commentLike.getCreatedAt(),
        comment.getId(),
        comment.getArticleId(),
        comment.getUserId(),
        user.getNickname(),
        comment.getContent(),
        comment.getLikeCount(),
        comment.getCreatedAt()
    );
  }

  public void unlikeComment(UUID userId, UUID commentId) {
    Comment comment = getActiveComment(commentId);
    int deleted = commentLikeRepository.deleteByComment_IdAndUser_Id(commentId, userId);
    if (deleted == 0) {
      throw new LikeNotFoundException();
    }
    comment.decreaseLikeCount();
  }

  @Transactional(readOnly = true)
  public CursorPageResponseDto<CommentDto> getComments(
      CommentResponseDto requestDto, UUID userId
  ) {
    CommentSortType orderBy = requestDto.orderBy();
    String rawCursor = requestDto.cursor();
    int size = requestDto.limit();
    UUID articleId = requestDto.articleId();

    CommentCursor cursor = parseCursor(orderBy, rawCursor);

    List<Comment> comments = commentRepository.findByArticleIdWithCursor(
        requestDto.articleId(), orderBy, cursor, size+1);

    boolean hasNext = comments.size() > size;
    List<Comment> pageComments = hasNext ? comments.subList(0, size) : comments;

    // N+1 방지: 한 번에 likedByMe 조회
    List<UUID> commentIds = pageComments.stream().map(Comment::getId).toList();
    Set<UUID> likedCommentIds = new HashSet<>(
        commentLikeRepository.findCommentIdsByUserIdAndCommentIdIn(userId, commentIds)
    );

    List<UUID> userIds = pageComments.stream().map(Comment::getUserId).toList();
    Map<UUID, String> nicknameMap = userRepository.findAllById(userIds).stream()
        .collect(Collectors.toMap(User::getId, User::getNickname));

    List<CommentDto> content = pageComments.stream()
        .map(comment -> new CommentDto(
            comment.getId(),
            comment.getArticleId(),
            comment.getUserId(),
            nicknameMap.getOrDefault(comment.getUserId(), ""),
            comment.getContent(),
            comment.getLikeCount(),
            likedCommentIds.contains(comment.getId()),
            comment.getCreatedAt()
        ))
        .toList();

    String nextCursor = null;
    Instant nextAfter = null;

    if (hasNext) {
      Comment last = pageComments.get(pageComments.size() - 1);
      if (requestDto.orderBy() == CommentSortType.LIKE_COUNT) {
        nextCursor = last.getId() + "," + last.getLikeCount();
      } else {
        nextAfter = last.getCreatedAt();
        nextCursor = last.getId() + "," + nextAfter;
      }
    }

    long totalElements = commentRepository.countByArticle_IdAndDeletedAtIsNull(articleId);

    return CursorPageResponseDto.<CommentDto>builder()
        .content(content)
        .nextCursor(nextCursor)
        .nextAfter(nextAfter)
        .size(content.size())
        .totalElements(totalElements)
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
    return commentRepository.findById(commentId)
        .orElseThrow(CommentNotFoundException::new);
  }
}
