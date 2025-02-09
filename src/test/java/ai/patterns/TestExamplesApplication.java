package ai.patterns;

import org.springframework.boot.SpringApplication;

public class TestExamplesApplication {

	public static void main(String[] args) {
		SpringApplication.from(AIPatternsWebApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
