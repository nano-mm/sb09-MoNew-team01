package com.monew.entity;

import com.monew.entity.base.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "users")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User extends BaseUpdatableEntity {
  @Column(name = "email")
  private String email;
  @Column(name = "nickname")
  private String nickname;
  @Column(name = "password")
  private String password;
  @Column(name = "isDeleted")
  private Boolean isDeleted;

  public static User to(String email, String nickname, String password) {
    return User.builder()
        .email(email)
        .nickname(nickname)
        .password(password)
        .isDeleted(false)
        .build();
  }
}
