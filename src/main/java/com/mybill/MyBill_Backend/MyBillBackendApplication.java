package com.mybill.MyBill_Backend;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableAsync
public class MyBillBackendApplication {

    public static void main(String[] args) {
        String configuredTimeZone = System.getenv().getOrDefault(
                "APP_TIME_ZONE",
                "Asia/Kolkata"
        );
        TimeZone.setDefault(TimeZone.getTimeZone(configuredTimeZone));
        SpringApplication.run(MyBillBackendApplication.class, args);
    }
}
