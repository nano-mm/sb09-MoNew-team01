package com.monew.entity;

import com.monew.entity.enums.ArticleSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "articles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@SQLRestriction("is_deleted = false")
public class Article {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(name = "source", nullable = false)
  private ArticleSource source;

  @Column(name = "source_url", nullable = false, length = 500)
  private String sourceUrl;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(name = "publish_date", nullable = false)
  private Instant publishDate;

  @Column(columnDefinition = "TEXT")
  private String summary;

  @Builder.Default
  @Column(name = "comment_count", nullable = false)
  private Long commentCount = 0L;

  // 임시
  @Column(name = "interest_id")
  private UUID interestId;

  @Builder.Default
  @Column(name = "view_count", nullable = false)
  private Long viewCount = 0L;

  @Builder.Default
  @Column(name = "is_deleted", nullable = false)
  private Boolean isDeleted = false;

  public void markAsDeleted() {
    this.isDeleted = true;
  }

}
