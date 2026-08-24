package com.company.attendance.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;
import java.util.Objects;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.company.attendance.repository.source",
        entityManagerFactoryRef = "sourceEntityManagerFactory",
        transactionManagerRef = "sourceTransactionManager"
)
public class SourceDataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.source")
    public DataSourceProperties sourceDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.source.hikari")
    public HikariDataSource sourceDataSource() {
        return sourceDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean sourceEntityManagerFactory(
            EntityManagerFactoryBuilder builder
    ) {
        return builder
                .dataSource(sourceDataSource())
                .packages("com.company.attendance.entity.source")
                .persistenceUnit("source")
                .properties(jpaProperties())
                .build();
    }

    @Bean
    public PlatformTransactionManager sourceTransactionManager(
            LocalContainerEntityManagerFactoryBean sourceEntityManagerFactory
    ) {
        return new JpaTransactionManager(
                Objects.requireNonNull(sourceEntityManagerFactory.getObject())
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
