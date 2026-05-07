package com.monew.domain.model;

import com.monew.domain.model.base.BaseEntity;
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @JoinColumn(name = "interest_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private Interest interest;

  public Subscription(User user, Interest interest) {
    this.user = user;
    this.interest = interest;
  }
}