package com.monew.entity;

import com.monew.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "subscriptions")
@NoArgsConstructor
public class Subscription extends BaseEntity {

  @Column(nullable = false, name = "userId")
  private UUID userId;

  @JoinColumn(nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private Interest interest;

  public Subscription(UUID userId, Interest interest) {
    this.userId = userId;
    this.interest = interest;
  }
}