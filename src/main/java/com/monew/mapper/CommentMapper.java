package com.monew.mapper;

import com.monew.dto.response.CommentResponse;
import com.monew.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {

  @Mapping(target = "likeCount", expression = "java(comment.getLikes().size())")
  CommentResponse toResponse(Comment comment);
}
