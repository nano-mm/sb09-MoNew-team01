package com.monew.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.monew.dto.response.UserActivityDto;
import com.monew.entity.User;
import com.monew.mongo.repository.UserActivityDocumentRepository;
import com.monew.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "MONGODB_URI", matches = ".+")
class UserActivityMongoIntegrationTest {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserActivityReadModelService userActivityReadModelService;

  @Autowired
  private UserActivityDocumentRepository userActivityDocumentRepository;

  private UUID savedUserId;

  @AfterEach
  void cleanUp() {
    if (savedUserId != null) {
      userActivityDocumentRepository.deleteById(savedUserId);
      userRepository.deleteById(savedUserId);
    }
  }

  @Test
  @DisplayName("사용자 활동 스냅샷을 MongoDB에 저장하고 다시 조회할 수 있다")
  void refreshSnapshot_StoresAndReadsFromMongo() {
    User savedUser = userRepository.save(User.of("mongo-test@monew.com", "mongoTester", "pass!123"));
    savedUserId = savedUser.getId();

    userActivityReadModelService.refreshSnapshot(savedUserId);
    Optional<UserActivityDto> result = userActivityReadModelService.findByUserId(savedUserId);

    assertTrue(result.isPresent());
    assertEquals(savedUserId, result.get().id());
    assertEquals("mongo-test@monew.com", result.get().email());
    assertTrue(userActivityDocumentRepository.existsById(savedUserId));
  }
}
