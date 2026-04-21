package com.monew.repository;

import com.monew.entity.Interest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InterestRepository extends JpaRepository<Interest, UUID> {
  @Query("""
    SELECT DISTINCT i FROM Interest i
    LEFT JOIN i.keywords k
    WHERE i.name LIKE %:keyword%
    OR k LIKE %:keyword%
""")
  List<Interest> search(String keyword);


  // 뉴스 기사 수집할 때 쓸 쿼리
  @Query("SELECT DISTINCT i FROM Interest i LEFT JOIN FETCH i.keywords")
  List<Interest> findAllWithKeywords();
}