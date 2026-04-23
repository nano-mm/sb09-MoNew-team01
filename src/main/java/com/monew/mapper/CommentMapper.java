package com.monew.mapper;

import com.monew.dto.response.CommentActivityDto;
import com.monew.dto.response.CommentLikeActivityDto;
import com.monew.dto.response.CommentResponse;
import com.monew.entity.Comment;
import com.monew.entity.CommentLike;
import com.monew.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {

  @Mapping(target = "id", source = "comment.id")
  @Mapping(target = "articleId", source = "comment.article.id")
  @Mapping(target = "userId", source = "user.id")
  @Mapping(target = "userNickname", source = "user.nickname")
  @Mapping(target = "content", source = "comment.content")
  @Mapping(target = "likeCount", source = "comment.likeCount")
  @Mapping(target = "createdAt", source = "comment.createdAt")
  @Mapping(target = "likedByMe", constant = "false")
  CommentResponse toResponse(Comment comment, User user);

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
