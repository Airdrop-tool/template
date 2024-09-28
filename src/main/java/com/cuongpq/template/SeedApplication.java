package com.cuongpq.template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SeedApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeedApplication.class, args);
    }
}
