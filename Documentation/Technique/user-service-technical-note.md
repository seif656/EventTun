
# 9. User Service â€” User and Club Management

## 9.1. Overview and Responsibilities

User Service is responsible for managing user profiles
and university clubs within the EventTun platform.

Its main responsibilities are:

- Maintaining student, organizer, controller and administrator profiles.
- Managing user information and account status.
- Creating and managing university clubs.
- Associating clubs with their responsible organizers.
- Providing user and club information to other microservices.

User Service is responsible for business profiles, while
authentication and identity management are handled by
Keycloak in the target architecture.

Passwords are not stored in the User Service database.

## 9.2. Functional Scope

User Service provides two main functional areas.

### User Management

- Create a user profile.
- List registered users.
- Retrieve a user by identifier.
- Update user information.
- Block or reactivate a user account.
- Associate a user profile with a Keycloak identity.

### Club Management

- Create a university club.
- List university clubs.
- Retrieve club details.
- Update club information.
- Associate a club with an organizer.

Each club has one responsible organizer, while an
organizer may manage several clubs.

Account authentication, ticket reservations, payments
and event publication are outside the responsibilities
of User Service.

## 9.3. Architecture and Technologies

User Service is developed as an independent
Spring Boot application using Java 17 and Maven.

### Technical Configuration

| Component | Technology |
|-----------|------------|
| Application framework | Spring Boot |
| Programming language | Java 17 |
| Build tool | Maven |
| REST API | Spring Web |
| Persistence | Spring Data JPA |
| Local database | H2 |
| Target database | MySQL |
| API specification | OpenAPI 3.0 |
| Monitoring | Spring Boot Actuator |
| Application port | 8081 |

### Internal Architecture

The service is organized into separate layers:

- Controller: defines REST API entry points.
- Service: defines business operations.
- Repository: provides database access.
- Entity: represents the persistent data model.

This organization separates HTTP request handling,
business responsibilities and persistence concerns.

### Database Independence

User Service owns the users_db database.

Other microservices do not access its tables directly.
They exchange information with User Service through
REST APIs.

MySQL is the target relational database because
user profiles and clubs require structured data,
unique identifiers and relational consistency.

H2 is currently used for local development and
application startup verification.

## 9.4. Data Model and Entities

### Entity: User

The User entity represents a profile registered
on EventTun.

| Attribute | Type | Description |
|-----------|------|-------------|
| id | Long | Internal user identifier |
| firstName | String | User first name |
| lastName | String | User last name |
| email | String | Unique email address |
| keycloakSubject | String | External identity reference |
| role | Role | User role |
| status | UserStatus | Account status |

Supported roles:

- STUDENT
- ORGANIZER
- CONTROLLER
- ADMIN

Supported account statuses:

- ACTIVE
- BLOCKED

The email attribute has a uniqueness constraint.

The keycloakSubject attribute is reserved for
integration with the identity provider.

### Entity: Club

The Club entity represents a university club
managed by an organizer.

| Attribute | Type | Description |
|-----------|------|-------------|
| id | Long | Club identifier |
| name | String | Club name |
| description | String | Club description |
| ownerId | Long | Responsible user's ID |
| createdAt | LocalDateTime | Creation timestamp |

### Entity Relationship

The relationship between User and Club is 1:N.

One user can be responsible for multiple clubs,
while each club has one responsible user.

The ownerId attribute identifies the associated
user within the User Service database.

The current model stores this identifier directly.
A JPA relationship mapping may be added during
subsequent development.

## 9.5. UML Class Diagram

The class diagram describes the User and Club
entities, their attributes and their relationship.

Diagram file:

[User Service Class Diagram](../Diagrams/user-service-classes.md)

The diagram also includes the Role and UserStatus
enumerations.

## 9.6. REST API Contract

User Service exposes two API resource groups:

- /api/users
- /api/clubs

### User Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/users | Create a user |
| GET | /api/users | List users |
| GET | /api/users/{id} | Retrieve user |
| PUT | /api/users/{id} | Update user |
| PATCH | /api/users/{id}/status | Update account status |

### Club Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/clubs | Create a club |
| GET | /api/clubs | List clubs |
| GET | /api/clubs/{id} | Retrieve club |
| PUT | /api/clubs/{id} | Update club |
| DELETE | /api/clubs/{id} | Delete club |

