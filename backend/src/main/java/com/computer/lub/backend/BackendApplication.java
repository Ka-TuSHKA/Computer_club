package com.computer.lub.backend;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // <-- ИМПОРТ

@SpringBootApplication
@EnableScheduling
public class BackendApplication {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Ulyanovsk"));
        SpringApplication.run(BackendApplication.class, args);
    }
}
