package com.monew.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CreateCommentRequest {

  @NotNull
  private Long newsId;

  @NotBlank
  @Size(max = 500)
  private String content;
}
