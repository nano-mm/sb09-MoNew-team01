package com.monew.controller;

import com.monew.dto.request.CreateCommentRequest;
import com.monew.dto.request.UpdateCommentRequest;
import com.monew.dto.response.CommentResponse;
import com.monew.entity.Comment;
import com.monew.service.CommentService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
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
  private Long getUserId() {
    return 1L;
  }

  @PostMapping
  public ResponseEntity<Long> createComment(
      @RequestBody @Valid CreateCommentRequest request
  ) {
    Long userId = getUserId();

    Long commentId = commentService.createComment(
        userId,
        request.getArticleId(),
        request.getContent()
    );

    return ResponseEntity.ok(commentId);
  }

  @PatchMapping("/{commentId}")
  public ResponseEntity<Void> updateComment(
      @PathVariable Long commentId,
      @RequestBody @Valid UpdateCommentRequest request
  ) {
    Long userId = getUserId();

    commentService.updateComment(userId, commentId, request.getContent());

    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{commentId}")
  public ResponseEntity<Void> deleteComment(
      @PathVariable Long commentId
  ) {
    Long userId = getUserId();

    commentService.deleteComment(userId, commentId);

    return ResponseEntity.ok().build();
  }

  @PostMapping("/{commentId}/like")
  public ResponseEntity<Void> like(@PathVariable Long commentId) {
    Long userId = getUserId();

    commentService.likeComment(userId, commentId);

    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{commentId}/like")
  public ResponseEntity<Void> unlike(@PathVariable Long commentId) {
    Long userId = getUserId();

    commentService.unlikeComment(userId, commentId);

    return ResponseEntity.ok().build();
  }

  @GetMapping
  public ResponseEntity<List<CommentResponse>> getComments(
      @RequestParam Long newsId,
      @RequestParam String sortType,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "10") int size
  ) {

    Object parsedCursor = parseCursor(sortType, cursor);

    List<Comment> comments = commentService.getComments(
        newsId,
        sortType,
        parsedCursor,
        size
    );

    List<CommentResponse> response = comments.stream()
        .map(c -> CommentResponse.builder()
            .commentId(c.getId())
            .userId(c.getUserId())
            .content(c.getContent())
            .likeCount(c.getLikeCount())
            .createdAt(c.getCreatedAt())
            .build())
        .toList();

    return ResponseEntity.ok(response);
  }

  private Object parseCursor(String sortType, String cursor) {
    if (cursor == null) return null;

    if ("LATEST".equals(sortType)) {
      return LocalDateTime.parse(cursor);
    }

    if ("LIKE".equals(sortType)) {
      String[] parts = cursor.split(",");
      return new Long[]{
          Long.parseLong(parts[0]),
          Long.parseLong(parts[1])
      };
    }

    throw new IllegalArgumentException("잘못된 정렬 조건");
  }
}