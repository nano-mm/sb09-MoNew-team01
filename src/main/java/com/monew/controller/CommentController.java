package com.monew.controller;

import com.monew.dto.comment.CommentCursor;
import com.monew.dto.comment.CommentSortType;
import com.monew.dto.request.CreateCommentRequest;
import com.monew.dto.request.UpdateCommentRequest;
import com.monew.dto.response.CommentResponse;
import com.monew.entity.Comment;
import com.monew.service.CommentService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

  private final CommentService commentService;

  // TODO: 임시 (인증 없을 때)
  private UUID getUserId() {
    return UUID.fromString("00000000-0000-0000-0000-000000000001");
  }

  @PostMapping
  public ResponseEntity<UUID> createComment(
      @RequestBody @Valid CreateCommentRequest request
  ) {
    UUID userId = getUserId();

    UUID commentId = commentService.createComment(
        userId,
        request.getArticleId(),
        request.getContent()
    );

    return ResponseEntity.ok(commentId);
  }

  @PatchMapping("/{commentId}")
  public ResponseEntity<Void> updateComment(
      @PathVariable UUID commentId,
      @RequestBody @Valid UpdateCommentRequest request
  ) {
    UUID userId = getUserId();

    commentService.updateComment(userId, commentId, request.getContent());

    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{commentId}")
  public ResponseEntity<Void> deleteComment(
      @PathVariable UUID commentId
  ) {
    UUID userId = getUserId();

    commentService.deleteComment(userId, commentId);

    return ResponseEntity.ok().build();
  }

  @PostMapping("/{commentId}/like")
  public ResponseEntity<Void> like(@PathVariable UUID commentId) {
    UUID userId = getUserId();

    commentService.likeComment(userId, commentId);

    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{commentId}/like")
  public ResponseEntity<Void> unlike(@PathVariable UUID commentId) {
    UUID userId = getUserId();

    commentService.unlikeComment(userId, commentId);

    return ResponseEntity.ok().build();
  }

  @GetMapping
  public ResponseEntity<List<CommentResponse>> getComments(
      @RequestParam UUID articleId,
      @RequestParam CommentSortType sortType,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "10") int size
  ) {
    CommentSortType parsedSortType;
    try {
      parsedSortType = CommentSortType.valueOf(String.valueOf(sortType));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("지원하지 않는 정렬 타입입니다: " + sortType);
    }
    CommentCursor parsedCursor = parseCursor(String.valueOf(sortType), cursor);

    List<Comment> comments = commentService.getComments(
        articleId,
        parsedSortType,
        parsedCursor,
        size
    );

    List<CommentResponse> response = comments.stream()
        .map(c -> CommentResponse.builder()
            .commentId(c.getId())
            .userId(c.getUserId())
            .content(c.getContent())
            .likeCount(c.getLikeCount())
            .createdAt(LocalDateTime.from(c.getCreatedAt()))
            .build())
        .toList();

    return ResponseEntity.ok(response);
  }

  private CommentCursor parseCursor(String sortType, String cursor) {
    if (cursor == null)
      return null;

    String[] parts = cursor.split(",", -1);
    if (parts.length != 2) {
      throw new IllegalArgumentException("잘못된 cursor 형식: " + cursor);
    }
    if ("CREATED_AT".equals(sortType)) {
      try {
        UUID lastId = UUID.fromString(parts[0]);
        return new CommentCursor(lastId, Instant.parse(parts[1]), 0);
      } catch (RuntimeException e) {
        throw new IllegalArgumentException("잘못된 cursor 형식: " + cursor, e);
      }
    }
    if ("LIKE_COUNT".equals(sortType)) {
      try {
        UUID lastId = UUID.fromString(parts[0]);
        return new CommentCursor(lastId, null, Integer.parseInt(parts[1]));
      } catch (RuntimeException e) {
        throw new IllegalArgumentException("잘못된 cursor 형식: " + cursor, e);
      }
    }
    return null;
  }
}
