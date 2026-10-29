package etude_cas_asin.asin_test;

import org.springframework.boot.SpringApplication;

public class TestAsinTestApplication {

	public static void main(String[] args) {
		SpringApplication.from(AsinTestApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
