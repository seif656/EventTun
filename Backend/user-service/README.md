# User Service

Spring Boot / Java 17 service on port 8081, using an in-memory H2 database.
Data is reset when the application stops.

## Run and test

From this directory, with Java 17 configured:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Swagger UI: http://localhost:8081/swagger-ui/index.html
Generated OpenAPI: http://localhost:8081/v3/api-docs

## User operations

| Method | Path | Result |
|---|---|---|
| POST | /api/users | 201 + Location |
| GET | /api/users | 200 |
| GET | /api/users/{id} | 200 or 404 |
| PUT | /api/users/{id} | 200, 400, 404 or 409 |
| PATCH | /api/users/{id}/status | 200, 400 or 404 |

Create or replace a profile:

```json
{"firstName":"Seif","lastName":"Test","email":"seif@example.com"}
```

Names must not be blank, email must be valid, and text fields accept at most
255 characters. Emails are stored lowercase and are unique ignoring case.
Optional fields: role and keycloakSubject. New users are ACTIVE; an omitted
role defaults to STUDENT. PUT replaces profile fields (omitted role becomes
STUDENT and omitted keycloakSubject becomes null), preserving account status.

Block: PATCH /api/users/1/status with `{"status":"BLOCKED"}`.
Reactivate: the same endpoint with `{"status":"ACTIVE"}`.
There is no delete operation. Errors use application/problem+json.

Request DTOs are simple Java input classes that keep IDs and account status
out of profile creation/update input. The controller handles HTTP, the service
contains rules, and the repository accesses H2.

Keycloak and authorization remain planned;
role selection and administrative operations are currently for the local
course exercise. Blocking records status; other services must check it when
those services are implemented.

## Club operations

| Method | Path | Success |
|---|---|---|
| POST | /api/clubs | 201 + Location |
| GET | /api/clubs | 200 |
| GET | /api/clubs/{id} | 200 |
| PUT | /api/clubs/{id} | 200 |
| DELETE | /api/clubs/{id} | 204 |

First create a user with role ORGANIZER, then use its ID:

```json
{"name":"Robotics Club","description":"University robotics club","ownerId":1}
```

The name is required and cannot be blank. Name and description are limited to
255 characters. ownerId must be positive and refer to an existing ORGANIZER.
A missing club or owner returns 404; a non-organizer owner returns 409;
invalid input returns 400. createdAt is assigned by the server and preserved
on update. PUT replaces the profile, including ownerId; omitted description
is cleared. DELETE removes the club, not its owner. Event-service references
will need coordination once that service is implemented.
