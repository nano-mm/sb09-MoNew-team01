package com.monew.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentResponse {

  private UUID commentId;
  private UUID userId;
  private String content;
  private int likeCount;
  private LocalDateTime createdAt;

}
