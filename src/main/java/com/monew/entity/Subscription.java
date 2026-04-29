package com.monew.entity;

import com.monew.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "subscriptions", uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "interest_id"})})
@NoArgsConstructor
public class Subscription extends BaseEntity {

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;

  @JoinColumn(nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private Interest interest;

  public Subscription(User user, Interest interest) {
    this.user = user;
    this.interest = interest;
  }
}