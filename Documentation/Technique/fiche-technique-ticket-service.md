# Fiche technique — Ticket Service

Responsable : M5 – fatma felhi

Validation 1 

## État d'avancement (validation 1)

Cette fiche distingue ce qui est *conçu*, *développé* et *testé*. Aucun résultat réel de test métier n'est présenté : la logique métier sera implémentée aux validations suivantes.

| Élément | Conçu | Développé | Testé |
|---|---|---|---|
| Projet Spring Boot `ticket-service` (port 8085) | Oui | Oui | Démarrage et Swagger UI vérifiés |
| Entités `Ticket` et `TicketValidation` | Oui | Oui (classes et annotations MongoDB) | Non |
| Repositories | Oui | Oui (interfaces) | Non |
| Contrat OpenAPI et Swagger (5 endpoints, 6 opérations) | Oui | Oui (annotations) | Affichage Swagger UI vérifié |
| Logique métier (émission, validation atomique, annulation) | Oui | Non (501 Not Implemented) | Non |
| Base MongoDB `tickets_db` et index unique | Oui | Non (index prévu) | Non |
| Scénarios de test (émission, validation, refus) | Oui (décrits) | Non | À exécuter après implémentation |

## 1. Nom du microservice

**Ticket Service** (artefact Maven : `ticket-service`, paquet `com.eventtun.ticket`).

## 2. Responsable

M5 – fatma felhi. M5 assure aussi la coordination du microservice commun Notification et l'assemblage du document technique et du PowerPoint.

## 3. Description et responsabilités

Le Ticket Service gère l'émission, la consultation et la validation des billets numériques d'EventTun. Une fois la commande payée, Order Service lui demande d'émettre les billets. Le jour de l'événement, un contrôleur valide chaque billet à l'entrée.

Responsabilités :

- émettre exactement le nombre de billets payés, chacun avec un code unique (`uniqueCode`) ;
- permettre de consulter les billets d'un utilisateur ou d'une commande ;
- valider un billet à l'entrée : un billet valide ne peut être utilisé qu'une seule fois ;
- conserver l'historique de chaque tentative de validation ;
- permettre l'annulation d'un billet non utilisé (annulation de commande).

Le Ticket Service n'appelle aucun autre microservice : il est uniquement appelé.

## 4. Fonctionnalités

| Réf. | Fonctionnalité | Règle principale |
|---|---|---|
| F1 | Émettre les billets d'une commande | Autant de billets que payés ; idempotent par `orderId` (200 si rejeu identique, 409 si contenu différent). |
| F2 | Consulter un billet | Recherche par identifiant ; 404 si introuvable. |
| F3 | Lister les billets | Filtres `ownerId` et/ou `orderId`. |
| F4 | Valider un billet à l'entrée | Passage atomique `VALID` → `USED` (une seule mise à jour conditionnelle) ; une seconde utilisation renvoie 409. |
| F5 | Consulter l'historique des validations | Toutes les tentatives, y compris les refus. |
| F6 | Annuler un billet | `VALID` → `CANCELLED` ; un billet `USED` ne peut pas être annulé (409). |

### Saisie ou scan du code par le contrôleur

Le billet possède un code unique (`uniqueCode`, par exemple `EVT12-7F3K9Q2A`). Le QR code graphique est une amélioration future ; pour l'instant, le contrôleur saisit ou scanne ce code. Déroulement prévu :

1. le contrôleur saisit (ou scanne) le `uniqueCode` du billet ;
2. le système retrouve le billet grâce à l'index unique sur `uniqueCode` (lecture directe, sans parcours de collection) ;
3. il appelle ensuite la validation du billet (`POST /api/tickets/{id}/validations`) ;
4. la réponse indique si l'accès est accepté (201) ou refusé (409 : déjà utilisé ou annulé).

> **Point à valider avec l'équipe :** le contrat actuel permet de lister par `ownerId` ou `orderId`, mais n'expose pas encore la recherche par code. Une solution simple est d'ajouter le filtre optionnel `uniqueCode` à `GET /api/tickets` (le repository prévoit déjà `findByUniqueCode`). Cet ajout n'est pas encore dans les contrats des autres membres.

## 5. Entités et attributs

