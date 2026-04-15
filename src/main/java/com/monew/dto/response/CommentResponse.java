package com.monew.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentResponse {

  private Long commentId;
  private Long userId;
  private String content;
  private int likeCount;
  private LocalDateTime createdAt;

}
