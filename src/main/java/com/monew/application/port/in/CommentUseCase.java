package com.monew.application.port.in;

import com.monew.dto.request.CommentResponseDto;
import com.monew.dto.response.CommentDto;
import com.monew.dto.response.CommentLikeResponse;
import com.monew.dto.response.CursorPageResponseDto;
import java.util.UUID;

public interface CommentUseCase {
  CommentDto createComment(UUID userId, UUID articleId, String content);
  CommentDto updateComment(UUID userId, UUID commentId, String content);
  void deleteComment(UUID commentId);
  void hardDeleteComment(UUID commentId);
  CommentLikeResponse likeComment(UUID userId, UUID commentId);
  void unlikeComment(UUID userId, UUID commentId);
  CursorPageResponseDto<CommentDto> getComments(CommentResponseDto requestDto, UUID userId);
}
