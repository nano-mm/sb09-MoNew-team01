package com.monew.dto.request;

import org.hibernate.validator.constraints.Length;

public record UserUpdateRequest(
    @Length(min = 1, max = 20) String nickname
) {

}
