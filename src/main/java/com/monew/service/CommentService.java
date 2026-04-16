package com.monew.service;

import com.monew.dto.comment.CommentCursor;
import com.monew.dto.comment.CommentSortType;
import com.monew.entity.Comment;
import com.monew.entity.CommentLike;
import com.monew.exception.CommentNotFoundException;
import com.monew.exception.DuplicateLikeException;
import com.monew.exception.ForbiddenException;
import com.monew.repository.CommentLikeRepository;
import com.monew.repository.CommentRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

  private final CommentRepository commentRepository;
  private final CommentLikeRepository commentLikeRepository;

  @Transactional
  public UUID createComment(UUID userId, UUID articleId, String content) {

    Comment comment = Comment.create(articleId.toString(), userId, content);

    commentRepository.save(comment);

    return comment.getId();
  }

  @Transactional
  public void updateComment(UUID userId, UUID commentId, String content) {

    Comment comment = getActiveComment(commentId);

    if (!comment.getUserId().equals(userId)) {
      throw new ForbiddenException();
    }

    comment.updateContent(content);
  }

  @Transactional
  public void deleteComment(UUID userId, UUID commentId) {

    Comment comment = getActiveComment(commentId);

    if (!comment.getUserId().equals(userId)) {
      throw new ForbiddenException();
    }

    comment.softDelete();
  }

  @Transactional
  public void likeComment(UUID userId, UUID commentId) {
    Comment comment = getActiveComment(commentId);
    if (commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
      throw new DuplicateLikeException();
    }
    commentLikeRepository.save(new CommentLike(comment, userId));
    comment.increaseLikeCount();
  }

  @Transactional
  public void unlikeComment(UUID userId, UUID commentId) {
    Comment comment = getActiveComment(commentId);
    commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
    comment.decreaseLikeCount();
  }

  public List<Comment> getComments(
      UUID articleId,
      CommentSortType sortType,
      CommentCursor cursor,
      int size
  ) {

    return commentRepository.findByArticleIdWithCursor(
        articleId.toString(), sortType, cursor, size
    );
  }

  private Comment getActiveComment(UUID commentId) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(CommentNotFoundException::new);

    if (comment.isDeleted()) {
      throw new CommentNotFoundException();
    }

    return comment;
  }

}
