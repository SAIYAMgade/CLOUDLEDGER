package com.cloudledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CloudLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudLedgerApplication.class, args);
    }
}
