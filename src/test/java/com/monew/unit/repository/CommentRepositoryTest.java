package com.monew.unit.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.monew.config.JpaAuditConfig;
import com.monew.config.TestQueryDslConfig;
import com.monew.dto.comment.CommentCursor;
import com.monew.dto.comment.CommentSortType;
import com.monew.domain.model.Article;
import com.monew.domain.model.Comment;
import com.monew.domain.model.CommentLike;
import com.monew.domain.model.User;
import com.monew.adapter.out.persistence.CommentLikeRepository;
import com.monew.adapter.out.persistence.CommentRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({TestQueryDslConfig.class, JpaAuditConfig.class})
class CommentRepositoryTest {

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private CommentLikeRepository commentLikeRepository;

  @Autowired
  private EntityManager em;

  private User user;
  private Article article;

  @BeforeEach
  void setUp() {
    user = User.of("test@test.com", "tester", "password123!");
    em.persist(user);

    article = Article.builder()
        .source(com.monew.domain.model.enums.ArticleSource.NAVER)
        .sourceUrl("http://link.com")
        .title("제목")
        .summary("요약")
        .publishDate(java.time.Instant.now())
        .build();
    em.persist(article);
    em.flush();
  }

  @Test
  @DisplayName("댓글을 물리 삭제하면 연관된 좋아요 데이터도 삭제되어야 한다 (Cascade)")
  void hardDeleteWithLikes() {
    // given
    Comment comment = Comment.create(article, user, "댓글 내용");
    commentRepository.save(comment);

    CommentLike like = new CommentLike(comment, user);
    commentLikeRepository.save(like);
    
    em.flush();
    em.clear();

    // when
    Comment foundComment = commentRepository.findById(comment.getId()).orElseThrow();
    commentRepository.delete(foundComment);
    em.flush();

    // then
    assertThat(commentRepository.findById(comment.getId())).isEmpty();
    assertThat(commentLikeRepository.findById(like.getId())).isEmpty();
  }

  @Test
  @DisplayName("커서 기반으로 댓글 목록을 조회한다 - 첫 페이지 (최신순)")
  void findByArticleIdWithCursor_FirstPage() {
    // Given
    Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    Comment comment1 = Comment.create(article, user, "댓글1");
    Comment comment2 = Comment.create(article, user, "댓글2");
    commentRepository.saveAll(List.of(comment1, comment2));

    setField(comment1, "createdAt", now.minusSeconds(60));
    setField(comment2, "createdAt", now);

    em.flush();
    em.clear();

    // When
    List<Comment> results = commentRepository.findByArticleIdWithCursor(
        article.getId(), CommentSortType.CREATED_AT, null, 10);

    // Then
    assertThat(results).hasSize(2);
    assertThat(results.get(0).getContent()).isEqualTo("댓글2");
    assertThat(results.get(1).getContent()).isEqualTo("댓글1");
  }

  @Test
  @DisplayName("생성일 기준 커서가 있을 때 다음 페이지를 조회한다")
  void findByArticleIdWithCreatedAtCursor() {
    // Given - 명시적으로 시간을 지정
    Instant now = Instant.parse("2026-04-30T10:00:00Z");

    Comment comment1 = Comment.create(article, user, "댓글1");
    Comment comment2 = Comment.create(article, user, "댓글2");

    setField(comment1, "createdAt", now.minusSeconds(10));
    setField(comment2, "createdAt", now);

    commentRepository.saveAll(List.of(comment1, comment2));

    em.flush();
    em.clear();

    Comment savedComment2 = commentRepository.findById(comment2.getId()).get();
    CommentCursor cursor = new CommentCursor(
        savedComment2.getId(),
        savedComment2.getCreatedAt(),
        0
    );

    // When
    List<Comment> results = commentRepository.findByArticleIdWithCursor(
        article.getId(),
        CommentSortType.CREATED_AT,
        cursor,
        10
    );

    // Then
    assertThat(results)
        .extracting(Comment::getContent)
        .containsExactly("댓글1");
  }

  @Test
  @DisplayName("좋아요 순 및 커서를 기반으로 댓글 목록을 조회한다")
  void findByArticleIdWithLikeCountCursor() {
    // given
    Comment comment1 = Comment.create(article, user, "댓글1");
    Comment comment2 = Comment.create(article, user, "댓글2");
    // Reflection으로 좋아요 수 강제 설정
    setField(comment1, "likeCount", 10);
    setField(comment2, "likeCount", 10);

    commentRepository.saveAll(List.of(comment1, comment2));
    em.flush();
    em.clear();

    // 1. 첫 페이지 조회 (좋아요 순)
    List<Comment> firstPage = commentRepository.findByArticleIdWithCursor(
        article.getId(), CommentSortType.LIKE_COUNT, null, 1);

    // 2. 커서 생성 (첫 번째 결과의 좋아요 수와 ID 사용)
    CommentCursor cursor = new CommentCursor(
        firstPage.get(0).getId(),
        firstPage.get(0).getCreatedAt(),
        firstPage.get(0).getLikeCount()
    );

    // when
    List<Comment> secondPage = commentRepository.findByArticleIdWithCursor(
        article.getId(), CommentSortType.LIKE_COUNT, cursor, 1);

    // then
    assertThat(secondPage).hasSize(1);
    assertThat(secondPage.get(0).getId()).isNotEqualTo(firstPage.get(0).getId());
  }
}