### HTTP Response Codes

The planned API contract defines the following codes:

- 200 OK: successful retrieval or update.
- 201 Created: resource created.
- 400 Bad Request: invalid request data.
- 404 Not Found: requested resource does not exist.
- 409 Conflict: conflicting data or constraints.

Error responses are specified using the
Problem Details format.

Complete API specification:

[User Service OpenAPI Contract](../OpenAPI/user-service.yaml)

The OpenAPI contract describes the intended API.
User and Club operations are implemented.

## 9.7. Interactions with Other Microservices

User Service provides profile and club information
to other components of EventTun.

### Interaction with Order Service

Before processing a ticket order, Order Service
must verify the user's existence and account status.

Planned interaction:

1. Order Service receives a ticket order request.
2. Order Service calls GET /api/users/{id}.
3. User Service returns the user profile.
4. Order Service checks the account status.
5. Order Service continues only if the user is
   authorized to proceed.

Data exchanged:

- User identifier.
- User role.
- Account status.

Communication mode: synchronous HTTP/REST.

This operation is synchronous because the order
workflow requires the verification result before
continuing.

If the user does not exist, the verification
returns a 404 response.

If the user is blocked, Order Service must reject
the purchase according to its authorization rules.

### Interaction with Event Service

Event Service maintains event information and
stores the identifier of the organizing club.

When necessary, Event Service may verify the
existence of a club through User Service.

Planned interaction:

GET /api/clubs/{id}

Data exchanged:

- Club identifier.
- Club name.
- Responsible organizer identifier.

Communication mode: synchronous HTTP/REST.

This verification is optional in the initial
integration scope.

### Frontend Interaction

The frontend communicates with User Service
through the API Gateway in the target architecture.

The frontend uses the REST API to display profiles,
manage account information and access club details.

### Service Independence

User Service does not access the databases of
Event Service, Order Service, Payment Service
or Ticket Service.

Each microservice remains responsible for its own
business data.

## 9.8. Risks and Design Considerations

### Service Unavailability

Order Service depends on user verification during
the purchase workflow.

If User Service is unavailable, the order process
must not treat a network failure as proof that
the user does not exist.

The planned solution includes request timeouts,
controlled retries and explicit handling of
temporary service failures.

### Data Consistency

An event can reference a club by its identifier.

Changes to club information must not require
direct access to the User Service database.

Identifiers and REST contracts provide the
integration boundary between services.

### Duplicate User Profiles

The email field has a uniqueness constraint.

The planned API returns HTTP 409 when a request
conflicts with existing unique data.

### Authorization

Administrative functions such as listing users
and blocking accounts require access control.

Authentication and role-based authorization will
be integrated using Keycloak.

These security restrictions are part of the
target design and are not yet enforced.

### External Identity Integration

User Service uses its own internal identifiers.

The keycloakSubject field provides a reference
to identities managed by Keycloak.

This separates profile management from
authentication responsibilities.

## 9.9. Implementation Status

### Implemented Structure

- Spring Boot application initialized.
- Java 17 and Maven configuration.
- Application configured on port 8081.
- User and Club entity classes.
- Role and UserStatus enumerations.
- UserRepository and ClubRepository.
- UserService and ClubService interfaces.
- UserController and ClubController skeletons.
- H2 database configuration.
- Spring Boot Actuator configuration.

### Prepared Documentation

- UML class diagram.
- REST API contract in OpenAPI format.
- Technical architecture and data model.
- Interservice interaction design.
- Failure scenarios and integration constraints.

### Remaining Development

- Integrate Club references with Event Service when available.
- Coordinate club deletion with future event references.
- Enforce access control when authentication is integrated.
- Integrate authentication and authorization.
- Configure the target MySQL database.
- Implement interservice communications.
- Execute functional and integration tests.

User endpoints now implement create, list, lookup, full profile update and
block/reactivate. Validation and Problem Details errors are implemented, and
Swagger UI is available. No delete endpoint is provided.
Club endpoints implement create, list, lookup, update and delete. Owners must
exist and be organizers; creation timestamps are managed by the server.
See the service README for defaults, PUT behavior, and test instructions.
