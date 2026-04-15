package com.monew.entity;

import com.monew.entity.base.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Subscription extends BaseEntity {

  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY)
  private Interest interest;

  public Subscription(UUID userId, Interest interest) {
    this.userId = userId;
    this.interest = interest;
  }
}