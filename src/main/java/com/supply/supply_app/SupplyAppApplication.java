package com.supply.supply_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.supply")
public class SupplyAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(SupplyAppApplication.class, args);
	}

}
