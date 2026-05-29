package com.vr.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.vr")
@MapperScan("com.vr")
@EnableScheduling
public class VRApplication {
    public static void main(String[] args) {
        SpringApplication.run(VRApplication.class, args);
    }
}