### Entité `Ticket` (collection `tickets`)

| Attribut | Type | Description |
|---|---|---|
| `id` | String | Identifiant MongoDB (ObjectId). |
| `orderId` | Long | Commande à l'origine du billet (Order Service). Indexé. |
| `eventId` | Long | Événement concerné (Event Service). |
| `ownerId` | Long | Propriétaire du billet (User Service). Indexé. |
| `uniqueCode` | String | Code unique saisi ou scanné à l'entrée. Index unique. |
| `status` | Enum `TicketStatus` | `VALID`, `USED` ou `CANCELLED`. |
| `issuedAt` | LocalDateTime | Date d'émission (ISO 8601). |

### Entité `TicketValidation` (collection `ticket_validations`)

| Attribut | Type | Description |
|---|---|---|
| `id` | String | Identifiant MongoDB (ObjectId). |
| `ticketId` | String | Référence au document `Ticket`. Indexé. |
| `controllerId` | Long | Contrôleur ayant effectué la tentative (User Service). |
| `validationDate` | LocalDateTime | Date de la tentative (ISO 8601). |
| `result` | Enum `ValidationResult` | `ACCEPTED`, `REFUSED_ALREADY_USED` ou `REFUSED_CANCELLED`. |

**Relation :** un billet peut posséder plusieurs tentatives de validation (1 vers 0..*). Un billet valide ne doit pouvoir être utilisé qu'une seule fois.

**Cycle de vie :** `VALID` → `USED` (validation acceptée) ; `VALID` → `CANCELLED` (annulation). `USED` et `CANCELLED` sont des états finaux.

## 6. Diagramme de classes

![Diagramme de classes du Ticket Service](../Diagrams/classes-ticket.png)

*Figure 1 – Diagramme de classes du Ticket Service (fichier `Documentation/Diagrams/classes-ticket.png`).*

## 7. Base de données

**Base :** MongoDB, base `tickets_db`, indépendante des autres microservices (une base par service, pas de jointure entre services). MongoDB est la technologie avancée retenue par l'équipe pour M

### Pourquoi MongoDB pour les billets

- Un billet est un document autonome : il se lit et s'écrit en une seule opération, sans jointure.
- Les lectures se font surtout par code unique ; un index unique sur `uniqueCode` donne un accès direct et garantit qu'aucun code n'existe en double.
- L'historique des validations évolue (nouvelles informations de contrôle : porte d'entrée, appareil, etc.) : le schéma flexible évite les migrations.
- La validation atomique (`VALID` → `USED`) s'écrit comme une seule mise à jour conditionnelle sur un document : l'opération est atomique, ce qui évite que deux contrôleurs valident le même billet.

### Collections et index

| Collection | Contenu | Index |
|---|---|---|
| `tickets` | Un document par billet | `uniqueCode` (unique), `orderId`, `ownerId` |
| `ticket_validations` | Un document par tentative de validation | `ticketId` |

### Schéma des documents (exemple)

```json
// collection tickets
{
  "_id": "665f1c2e9a1b2c3d4e5f6a7b",
  "orderId": 101,
  "eventId": 12,
  "ownerId": 4,
  "uniqueCode": "EVT12-7F3K9Q2A",
  "status": "VALID",
  "issuedAt": "2026-11-14T22:30:00"
}

// collection ticket_validations
{
  "_id": "665f1d3b9a1b2c3d4e5f6a80",
  "ticketId": "665f1c2e9a1b2c3d4e5f6a7b",
  "controllerId": 7,
  "validationDate": "2026-12-01T18:45:00",
  "result": "ACCEPTED"
}
```

`orderId`, `eventId`, `ownerId` et `controllerId` sont des références logiques vers les autres microservices : le Ticket Service ne les résout pas lui-même et n'appelle personne.

**Mise en œuvre :** l'adresse de la base se règle par la variable `MONGODB_URI` (défaut : `mongodb://localhost:27017/tickets_db`). La création automatique des index est activée avec `MONGO_AUTO_INDEX=true` ; elle est désactivée par défaut pour que le service démarre même sans MongoDB en validation 1. L'installation complète de MongoDB n'est pas exigée pour cette validation.

## 8. Technologies

