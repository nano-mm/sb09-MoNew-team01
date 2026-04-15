package com.monew.service;

import com.monew.entity.Comment;
import com.monew.entity.CommentLike;
import com.monew.repository.CommentLikeRepository;
import com.monew.repository.CommentRepository;
import java.util.List;
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
  public Long createComment(Long userId, Long newsId, String content) {

    Comment comment = new Comment(userId, newsId, content);

    commentRepository.save(comment);

    return comment.getId();
  }

  @Transactional
  public void updateComment(Long userId, Long commentId, String content) {

    Comment comment = getActiveComment(commentId);

    if (!comment.getUserId().equals(userId)) {
      throw new ForbiddenException();
    }

    comment.updateContent(content);
  }

  @Transactional
  public void deleteComment(Long userId, Long commentId) {

    Comment comment = getActiveComment(commentId);

    if (!comment.getUserId().equals(userId)) {
      throw new ForbiddenException();
    }

    comment.delete();
  }

  @Transactional
  public void likeComment(Long userId, Long commentId) {

    Comment comment = getActiveComment(commentId);

    if (commentLikeRepository.existsByUserIdAndCommentId(userId, commentId)) {
      throw new DuplicateLikeException();
    }

    commentLikeRepository.save(new CommentLike(userId, commentId));

    comment.increaseLikeCount();
  }

  @Transactional
  public void unlikeComment(Long userId, Long commentId) {

    Comment comment = getActiveComment(commentId);

    commentLikeRepository.deleteByUserIdAndCommentId(userId, commentId);

    comment.decreaseLikeCount();
  }

  public List<Comment> getComments(
      Long newsId,
      String sortType,
      Object cursor,
      int size
  ) {
    return commentRepository.findByNewsIdWithCursor(newsId, sortType, cursor, size);
  }

  private Comment getActiveComment(Long commentId) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(CommentNotFoundException::new);

    if (comment.isDeleted()) {
      throw new CommentNotFoundException();
    }

    return comment;
  }

}
