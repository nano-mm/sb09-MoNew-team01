package com.monew.entity;

import com.monew.entity.base.BaseUpdatableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Table(name = "users")
@SQLRestriction("is_deleted = false")
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
  @Builder.Default
  @Column(name = "is_deleted")
  private boolean isDeleted = false;
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

  public void markAsDeleted(boolean isDeleted) {
    this.isDeleted = isDeleted;
    if (isDeleted) {
      this.deletedAt = Instant.now();
    } else {
      this.deletedAt = null;
    }
  }
}
