package com.company.attendance.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;
import java.util.Objects;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.company.attendance.repository.dashboard",
        entityManagerFactoryRef = "dashboardEntityManagerFactory",
        transactionManagerRef = "dashboardTransactionManager"
)
public class DashboardDataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.dashboard")
    public DataSourceProperties dashboardDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.dashboard.hikari")
    public HikariDataSource dashboardDataSource() {
        return dashboardDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean dashboardEntityManagerFactory(
            EntityManagerFactoryBuilder builder
    ) {
        return builder
                .dataSource(dashboardDataSource())
                .packages("com.company.attendance.entity.dashboard")
                .persistenceUnit("dashboard")
                .properties(jpaProperties())
                .build();
    }

    @Bean
    @Primary
    public PlatformTransactionManager dashboardTransactionManager(
            LocalContainerEntityManagerFactoryBean dashboardEntityManagerFactory
    ) {
        return new JpaTransactionManager(
                Objects.requireNonNull(dashboardEntityManagerFactory.getObject())
        );
    }

    private Map<String, Object> jpaProperties() {
        return Map.of(
                "hibernate.hbm2ddl.auto", "none",
                "hibernate.format_sql", "true",
                "hibernate.show_sql", "false",
                "hibernate.jdbc.time_zone", "UTC"
        );
    }
}
