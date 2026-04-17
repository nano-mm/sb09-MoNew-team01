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
  @Column(name = "is_deleted")
  private boolean isDeleted = false;

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
  }
}

/* 아래 부분은 PR 날아오는 것을 확인하고서 작업 진행할 예정

  // 사용자가 작성한 구독 목록
  @Builder.Default
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Subscription> subscriptions = new ArrayList<>();

  // 사용자가 쓴 댓글 목록
  @Builder.Default
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Comment> comments = new ArrayList<>();

  // 사용자가 누른 댓글 좋아요 목록
  @Builder.Default
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CommentLike> commentLikes = new ArrayList<>();

  // 사용자의 게시글 조회 기록
  @Builder.Default
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ArticleView> articleViews = new ArrayList<>();

  // 사용자의 알림 목록
  @Builder.Default
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Notification> notifications = new ArrayList<>();

   */
