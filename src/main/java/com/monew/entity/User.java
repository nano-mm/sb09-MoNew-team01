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
import org.hibernate.annotations.SoftDelete;

@Entity
@Getter
@Table(name = "users")
@SoftDelete(columnName = "is_deleted")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User extends BaseUpdatableEntity {
  @Column(name = "email", unique = true, nullable = false)
  private String email;
  @Column(name = "nickname", nullable = false)
  private String nickname;
  @Column(name = "password", nullable = false)
  private String password;

  public static User to(String email, String nickname, String password) {
    return User.builder()
        .email(email)
        .nickname(nickname)
        .password(password)
        .build();
  }

  public User update(String nickname){
    this.nickname = nickname;
    return this;
  }
}
