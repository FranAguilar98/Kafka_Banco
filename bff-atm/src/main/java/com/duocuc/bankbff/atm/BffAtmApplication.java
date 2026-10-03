package com.duocuc.bankbff.atm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.duocuc.bankbff.atm", "com.duocuc.bankbff.core"})
public class BffAtmApplication {
    public static void main(String[] args) {
        SpringApplication.run(BffAtmApplication.class, args);
    }
}