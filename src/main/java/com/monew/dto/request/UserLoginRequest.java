package com.monew.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record UserLoginRequest(
    @NotBlank @Email @Length ( min = 1 , max = 254 )  String email,
    @NotBlank @Length(min = 6, max = 20) String password
) {

}
