package com.monew.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateCommentRequest(

    @NotNull
    UUID articleId,
    @NotNull
    UUID userId,

    @NotBlank
    @Size(max = 1000)
    String content

) {}
