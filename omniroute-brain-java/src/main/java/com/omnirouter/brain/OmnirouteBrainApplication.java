package com.omnirouter.brain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OmnirouteBrainApplication {

	public static void main(String[] args) {
		SpringApplication.run(OmnirouteBrainApplication.class, args);
	}

}
