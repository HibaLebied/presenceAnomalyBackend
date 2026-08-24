# Attendance Anomaly Backend

Initial Spring Boot backend structure for Attendance Anomaly Management.

## Current Phase

This phase creates only the backend structure and configuration.

Implemented:

- Maven project
- Java 21 / Spring Boot 3 setup
- Package organization
- Springdoc OpenAPI configuration
- Global exception handling
- `application.yml` template
- Logging configuration

Not implemented yet:

- Entities
- Repositories
- Controllers
- Synchronization
- Anomaly detection
- Statistics

## Package Overview

- `config`: technical Spring configuration.
- `controller`: REST controllers for future dashboard endpoints.
- `service`: application services and use-case orchestration.
- `repository`: Spring Data JPA repositories in future phases.
- `entity`: JPA entities in future phases.
- `dto`: request and response DTOs.
- `mapper`: mapping between DTOs, entities, and application models.
- `synchronization`: synchronization workflow contracts and services.
- `detection`: anomaly detection rules and engine.
- `statistics`: dynamic dashboard statistics.
- `exception`: global exception handling.
- `util`: small shared technical helpers.
