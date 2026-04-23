package com.monew.mapper;

import com.monew.dto.response.CommentActivityDto;
import com.monew.dto.response.CommentLikeActivityDto;
import com.monew.dto.response.CommentResponse;
import com.monew.entity.Comment;
import com.monew.entity.CommentLike;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {

  @Mapping(target = "userNickname", source = "user.nickname")
  @Mapping(target = "likedByMe", ignore = true)
  @Mapping(target = "likeCount", source = "likeCount")
  CommentResponse toResponse(Comment comment);

  @Mapping(source = "article.id", target = "articleId")
  @Mapping(source = "article.title", target = "articleTitle")
  @Mapping(source = "user.id", target = "userId")
  @Mapping(source = "user.nickname", target = "userNickname")
  CommentActivityDto toActivityDto(Comment comment);

  @Mapping(source = "comment.id", target = "commentId")
  @Mapping(source = "comment.article.id", target = "articleId")
  @Mapping(source = "comment.article.title", target = "articleTitle")
  @Mapping(source = "comment.user.id", target = "commentUserId")
  @Mapping(source = "comment.user.nickname", target = "commentUserNickname")
  @Mapping(source = "comment.content", target = "commentContent")
  @Mapping(source = "comment.likeCount", target = "commentLikeCount")
  @Mapping(source = "comment.createdAt", target = "commentCreatedAt")
  CommentLikeActivityDto toLikeActivityDto(CommentLike commentLike);
}
