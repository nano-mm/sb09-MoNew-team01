package com.monew.adapter.out.mongo.repository;

import com.monew.adapter.out.mongo.document.UserActivityDocument;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

public interface UserActivityDocumentRepository
    extends MongoRepository<UserActivityDocument, UUID> {

    @Query("{ '_id': ?0 }")
    @Update("{ '$pull': { 'subscriptions': { 'interestId': ?1 } } }")
    void removeSubscription(UUID userId, UUID interestId);
}
