package com.monew.mapper;

import com.monew.dto.response.UserDto;
import com.monew.entity.User;
import java.time.Instant;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-17T15:22:56+0900",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.4.jar, environment: Java 17.0.18 (Amazon.com Inc.)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserDto toDto(User user) {
        if ( user == null ) {
            return null;
        }

        UUID id = null;
        String email = null;
        String nickname = null;
        Instant createdAt = null;

        id = user.getId();
        email = user.getEmail();
        nickname = user.getNickname();
        createdAt = user.getCreatedAt();

        UserDto userDto = new UserDto( id, email, nickname, createdAt );

        return userDto;
    }
}
