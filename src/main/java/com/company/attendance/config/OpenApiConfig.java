package com.company.attendance.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI attendanceAnomalyOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Attendance Anomaly Management API")
                        .version("v1")
                        .description("Backend API documentation for Attendance Anomaly Management."));
    }
}
