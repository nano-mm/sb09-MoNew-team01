package com.monew.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comments",
    indexes = {
        @Index(name = "idx_news_created", columnList = "articleId, createdAt DESC"),
        @Index(name = "idx_news_like", columnList = "articleId, likeCount DESC, id DESC")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long userId;

  private Long articleId;

  @Column(nullable = false, length = 500)
  private String content;

  private int likeCount;

  private boolean deleted;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // 생성자
  public Comment(Long userId, Long articleId, String content) {
    this.userId = userId;
    this.articleId = articleId;
    this.content = content;
    this.likeCount = 0;
    this.deleted = false;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  // 비즈니스 로직
  public void updateContent(String content) {
    this.content = content;
    this.updatedAt = LocalDateTime.now();
  }

  public void delete() {
    this.deleted = true;
  }

  public void increaseLikeCount() {
    this.likeCount++;
  }

  public void decreaseLikeCount() {
    this.likeCount--;
  }
}
