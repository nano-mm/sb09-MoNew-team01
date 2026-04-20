package com.monew.repository;

import com.monew.entity.ArticleInterest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ArticleInterestRepository extends JpaRepository<ArticleInterest, UUID> {
  @Query("SELECT ai FROM ArticleInterest ai JOIN FETCH ai.interest")
  List<ArticleInterest> findAllWithInterest();
}
