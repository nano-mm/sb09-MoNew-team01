package com.monew.repository;

import com.monew.entity.ArticleInterest;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleInterestRepository extends JpaRepository<ArticleInterest, UUID> {

}
