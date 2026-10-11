package org.mnuykin;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class CashApplication {
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(CashApplication.class, args);
    }
}