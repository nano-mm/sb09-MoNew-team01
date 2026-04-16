package com.monew.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Getter;

@Getter
public class CreateCommentRequest {

  @NotNull
  private UUID articleId;

  @NotBlank
  @Size(max = 1000)
  private String content;
}
