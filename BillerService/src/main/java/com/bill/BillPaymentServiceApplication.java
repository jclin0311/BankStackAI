package com.bill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.bill", "com.commons"})
@EnableFeignClients(basePackages = "com.bill")
@EnableKafka
@EnableScheduling
public class BillPaymentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BillPaymentServiceApplication.class, args);
	}

}
