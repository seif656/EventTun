# ticket-service (M5)

Microservice **Ticket Service** d'EventTun : emission, consultation et validation des billets numeriques.

| | |
|---|---|
| Port | 8085 |
| Base | MongoDB `tickets_db` (collections `tickets`, `ticket_validations`) |
| Stack | Java 17+, Spring Boot 3.3, Spring Web, Spring Data MongoDB, springdoc-openapi |

> **Validation 1** : projet demarrable, entites, endpoints documentes (Swagger).
> La logique metier est reportee : les endpoints repondent `501 Not Implemented`.

## Prerequis
- JDK 17 ou plus
- Maven (ou le Maven integre a IntelliJ)
- MongoDB (optionnel pour demarrer) : `docker run -d -p 27017:27017 --name mongo mongo`

## Lancer
```bash
cd Backend/ticket-service
mvn spring-boot:run
```
L'application demarre meme si MongoDB n'est pas lance. Pour creer l'index unique sur `uniqueCode`
(MongoDB lance) : `MONGO_AUTO_INDEX=true mvn spring-boot:run`.
Autre adresse MongoDB : variable `MONGODB_URI` (defaut `mongodb://localhost:27017/tickets_db`).

## Swagger / OpenAPI
- Swagger UI : http://localhost:8085/swagger-ui/index.html
- Contrat JSON : http://localhost:8085/v3/api-docs (a copier dans `Documentation/OpenAPI/` pour Swagger Editor)

## Endpoints
| Methode | Endpoint | Description |
|---|---|---|
| POST | /api/tickets | Emettre les billets d'une commande (201, 200 rejeu, 400, 409) |
| GET | /api/tickets/{id} | Consulter un billet (200, 404) |
| GET | /api/tickets?ownerId=&orderId= | Billets d'un utilisateur / d'une commande (200) |
| POST | /api/tickets/{id}/validations | Valider un billet (201, 404, 409) |
| GET | /api/tickets/{id}/validations | Historique des validations (200, 404) |
| PATCH | /api/tickets/{id}/status | Annuler un billet (200, 404, 409) |

## Structure
```
src/main/java/com/eventtun/ticket/
  controller/  TicketController      (endpoints + annotations Swagger)
  service/     TicketService         (logique metier : a implementer)
  repository/  TicketRepository, TicketValidationRepository
  model/       Ticket, TicketValidation, TicketStatus, ValidationResult
  dto/         requetes + format d'erreur Problem Details
  config/      OpenApiConfig
```
