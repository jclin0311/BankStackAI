package com.billpay.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.commons", "com.billpay.worker"})
@EnableKafka
@EnableScheduling
public class BillPayWorkerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillPayWorkerServiceApplication.class, args);
    }
}