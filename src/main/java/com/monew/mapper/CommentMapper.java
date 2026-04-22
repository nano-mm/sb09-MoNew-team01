package com.monew.mapper;

import com.monew.dto.response.CommentResponse;
import com.monew.entity.Comment;
import com.monew.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {

  @Mapping(target = "userId", source = "comment.userId")
  @Mapping(target = "userNickname", source = "user.nickname")
  @Mapping(target = "likeCount", source = "comment.likeCount")
  @Mapping(target = "likedByMe", constant = "false")
  CommentResponse toResponse(Comment comment, User user);
}
