package com.monew.repository;

import com.monew.entity.User;
import com.monew.repository.user.UserRepositoryCustom;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID>, UserRepositoryCustom {
  Optional<User> findByEmail(@Param("email") String email);
}
