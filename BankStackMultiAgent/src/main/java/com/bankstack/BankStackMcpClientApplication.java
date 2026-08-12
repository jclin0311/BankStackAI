package com.bankstack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BankStackMcpClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankStackMcpClientApplication.class, args);
    }
}