package com.vyankatesh.resumeoptimizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ResumeoptimizerApplication {

	public static void main(String[] args) {
		SpringApplication.run(
				ResumeoptimizerApplication.class,
				args
		);
	}
}