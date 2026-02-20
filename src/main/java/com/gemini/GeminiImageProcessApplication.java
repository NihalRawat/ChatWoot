package com.gemini;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class GeminiImageProcessApplication {

	public static void main(String[] args) {
		System.err.println("==============================================");
		SpringApplication.run(GeminiImageProcessApplication.class, args);
		System.err.println("====================== Started ========================");
	}

}
