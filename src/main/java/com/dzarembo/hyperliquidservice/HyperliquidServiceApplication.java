package com.dzarembo.hyperliquidservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HyperliquidServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HyperliquidServiceApplication.class, args);
    }

}