| Technologie | Version | Usage |
|---|---|---|
| Java | 17 | Langage |
| Spring Boot | 3.3.5 | Framework du microservice |
| Spring Web | — | API REST (controllers) |
| Spring Data MongoDB | — | Accès à MongoDB (repositories) |
| Bean Validation | — | Validation des corps de requête (`@Valid`, `@NotNull`) |
| springdoc-openapi | 2.6.0 | Génération du contrat OpenAPI et Swagger UI |
| Spring Boot Actuator | — | Endpoint de santé (`/actuator/health`) |
| Maven | — | Construction et dépendances |
| MongoDB | — | Base de données documentaire `tickets_db` |
| Git / GitHub | — | Branche `feature/ticket-service`, Pull Request |

## 9. Architecture interne du code

Le service suit l'architecture en trois couches Controller / Service / Repository. En validation 1, seule la structure est posée : la logique métier est reportée.

```text
Backend/ticket-service/
├── pom.xml
├── README.md
└── src/main/
    ├── resources/application.properties
    └── java/com/eventtun/ticket/
        ├── TicketServiceApplication.java
        ├── controller/   TicketController
        ├── service/      TicketService
        ├── repository/   TicketRepository, TicketValidationRepository
        ├── model/        Ticket, TicketValidation, TicketStatus, ValidationResult
        ├── dto/          IssueTicketsRequest, ValidateTicketRequest,
        │                 UpdateTicketStatusRequest, ProblemDetailResponse
        └── config/       OpenApiConfig
```

| Couche | Rôle | État |
|---|---|---|
| Controller | Reçoit les requêtes HTTP, valide les corps, renvoie les codes HTTP. Porte la documentation Swagger. | Endpoints déclarés ; renvoient 501 Not Implemented |
| Service | Logique métier : émission idempotente, validation atomique, annulation, historique. | Classe créée, méthodes à implémenter |
| Repository | Accès MongoDB (`findByOwnerId`, `findByOrderId`, `findByUniqueCode`, historique par `ticketId`). | Interfaces déclarées |
| Model / DTO | Documents MongoDB, corps de requête, format d'erreur. | Créés |

## 10. Endpoints REST

Base : `http://localhost:8085`. Les URI ne contiennent pas de verbe ; les champs JSON sont en camelCase et les dates en ISO 8601.

| Méthode | Endpoint | Description | Codes |
|---|---|---|---|
| POST | `/api/tickets` | Émettre les billets d'une commande | 201, 200 (rejeu identique), 400, 409 |
| GET | `/api/tickets/{id}` | Consulter un billet | 200, 404 |
| GET | `/api/tickets?ownerId=&orderId=` | Billets d'un utilisateur ou d'une commande | 200 |
| POST | `/api/tickets/{id}/validations` | Valider un billet à l'entrée | 201, 404, 409 |
| GET | `/api/tickets/{id}/validations` | Historique des validations | 200, 404 |
| PATCH | `/api/tickets/{id}/status` | Annuler un billet | 200, 404, 409 |

Pour annuler les billets d'une commande, Order filtre d'abord avec `GET /api/tickets?orderId=`, puis appelle `PATCH /api/tickets/{id}/status` pour chaque billet. Ticket n'appelle jamais Order.

## 11. Exemples de requêtes et réponses JSON

Ces exemples décrivent le comportement cible du contrat. Les valeurs sont des exemples ; ils seront exécutés après l'implémentation.

### 11.1 Émettre les billets d'une commande

`POST /api/tickets`

```json
{
  "orderId": 101,
  "eventId": 12,
  "ownerId": 4,
  "quantity": 2
}
```

Réponse **201 Created** :

```json
[
  {
    "id": "665f1c2e9a1b2c3d4e5f6a7b",
    "orderId": 101,
    "eventId": 12,
    "ownerId": 4,
    "uniqueCode": "EVT12-7F3K9Q2A",
    "status": "VALID",
    "issuedAt": "2026-11-14T22:30:00"
  },
  {
    "id": "665f1c2e9a1b2c3d4e5f6a7c",
    "orderId": 101,
    "eventId": 12,
    "ownerId": 4,
    "uniqueCode": "EVT12-B84M1XZ7",
    "status": "VALID",
    "issuedAt": "2026-11-14T22:30:00"
  }
]
```

