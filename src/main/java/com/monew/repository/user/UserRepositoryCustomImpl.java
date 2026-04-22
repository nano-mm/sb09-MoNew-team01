package com.monew.repository.user;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {
  private final JdbcTemplate jdbcTemplate;

  @Override
  public int deleteSoftDeletedUsersOlderThan(Instant dateTime) {
    String sql = "DELETE FROM users WHERE is_deleted = true AND deleted_at < ?";
    return jdbcTemplate.update(sql, dateTime);
  }

  @Override
  public boolean existsInAllUsers(String email) {
    String sql = "SELECT EXISTS (SELECT 1 FROM users WHERE email = ?)";
    return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, email));
  }

  @Override
  public boolean existsByIdPhysical(UUID userId){
    String sql = "SELECT EXISTS (SELECT 1 FROM users WHERE id = CAST(? AS UUID))";
    return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, userId));
  }

  @Override
  public void deleteByIdPhysical(UUID userId) {
    String sql = "DELETE FROM users WHERE id = CAST(? AS UUID)";
    jdbcTemplate.update(sql, userId);
  }
}
