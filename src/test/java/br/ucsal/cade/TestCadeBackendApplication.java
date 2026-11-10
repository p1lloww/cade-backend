package br.ucsal.cade;

import org.springframework.boot.SpringApplication;

public class TestCadeBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(CadeBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
