package org.mnuykin;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class CashApplication {
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(CashApplication.class, args);
    }
}