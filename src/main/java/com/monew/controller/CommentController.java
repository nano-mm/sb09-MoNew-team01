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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

  private final CommentService commentService;

  @PostMapping
  public ResponseEntity<UUID> createComment(
      @RequestHeader("Monew-Request-User-ID") UUID userId,
      @RequestBody @Valid CreateCommentRequest request
  ) {
    log.info("댓글 생성 요청 수신: userId={}", userId);
    UUID commentId = commentService.createComment(userId, request.articleId(), request.content());
    log.debug("댓글 생성 요청 처리 완료: commentId={}", commentId);
    return ResponseEntity.ok(commentId);
  }

  @PatchMapping("/{commentId}")
  public ResponseEntity<Void> updateComment(
      @RequestHeader("Monew-Request-User-ID") UUID userId,
      @PathVariable UUID commentId,
      @RequestBody @Valid UpdateCommentRequest request
  ) {
    log.info("댓글 수정 요청 수신: userId={}, commentId={}", userId, commentId);
    commentService.updateComment(userId, commentId, request.content());
    log.debug("댓글 수정 요청 처리 완료: commentId={}", commentId);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{commentId}")
  public ResponseEntity<Void> deleteComment(
      @RequestHeader("Monew-Request-User-ID") UUID userId,
      @PathVariable UUID commentId
  ) {
    log.info("댓글 삭제 요청 수신: userId={}, commentId={}", userId, commentId);
    commentService.deleteComment(userId, commentId);
    log.debug("댓글 삭제 요청 처리 완료: commentId={}", commentId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{commentId}/likes")
  public ResponseEntity<Void> like(
      @RequestHeader("Monew-Request-User-ID") UUID userId,
      @PathVariable UUID commentId
  ) {
    log.info("댓글 좋아요 요청 수신: userId={}, commentId={}", userId, commentId);
    commentService.likeComment(userId, commentId);
    log.debug("댓글 좋아요 요청 처리 완료: commentId={}", commentId);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{commentId}/likes")
  public ResponseEntity<Void> unlike(
      @RequestHeader("Monew-Request-User-ID") UUID userId,
      @PathVariable UUID commentId
  ) {
    log.info("댓글 좋아요 취소 요청 수신: userId={}, commentId={}", userId, commentId);
    commentService.unlikeComment(userId, commentId);
    log.debug("댓글 좋아요 취소 요청 처리 완료: commentId={}", commentId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping
  public ResponseEntity<CursorPageResponseDto<CommentResponse>> getComments(
      @RequestHeader("Monew-Request-User-ID") UUID userId,
      @RequestParam UUID articleId,
      @RequestParam CommentSortType sortType,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "10") int size
  ) {
    log.info("댓글 목록 조회 요청 수신: userId={}, articleId={}", userId, articleId);
    CursorPageResponseDto<CommentResponse> response =
        commentService.getComments(articleId, userId, sortType, cursor, size);
    log.debug("댓글 목록 조회 요청 처리 완료: articleId={}", articleId);
    return ResponseEntity.ok(response);
  }
}