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
select distinct i from Interest i
left join i.keywords k
where (:keyword is null 
   or i.name like concat('%', :keyword, '%')
   or k like concat('%', :keyword, '%')
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
      @Param("after") Instant after,
      Pageable pageable
  );

  @Query("""
select distinct i from Interest i
left join i.keywords k
where (:keyword is null 
   or i.name like concat('%', :keyword, '%')
   or k like concat('%', :keyword, '%')
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
      @Param("after") Instant after,
      Pageable pageable
  );

  @Query("""
select distinct i from Interest i
left join i.keywords k
where (:keyword is null 
   or i.name like concat('%', :keyword, '%')
   or k like concat('%', :keyword, '%')
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
      @Param("after") Instant after,
      Pageable pageable
  );

  @Query("""
select distinct i from Interest i
left join i.keywords k
where (:keyword is null 
   or i.name like concat('%', :keyword, '%')
   or k like concat('%', :keyword, '%')
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
      @Param("after") Instant after,
      Pageable pageable
  );

}