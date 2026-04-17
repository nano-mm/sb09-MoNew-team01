package com.monew.repository;

import com.monew.entity.Interest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterestRepository extends JpaRepository<Interest, UUID> {

  @Query("""
SELECT DISTINCT i FROM Interest i
LEFT JOIN i.keywords k
WHERE (:keyword IS NULL OR i.name LIKE %:keyword% OR k LIKE %:keyword%)
AND (
    :cursor IS NULL OR
    (:sort = 'name' AND (
        i.name > :cursor OR
        (i.name = :cursor AND i.createdAt > :after)
    )) OR
    (:sort = 'subscriberCount' AND (
        i.subscriberCount < :cursorCount OR
        (i.subscriberCount = :cursorCount AND i.createdAt > :after)
    ))
)
ORDER BY
    CASE WHEN :sort = 'name' THEN i.name END ASC,
    CASE WHEN :sort = 'subscriberCount' THEN i.subscriberCount END DESC,
    i.createdAt ASC
""")

  List<Interest> searchWithCursor(
      @Param("keyword") String keyword,
      @Param("sort") String sort,
      @Param("cursor") String cursor,
      @Param("cursorCount") Long cursorCount,
      @Param("after") Instant after,
      Pageable pageable
  );
}