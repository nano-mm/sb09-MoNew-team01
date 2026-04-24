package com.monew.controller;

import com.monew.config.LoginUser;
import com.monew.dto.comment.CommentSortType;
import com.monew.dto.request.CommentResponseDto;
import com.monew.dto.request.CreateCommentRequest;
import com.monew.dto.request.UpdateCommentRequest;
import com.monew.dto.response.CommentDto;
import com.monew.dto.response.CommentLikeResponse;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.service.CommentService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

  private final CommentService commentService;

  @PostMapping
  public ResponseEntity<CommentDto> createComment(
      @RequestBody @Valid CreateCommentRequest request
  ) {
    log.info("댓글 생성 요청 수신: userId={}", request.userId());
    CommentDto commentDto =
        commentService.createComment(request.userId(), request.articleId(), request.content());
    log.debug("댓글 생성 요청 처리 완료: commentId={}", commentDto.id());
    return ResponseEntity.status(HttpStatus.CREATED).body(commentDto);
  }

  @GetMapping
  public ResponseEntity<CursorPageResponseDto<CommentDto>> getComments(
      @LoginUser UUID userId,
      @ModelAttribute CommentResponseDto requestDto
  ) {
    UUID articleId = requestDto.articleId();
    log.info("댓글 목록 조회 요청 수신: userId={}, articleId={}", userId, articleId);
    CursorPageResponseDto<CommentDto> responseDto = commentService.getComments(requestDto, userId);
    log.debug("댓글 목록 조회 요청 처리 완료: articleId={}", articleId);
    return ResponseEntity.ok(responseDto);
  }

  @PatchMapping("/{commentId}")
  public ResponseEntity<CommentDto> updateComment(
      @LoginUser UUID userId,
      @PathVariable UUID commentId,
      @RequestBody @Valid UpdateCommentRequest request
  ) {
    log.info("댓글 수정 요청 수신: userId={}, commentId={}", userId, commentId);
    CommentDto commentDto = commentService.updateComment(userId, commentId,
        request.content());
    log.debug("댓글 수정 요청 처리 완료: commentId={}", commentId);
    return ResponseEntity.ok(commentDto);
  }

  // 논리 삭제 (Soft Delete) — isDeleted 플래그만 true로 변경
  @DeleteMapping("/{commentId}")
  public ResponseEntity<Void> deleteComment(
      @LoginUser UUID userId,
      @PathVariable UUID commentId
  ) {
    log.info("댓글 논리 삭제 요청 수신: userId={}, commentId={}", userId, commentId);
    commentService.deleteComment(userId, commentId);
    log.debug("댓글 논리 삭제 요청 처리 완료: commentId={}", commentId);
    return ResponseEntity.noContent().build();
  }

  // 물리 삭제 (Hard Delete) — DB에서 실제로 제거
  @DeleteMapping("/{commentId}/hard")
  public ResponseEntity<Void> hardDeleteComment(
      @LoginUser UUID userId,
      @PathVariable UUID commentId
  ) {
    log.info("댓글 물리 삭제 요청 수신: userId={}, commentId={}", userId, commentId);
    commentService.hardDeleteComment(userId, commentId);
    log.debug("댓글 물리 삭제 요청 처리 완료: commentId={}", commentId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{commentId}/comment-likes")
  public ResponseEntity<CommentLikeResponse> like(
      @LoginUser UUID userId,
      @PathVariable UUID commentId
  ) {
    log.info("댓글 좋아요 요청 수신: userId={}, commentId={}", userId, commentId);
    CommentLikeResponse response = commentService.likeComment(userId, commentId);
    log.debug("댓글 좋아요 요청 처리 완료: commentId={}", commentId);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{commentId}/comment-likes")
  public ResponseEntity<Void> unlike(
      @LoginUser UUID userId,
      @PathVariable UUID commentId
  ) {
    log.info("댓글 좋아요 취소 요청 수신: userId={}, commentId={}", userId, commentId);
    commentService.unlikeComment(userId, commentId);
    log.debug("댓글 좋아요 취소 요청 처리 완료: commentId={}", commentId);
    return ResponseEntity.noContent().build();
  }
}
