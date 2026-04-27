package com.monew.mongo.repository;

import com.monew.mongo.document.UserActivityDocument;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserActivityDocumentRepository
    extends MongoRepository<UserActivityDocument, UUID> {
}
