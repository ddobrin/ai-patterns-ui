package ai.patterns.dbingestion;

import ai.patterns.DbingestionApplication;
import org.springframework.boot.SpringApplication;

public class TestDbingestionApplication {

	public static void main(String[] args) {
		SpringApplication.from(DbingestionApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
