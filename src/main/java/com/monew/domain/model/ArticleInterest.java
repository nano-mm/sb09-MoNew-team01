package com.monew.domain.model;

import com.monew.domain.model.base.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "article_interests",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_article_interest",
            columnNames = {"article_id", "interest_id"}
        )
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ArticleInterest extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "article_id")
  private Article article;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "interest_id")
  private Interest interest;

  public static ArticleInterest of(Article article, Interest interest){
    return ArticleInterest.builder()
        .article(article)
        .interest(interest)
        .build();
  }

  public void generateIdForBulkInsert() {
    if (this.id == null) {
      this.id = UUID.randomUUID();
    }
  }
}
