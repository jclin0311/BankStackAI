package com.payments.orch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.payments", "com.commons"})
@EnableFeignClients
@EnableKafka
@EnableScheduling
public class PaymentOrchestratorApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentOrchestratorApplication.class, args);
	}

}
