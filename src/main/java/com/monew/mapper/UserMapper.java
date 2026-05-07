package com.monew.mapper;

import com.monew.dto.response.UserDto;
import com.monew.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
  UserDto toDto(User user);
}
