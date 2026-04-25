package com.monew.mongo.repository;

import com.monew.mongo.document.UserActivityDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserActivityDocumentRepository extends MongoRepository<UserActivityDocument, String> {
}
