package com.monew.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record InterestRegisterRequest(
    @NotBlank
    @Size(min = 1, max = 50, message = "관심사 이름은 1자 이상 50자 이하")
    String name,

    @NotNull
    @Size(min = 1, max = 10, message = "키워드는 최소 1개, 최대 10개까지 가능")
    List<@NotBlank String> keywords

){

}
