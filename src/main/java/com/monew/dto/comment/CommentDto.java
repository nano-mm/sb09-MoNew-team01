package com.monew.dto.comment;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {

  private UUID id;

  private UUID articleId;

  private UUID userId;

  private String userNickname;

  private String content;

  private long likeCount;

  private boolean likedByMe;

  private Instant createdAt;
}