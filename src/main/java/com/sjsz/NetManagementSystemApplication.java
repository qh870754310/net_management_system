package com.sjsz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class NetManagementSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(NetManagementSystemApplication.class, args);
	}
}
