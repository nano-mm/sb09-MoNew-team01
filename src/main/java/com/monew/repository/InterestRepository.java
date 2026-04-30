package com.monew.repository;

import com.monew.entity.Interest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterestRepository extends JpaRepository<Interest, UUID> {

  @Query("""
select distinct i from Interest i
left join fetch i.keywords k
where (:keyword is null 
   or lower(i.name) like lower(concat('%', :keyword, '%'))
   or lower(k) like lower(concat('%', :keyword, '%'))
)
and (:cursor is null 
   or (i.name > :cursor 
       or (i.name = :cursor and i.createdAt > :after))
)
order by i.name asc, i.createdAt asc
""")
  List<Interest> findByNameAsc(
      @Param("keyword") String keyword,
      @Param("cursor") String cursor,
      @Param("after") LocalDateTime after,
      Pageable pageable
  );

  @Query("""
select distinct i from Interest i
left join fetch i.keywords k
where (:keyword is null 
   or lower(i.name) like lower(concat('%', :keyword, '%'))
   or lower(k) like lower(concat('%', :keyword, '%'))
)
and (:cursor is null 
   or (i.name < :cursor 
       or (i.name = :cursor and i.createdAt > :after))
)
order by i.name desc, i.createdAt asc
""")
  List<Interest> findByNameDesc(
      @Param("keyword") String keyword,
      @Param("cursor") String cursor,
      @Param("after") LocalDateTime after,
      Pageable pageable
  );

  @Query("""
select distinct i from Interest i
left join fetch i.keywords k
where (:keyword is null 
   or lower(i.name) like lower(concat('%', :keyword, '%'))
   or lower(k) like lower(concat('%', :keyword, '%'))
)
and (:cursor is null 
   or (i.subscriberCount > :cursor 
       or (i.subscriberCount = :cursor and i.createdAt > :after))
)
order by i.subscriberCount asc, i.createdAt asc
""")
  List<Interest> findBySubscriberAsc(
      @Param("keyword") String keyword,
      @Param("cursor") Long cursor,
      @Param("after") LocalDateTime after,
      Pageable pageable
  );

  @Query("""
select distinct i from Interest i
left join fetch i.keywords k
where (:keyword is null 
   or lower(i.name) like lower(concat('%', :keyword, '%'))
   or lower(k) like lower(concat('%', :keyword, '%'))
)
and (:cursor is null 
   or (i.subscriberCount < :cursor 
       or (i.subscriberCount = :cursor and i.createdAt > :after))
)
order by i.subscriberCount desc, i.createdAt asc
""")
  List<Interest> findBySubscriberDesc(
      @Param("keyword") String keyword,
      @Param("cursor") Long cursor,
      @Param("after") LocalDateTime after,
      Pageable pageable
  );


  // 뉴스 기사 수집할 때 쓸 쿼리
  @Query("SELECT DISTINCT i FROM Interest i LEFT JOIN FETCH i.keywords")
  List<Interest> findAllWithKeywords();

  @Modifying
  @Query(value = "UPDATE interests SET subscriber_count = (SELECT COUNT(*) FROM subscriptions WHERE interest_id = :id) WHERE id = :id", nativeQuery = true)
  int updateSubscriberCount(@Param("id") UUID id, @Param("count") long count);
}