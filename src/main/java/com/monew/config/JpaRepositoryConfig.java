package com.monew.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.monew.adapter.out.persistence")
public class JpaRepositoryConfig {
}
