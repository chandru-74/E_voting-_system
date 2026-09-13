package com.example.E_voting_System;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing  // ← add this
public class EVotingSystemApplication {
	public static void main(String[] args) {
		SpringApplication.run(EVotingSystemApplication.class, args);
	}
}