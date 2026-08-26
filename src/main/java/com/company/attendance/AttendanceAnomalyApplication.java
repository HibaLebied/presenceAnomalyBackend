package com.company.attendance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class AttendanceAnomalyApplication {

    public static void main(String[] args) {
        SpringApplication.run(AttendanceAnomalyApplication.class, args);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        System.out.println(encoder.encode("Admin123!"));
        System.out.println(encoder.encode("Manager123!"));
    }
}
