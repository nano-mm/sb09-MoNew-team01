package com.monew;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
<<<<<<< HEAD
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
=======
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
>>>>>>> origin/develop
@SpringBootApplication
public class MonewApplication {

	public static void main(String[] args) {
		SpringApplication.run(MonewApplication.class, args);
	}

}