Si la même demande est rejouée (par exemple après un timeout côté Order), la réponse est **200 OK** avec les mêmes billets, sans nouvelle émission. Si la commande 101 a déjà des billets avec un contenu différent (quantité, propriétaire, événement), la réponse est **409 Conflict**.

### 11.2 Valider un billet à l'entrée

`POST /api/tickets/665f1c2e9a1b2c3d4e5f6a7b/validations`

```json
{
  "controllerId": 7
}
```

Réponse **201 Created** (première validation) :

```json
{
  "id": "665f1d3b9a1b2c3d4e5f6a80",
  "ticketId": "665f1c2e9a1b2c3d4e5f6a7b",
  "controllerId": 7,
  "validationDate": "2026-12-01T18:45:00",
  "result": "ACCEPTED"
}
```

Réponse **409 Conflict** (billet déjà utilisé) :

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Le billet a déjà été utilisé",
  "instance": "/api/tickets/665f1c2e9a1b2c3d4e5f6a7b/validations"
}
```

### 11.3 Consulter les billets d'un utilisateur

`GET /api/tickets?ownerId=4`

Réponse **200 OK** : tableau de billets (même structure que 11.1), éventuellement vide.

### 11.4 Annuler un billet

`PATCH /api/tickets/665f1c2e9a1b2c3d4e5f6a7b/status`

```json
{
  "status": "CANCELLED"
}
```

Réponse **200 OK** avec le billet dont le statut est `CANCELLED`. Si le billet est déjà `USED` : **409 Conflict**.

## 12. Codes HTTP et erreurs

| Code | Signification | Cas dans le Ticket Service |
|---|---|---|
| 200 | OK | Consultation ; rejeu identique de l'émission ; annulation réussie |
| 201 | Created | Billets émis ; validation acceptée |
| 400 | Bad Request | Corps invalide (champ manquant, quantité nulle ou négative) |
| 404 | Not Found | Billet introuvable |
| 409 | Conflict | Émission avec contenu différent ; billet déjà utilisé ou annulé ; annulation d'un billet `USED` |
| 501 | Not Implemented | Validation 1 uniquement : endpoint déclaré, logique à venir |
| 503 | Service Unavailable | Réponse d'Order lorsque Ticket est indisponible (côté appelant) |

Les erreurs suivent le format commun de l'équipe, Problem Details (`application/problem+json`) avec les champs `type`, `title`, `status`, `detail` et `instance`. Les réponses d'erreur ne contiennent ni trace d'exécution ni détail technique de la base. Le code 422 n'est pas utilisé dans nos contrats.

## 13. Interactions avec les autres microservices

| Appelant | Appelé | Objet | Mode |
|---|---|---|---|
| Order | Ticket | Émettre les billets (`POST /api/tickets`) | Synchrone (REST / OpenFeign) |
| Order | Ticket | Consulter et annuler les billets (`GET ?orderId=`, `PATCH /{id}/status`) | Synchrone |
| Application de contrôle | Ticket | Valider un billet à l'entrée | Synchrone |
| Ticket | — | Aucun appel sortant | — |

Ticket n'appelle aucun autre service : il ne peut donc pas créer de dépendance circulaire.

### Gestion des pannes

- Si Ticket est arrêté ou ne répond pas, Order ne marque pas la commande `CONFIRMED` (la confirmation exige que toutes les étapes indispensables aient réussi).
- Order vérifie alors si les billets ont été émis (`GET /api/tickets?orderId=`) avant de réessayer : l'émission étant idempotente par `orderId`, une reprise ne crée pas de doublons.
- Si l'émission reste impossible, Order déclenche une compensation documentée (remboursement du paiement, libération de la réservation) et répond 503.
- Une commande contenant un billet `USED` ne peut pas être annulée intégralement (409).
- Notification est asynchrone et non bloquant : sa panne n'affecte pas les billets.

## 14. Documentation Swagger

| Ressource | Adresse |
|---|---|
| Swagger UI | `http://localhost:8085/swagger-ui/index.html` |
| Contrat OpenAPI (JSON) | `http://localhost:8085/v3/api-docs` |

