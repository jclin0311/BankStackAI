package com.bankstack;

import com.commons.exception.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.bankstack.mcp.client")
@Import(GlobalExceptionHandler.class)
public class BankStackMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankStackMcpServerApplication.class, args);
    }

}
