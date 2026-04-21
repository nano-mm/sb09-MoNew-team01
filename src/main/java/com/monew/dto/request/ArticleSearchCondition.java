package com.monew.dto.request;

import com.monew.entity.enums.ArticleSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.springframework.format.annotation.DateTimeFormat;

@Builder(toBuilder = true)
public record ArticleSearchCondition(
  String keyword,
  UUID interestId,
  List<ArticleSource> sourceIn,
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  LocalDateTime publishDateFrom,

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  LocalDateTime publishDateTo
){
}
