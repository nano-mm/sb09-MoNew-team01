package com.monew.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UpdateCommentRequest {

  @NotBlank
  @Size(max = 500)
  private String content;
}
