# ticket-service (M5)

Microservice **Ticket Service** d'EventTun : emission, consultation et validation des billets numeriques.

| | |
|---|---|
| Port | 8085 |
| Base | MongoDB `tickets_db` (collections `tickets`, `ticket_validations`) |
| Stack | Java 17+, Spring Boot 3.3, Spring Web, Spring Data MongoDB, springdoc-openapi |

> **Validation 2** : les six operations sont implementees (emission idempotente, validation atomique
> VALID -> USED, annulation, historique). Erreurs au format Problem Details.
> MongoDB doit etre lance pour appeler les endpoints.

## Prerequis
- JDK 17 ou plus
- Maven (ou le Maven integre a IntelliJ)
- MongoDB (necessaire pour appeler les endpoints) : `docker run -d -p 27017:27017 --name mongo mongo`

## Lancer
```bash
cd Backend/ticket-service
mvn spring-boot:run
```
L'application demarre meme si MongoDB n'est pas lance (les appels renvoient alors 503). Pour creer l'index unique sur `uniqueCode`
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
| GET | /api/tickets?ownerId=&orderId= | Billets d'un utilisateur / d'une commande (200, 400 sans filtre) |
| POST | /api/tickets/{id}/validations | Valider un billet (201, 404, 409) |
| GET | /api/tickets/{id}/validations | Historique des validations (200, 404) |
| PATCH | /api/tickets/{id}/status | Annuler un billet (200, 400, 404, 409) |

## Tests
```bash
mvn test
```
Les tests unitaires simulent MongoDB : aucune base n'est necessaire.

## Structure
```
src/main/java/com/eventtun/ticket/
  controller/  TicketController      (endpoints + annotations Swagger)
  service/     TicketService         (logique metier)
  repository/  TicketRepository, TicketValidationRepository
  model/       Ticket, TicketValidation, TicketStatus, ValidationResult
  dto/         requetes + format d'erreur Problem Details
  exception/   erreurs metier + GlobalExceptionHandler (Problem Details)
  config/      OpenApiConfig
```
