package com.monew.dto.response;

import com.monew.dto.comment.CommentDto;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentCursorResponse {

  private List<CommentDto> content;

  private String nextCursor;

  private Instant nextAfter;

  private int size;

  private long totalElements;

  private boolean hasNext;
}
