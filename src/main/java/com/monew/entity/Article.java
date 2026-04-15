package com.monew.entity;

import com.monew.entity.base.BaseEntity;
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
public class Article extends BaseEntity {
  @Enumerated(EnumType.STRING)
  @Column(length = 50)
  private ArticleSource source;

  @Column(name = "source_url", nullable = false, columnDefinition = "TEXT")
  private String sourceUrl;

  @Column(nullable = false, length = 500)
  private String title;

  @Column(name = "publish_date")
  private Instant publishDate;

  @Column(columnDefinition = "TEXT")
  private String summary;

  @Builder.Default
  @Column(name = "comment_count", nullable = false)
  private Long commentCount = 0L;

  @Builder.Default
  @Column(name = "view_count", nullable = false)
  private Long viewCount = 0L;

  @Builder.Default
  @Column(name = "is_deleted", nullable = false)
  private Boolean isDeleted = false;

}
