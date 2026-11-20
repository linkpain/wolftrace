package com.linkpin.wolftrace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WolftraceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WolftraceApplication.class, args);
        System.out.println("Wolftrace Server started successfully!");
    }
}
