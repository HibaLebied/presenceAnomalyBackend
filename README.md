# Attendance Anomaly Backend

Backend Spring Boot de l'application de gestion des anomalies de presence. Il synchronise des donnees de pointage depuis une base RH source vers une base dediee au dashboard, detecte des anomalies selon des regles metier, puis expose une API REST securisee pour le frontend.

## Fonctionnalites

- Synchronisation asynchrone par lots entre une vue RH source et la base dashboard.
- Synchronisation incrementale basee sur `updated_at` et un curseur compose de `updatedAt`, `matricule` et `dtPointage`.
- Historique de synchronisation : progression, compteurs d'insertions et de mises a jour, erreurs et reprise apres arret.
- Detection d'anomalies : pointage impair, pointage ou absence non saisie, absence non saisie et heures supplementaires non declarees.
- Dashboard avec statistiques par departement et classement des superviseurs.
- Recherche filtree et paginee des anomalies, avec activation ou mise a l'ecart d'une anomalie pour les administrateurs RH.
- Authentification JWT avec refresh token, roles `ADMIN_RH` et `MANAGER`.
- Documentation interactive de l'API avec Springdoc OpenAPI / Swagger UI.

## Stack technique

- Java 21
- Spring Boot 3.3.5
- Spring Web, Spring Data JPA, Spring Security et Spring Validation
- Microsoft SQL Server
- Maven
- JWT avec JJWT
- Springdoc OpenAPI

## Architecture

Le projet utilise deux sources de donnees SQL Server :

- `source` : lecture seule de la vue RH `dbo.hr_anomaly_view`.
- `dashboard` : stockage des pointages synchronises, anomalies, utilisateurs, refresh tokens et historique de synchronisation.

Les configurations sont separees dans `SourceDataSourceConfig` et `DashboardDataSourceConfig`, avec leurs propres repositories, entites et gestionnaires de transactions.

## Synchronisation

Le lancement se fait avec `POST /api/synchronization/run`.

1. Le backend verifie qu'aucune synchronisation n'est deja en cours.
2. Il cree une entree `RUNNING` dans `synchronization_history`.
3. Il charge la derniere synchronisation `SUCCESS` ou `PARTIAL_SUCCESS` pour determiner le point de reprise.
4. Il lit la vue source par lots, de taille 500 par defaut.
5. Chaque record est insere ou mis a jour dans `attendance_records` grace a son `sourceRecordId`.
6. Les anomalies des records synchronises sont recalculees.
7. Apres chaque lot, le curseur et les compteurs sont enregistres dans l'historique.
8. Le job est marque `SUCCESS` ou `FAILED` a sa fin.

La borne haute de lecture correspond a l'heure de demarrage de la synchronisation. Ainsi, les modifications arrivees pendant le traitement seront prises en compte lors de la synchronisation suivante.

## Securite

- Les mots de passe sont verifies avec BCrypt.
- Les routes protegees utilisent un access token JWT transmis dans le header `Authorization: Bearer <token>`.
- Les refresh tokens sont transmis dans un cookie HTTP-only. Seul leur hash SHA-256 est stocke en base.
- Les refresh tokens sont renouveles lors de `/auth/refresh` et revoques lors de `/auth/logout`.
- Les autorisations sont controlees selon les roles `ADMIN_RH` et `MANAGER`.

## Endpoints principaux

| Methode | Endpoint | Description |
| --- | --- | --- |
| POST | `/api/auth/login` | Authentifie un utilisateur et retourne un JWT. |
| POST | `/api/auth/refresh` | Renouvelle le JWT avec le refresh token. |
| POST | `/api/auth/logout` | Revoque le refresh token et supprime le cookie. |
| GET | `/api/auth/me` | Retourne l'utilisateur authentifie. |
| POST | `/api/synchronization/run` | Lance une synchronisation, reserve a `ADMIN_RH`. |
| GET | `/api/synchronization/{id}` | Retourne le statut d'une synchronisation. |
| GET | `/api/dashboard` | Retourne les indicateurs du dashboard. |
| GET | `/api/anomalies` | Recherche les anomalies avec filtres et pagination. |
| PATCH | `/api/anomalies/{id}/ignore` | Met une anomalie a l'ecart, reserve a `ADMIN_RH`. |
| PATCH | `/api/anomalies/{id}/activate` | Reactive une anomalie, reserve a `ADMIN_RH`. |

## Configuration locale

Les valeurs sensibles et les URLs de bases sont fournies par variables d'environnement :

```text
APP_DB_URL
APP_DB_USERNAME
APP_DB_PASSWORD
SOURCE_DB_URL
SOURCE_DB_USERNAME
SOURCE_DB_PASSWORD
JWT_SECRET
```

Variables optionnelles :

```text
JWT_EXPIRATION_MS
REFRESH_TOKEN_EXPIRATION_MS
REFRESH_TOKEN_COOKIE_PATH
REFRESH_TOKEN_COOKIE_SECURE
REFRESH_TOKEN_COOKIE_SAME_SITE
```

La taille des lots de synchronisation est configurable dans `application.yml` :

```yaml
attendance:
  synchronization:
    batch-size: 500
```

## Lancer le projet

```bash
mvn spring-boot:run
```

L'API est exposee par defaut sur `http://localhost:8080/api`.

Swagger UI est disponible sur :

```text
http://localhost:8080/api/swagger-ui.html
```

## Tests

```bash
mvn test
```
