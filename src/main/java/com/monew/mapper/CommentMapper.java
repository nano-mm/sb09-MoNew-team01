package com.monew.mapper;

import com.monew.dto.response.CommentActivityDto;
import com.monew.dto.response.CommentLikeActivityDto;
import com.monew.dto.response.CommentDto;
import com.monew.domain.model.Comment;
import com.monew.domain.model.CommentLike;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {

  @Mapping(target = "likedByMe", constant = "false")
  CommentDto toResponse(Comment comment);

  CommentActivityDto toActivityDto(Comment comment);

  @Mapping(target = "commentId", source = "comment.id")
  @Mapping(target = "commentUserId", source = "comment.user.id")
  @Mapping(target = "commentUserNickname", source = "comment.user.nickname")
  @Mapping(target = "commentContent", source = "comment.content")
  @Mapping(target = "commentLikeCount", source = "comment.likeCount")
  @Mapping(target = "commentCreatedAt", source = "comment.createdAt")
  @Mapping(target = "articleId", source = "comment.article.id")
  @Mapping(target = "articleTitle", source = "comment.article.title")
  CommentLikeActivityDto toLikeActivityDto(CommentLike commentLike);
}
