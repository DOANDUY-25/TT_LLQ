package com.pmh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class PmhBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PmhBackendApplication.class, args);
	}

}
