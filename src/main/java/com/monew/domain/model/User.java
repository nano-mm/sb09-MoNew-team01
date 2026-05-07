package com.monew.domain.model;

import com.monew.domain.model.base.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "users")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User extends BaseUpdatableEntity {
  @Column(name = "email", unique = true, nullable = false)
  private String email;
  @Column(name = "nickname", nullable = false, length = 20)
  private String nickname;
  @Column(name = "password", nullable = false)
  private String password;
  @Column(name = "deleted_at")
  private Instant deletedAt;

  public static User of(String email, String nickname, String password) {
    return User.builder()
        .email(email)
        .nickname(nickname)
        .password(password)
        .build();
  }

  public void update(String nickname){
    this.nickname = nickname;
  }

  public void markAsDeleted(Instant time) {
    this.deletedAt = time;
  }
}
