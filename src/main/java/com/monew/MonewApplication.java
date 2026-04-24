package com.monew;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(exclude = {
    JpaRepositoriesAutoConfiguration.class,
    MongoRepositoriesAutoConfiguration.class
})
public class MonewApplication {

	public static void main(String[] args) {
		SpringApplication.run(MonewApplication.class, args);
	}

}
