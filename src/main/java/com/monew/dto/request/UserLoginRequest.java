package com.monew.dto.request;

import jakarta.validation.constraints.Email;
import org.hibernate.validator.constraints.Length;

public record UserLoginRequest(
    @Email String email,
    @Length(min = 6, max = 20) String password
) {

}