La documentation est générée par springdoc-openapi à partir des annotations (`@Tag`, `@Operation`, `@ApiResponse`, `@Schema`). Elle décrit chaque endpoint, ses paramètres, les corps de requête, les schémas `Ticket` et `TicketValidation` et les codes de réponse, y compris les erreurs au format Problem Details.

En validation 1, les endpoints sont documentés et visibles dans Swagger UI ; les appels renvoient 501 Not Implemented. Le contrat JSON peut être déposé dans `Documentation/OpenAPI/` pour être ouvert dans Swagger Editor.

## 15. Scénarios de tests prévus (résultats réels seulement après implémentation)

> **Important :** ces trois scénarios sont décrits et seront exécutés après l'implémentation. Aucun résultat réel n'est présenté dans cette version.

### Scénario 1 – Émission d'un billet

| Rubrique | Contenu |
|---|---|
| Objectif | Vérifier que Order peut obtenir les billets d'une commande payée. |
| Préconditions | Aucun billet n'existe pour `orderId` 101. |
| Requête | `POST /api/tickets` avec `{orderId: 101, eventId: 12, ownerId: 4, quantity: 2}` |
| Résultat attendu | 201 Created ; 2 billets au statut `VALID`, chacun avec un `uniqueCode` différent. |
| Vérifications | `GET /api/tickets?orderId=101` renvoie 2 billets. Le même POST rejoué renvoie 200 avec les mêmes billets. Avec `quantity: 3`, la réponse est 409. |
| Statut | Décrit. À exécuter après implémentation. |

### Scénario 2 – Première validation d'un billet

| Rubrique | Contenu |
|---|---|
| Objectif | Vérifier qu'un billet valide est accepté à l'entrée. |
| Préconditions | Un billet `VALID` existe (par exemple `EVT12-7F3K9Q2A`). |
| Requête | `POST /api/tickets/{id}/validations` avec `{controllerId: 7}` |
| Résultat attendu | 201 Created ; une validation avec `result` `ACCEPTED` ; le billet passe à `USED`. |
| Vérifications | `GET /api/tickets/{id}` montre `status` `USED`. `GET /api/tickets/{id}/validations` liste la tentative acceptée. |
| Statut | Décrit. À exécuter après implémentation. |

### Scénario 3 – Refus d'une seconde validation

| Rubrique | Contenu |
|---|---|
| Objectif | Vérifier qu'un billet ne peut être utilisé qu'une seule fois. |
| Préconditions | Le billet du scénario 2 est déjà au statut `USED`. |
| Requête | `POST /api/tickets/{id}/validations` avec `{controllerId: 8}` |
| Résultat attendu | 409 Conflict au format Problem Details (« Le billet a déjà été utilisé »). |
| Vérifications | Le billet reste `USED`. L'historique contient 2 tentatives : `ACCEPTED` puis `REFUSED_ALREADY_USED`. Deux requêtes simultanées sur un billet `VALID` ne produisent qu'une seule acceptation. |
| Statut | Décrit. À exécuter après implémentation. |

## 16. Limites et perspectives

### Limites de la validation 1

- La logique métier n'est pas implémentée : les endpoints renvoient 501 Not Implemented.
- MongoDB n'est pas obligatoire pour démarrer ; la création de l'index unique est désactivée par défaut (`MONGO_AUTO_INDEX`).
- Aucune sécurité opérationnelle : l'authentification et les rôles sont prévus plus tard.
- Aucun test automatisé : les scénarios sont décrits seulement.
- La recherche d'un billet par code (filtre `uniqueCode`) n'est pas encore dans le contrat des autres services.

### Perspectives

- Implémenter la logique du `TicketService` : émission idempotente, validation atomique par mise à jour conditionnelle, historique, annulation.
- Générer et afficher un QR code à partir du `uniqueCode`.
- Sécurité cible : Keycloak et API Gateway ; seul le rôle `CONTROLLER` pourrait valider un billet, `STUDENT` consulterait ses propres billets, `ADMIN` et `ORGANIZER` selon leurs droits.
- Ajouter la pagination, des tests automatisés (dont la validation concurrente) et l'intégration d'Eureka et de l'événement `OrderConfirmed`.
