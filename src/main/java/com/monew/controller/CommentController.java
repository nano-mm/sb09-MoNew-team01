package com.monew.controller;

import com.monew.dto.comment.CommentSortType;
import com.monew.dto.request.CreateCommentRequest;
import com.monew.dto.request.UpdateCommentRequest;
import com.monew.dto.response.CommentResponse;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.service.CommentService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    UUID commentId = commentService.createComment(
        getUserId(),
        request.articleId(),
        request.content()
    );
    return ResponseEntity.ok(commentId);
  }

  @PatchMapping("/{commentId}")
  public ResponseEntity<Void> updateComment(
      @PathVariable UUID commentId,
      @RequestBody @Valid UpdateCommentRequest request
  ) {
    commentService.updateComment(getUserId(), commentId, request.getContent());
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{commentId}")
  public ResponseEntity<Void> deleteComment(@PathVariable UUID commentId) {
    commentService.deleteComment(getUserId(), commentId);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{commentId}/like")
  public ResponseEntity<Void> like(@PathVariable UUID commentId) {
    commentService.likeComment(getUserId(), commentId);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{commentId}/like")
  public ResponseEntity<Void> unlike(@PathVariable UUID commentId) {
    commentService.unlikeComment(getUserId(), commentId);
    return ResponseEntity.ok().build();
  }

  @GetMapping
  public ResponseEntity<CursorPageResponseDto<CommentResponse>> getComments(
      @RequestParam UUID articleId,
      @RequestParam CommentSortType sortType,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "10") int size
  ) {
    return ResponseEntity.ok(
        commentService.getComments(articleId, sortType, cursor, size)
    );
  }
}