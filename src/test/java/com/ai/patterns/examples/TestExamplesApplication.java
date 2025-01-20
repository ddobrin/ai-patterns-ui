package com.ai.patterns.examples;

import org.springframework.boot.SpringApplication;

public class TestExamplesApplication {

	public static void main(String[] args) {
		SpringApplication.from(ExamplesApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
