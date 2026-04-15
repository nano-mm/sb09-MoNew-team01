package com.monew.entity;

import com.monew.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Getter
@NoArgsConstructor
public class Interest extends BaseEntity {

  @Column(nullable = false, length = 50, name = "name")
  private String name;

  private long subscriberCount;

  @ElementCollection
  private List<String> keywords = new ArrayList<>();

  public Interest(String name, List<String> keywords) {
    this.name = name;
    this.keywords = keywords;
    this.subscriberCount = 0;
  }
  public void updateKeywords(List<String> keywords) {
    this.keywords = keywords;
  }

  public void increaseSubscriber() {
    this.subscriberCount++;
  }

  public void decreaseSubscriber() {
    this.subscriberCount--;
  }
}