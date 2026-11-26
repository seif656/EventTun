# EventTun — Fiche technique M2 : Event Service

**Projet :** EventTun — plateforme universitaire de gestion d'événements  
**Responsable :** M2 — Event Service  
**Livrable :** Validation 1 — conception et squelette technique  
**Emplacement dans le dépôt :** `Documentation/Technique/event-service.md`

## 1. Objectif et responsabilités

Le microservice **Event Service** gère le catalogue des événements universitaires, leurs catégories de billets et les réservations de places associées aux commandes.

Responsabilités prévues :
- Créer, consulter et modifier des événements ; gérer leur état de publication.
- Définir et consulter les catégories de billets, leur prix et leur capacité.
- Réserver, confirmer et libérer des places pour les commandes du microservice M3.
- Prévenir la surréservation et gérer les appels répétés de façon idempotente lors de l'implémentation future.

**État réel en Validation 1 :** les entités JPA, les repositories et les classes d'organisation sont présents. Les routes métier sont **documentées dans un contrat OpenAPI statique, mais ne sont pas implémentées**.

## 2. Technologies et configuration

| Élément | Choix actuel |
|---|---|
| Langage | Java 17 |
| Framework | Spring Boot 3.5.7 |
| Build | Maven |
| API | Spring Web ; contrat OpenAPI 3.0.3, Swagger UI (springdoc) |
| Persistance | Spring Data JPA / Hibernate |
| Base locale | H2 en mémoire, `jdbc:h2:mem:eventtun_events` |
| Base cible ultérieure | MySQL, `events_db` |
| Port | `8082` |
| Surveillance | Spring Boot Actuator, `/actuator/health` |

Le démarrage local a été observé dans IntelliJ, Swagger UI s'affiche et une compilation Maven a abouti à `BUILD SUCCESS`. **Aucun test fonctionnel des API métier n'a été exécuté.**

## 3. Modèle de données

### 3.1 `Event`

| Attribut | Type Java | Rôle |
|---|---|---|
| `id` | `Long` | Identifiant de l'événement |
| `clubId` | `Long` | Référence logique au club du User Service (M1) |
| `title` | `String` | Titre |
| `description` | `String` | Description |
| `location` | `String` | Lieu |
| `startDate` | `LocalDateTime` | Début |
| `endDate` | `LocalDateTime` | Fin |
| `status` | `EventStatus` | `DRAFT`, `PUBLISHED`, `CANCELLED` |

Le statut initial défini dans le code est `DRAFT`.

### 3.2 `TicketCategory`

| Attribut | Type Java | Rôle |
|---|---|---|
| `id` | `Long` | Identifiant |
| `eventId` | `Long` | Référence logique à l'événement |
| `name` | `String` | Nom de catégorie |
| `price` | `BigDecimal` | Prix unitaire |
| `capacity` | `Integer` | Capacité totale |
| `availableQuantity` | `Integer` | Places encore disponibles |

### 3.3 `SeatReservation`

| Attribut | Type Java | Rôle |
|---|---|---|
| `id` | `Long` | Identifiant |
| `orderId` | `Long` | Référence logique à une commande M3 |
| `categoryId` | `Long` | Catégorie concernée |
| `quantity` | `Integer` | Nombre de places |
| `status` | `ReservationStatus` | `HELD`, `CONFIRMED`, `RELEASED` |
| `createdAt` | `LocalDateTime` | Date de création |

Le statut initial est `HELD`. La table déclare une contrainte d'unicité sur le couple `(order_id, category_id)`, préparant l'idempotence des réservations. Cette contrainte ne remplace pas l'implémentation des règles métier.

**Important :** `unitPrice` apparaît dans la réponse API de réservation destinée à M3, mais n'est pas un attribut de l'entité JPA `SeatReservation`. Les identifiants d'autres microservices (`clubId`, `orderId`) ne constituent pas des clés étrangères entre bases de données.

## 4. Organisation du code

- `model/` : les trois entités JPA et les deux énumérations.
- `repository/` : `EventRepository`, `TicketCategoryRepository`, `SeatReservationRepository`, chacun étendant `JpaRepository<Entité, Long>`.
- `controller/` : `EventController`, `CategoryController`, `ReservationController` ; **classes réservées, sans routes métier**.
- `service/` : `EventService` et `ReservationService` ; **squelettes, sans logique métier**.
- `EventServiceApplication.java` : démarrage de Spring Boot.
- `SwaggerDocumentationController.java` : expose uniquement le fichier de spécification statique `/openapi/event-service.yaml`.
- `src/main/resources/static/openapi/event-service.yaml` : contrat d'API.

## 5. Contrat REST documenté (11 opérations)

| Méthode | Chemin | Objectif prévu |
|---|---|---|
| POST | `/api/events` | Créer un événement brouillon |
| GET | `/api/events` | Lister / filtrer les événements |
| GET | `/api/events/{id}` | Consulter un événement |
| PUT | `/api/events/{id}` | Modifier un événement |
| PATCH | `/api/events/{id}/status` | Changer son statut |
| POST | `/api/events/{id}/categories` | Créer une catégorie de billets |
| GET | `/api/events/{id}/categories` | Lister les catégories |
| POST | `/api/categories/{id}/reservations` | Réserver des places |
| DELETE | `/api/categories/{id}/reservations/{orderId}` | Libérer des places |
| PATCH | `/api/categories/{id}/reservations/{orderId}/status` | Confirmer une réservation |
| GET | `/api/events/{id}/reservations` | Lister les réservations d'un événement |

Ces **11 opérations sur 8 chemins** sont des **spécifications**, et non des fonctionnalités REST déjà disponibles.

### Règles métier prévues dans le contrat

- Création de réservation : `201` lors du premier appel ; `200` si même commande/catégorie et quantité identique ; `409` si quantité incompatible.
- Confirmation : transition `HELD` → `CONFIRMED`, avec réponse `200` ; conflit `409` si réservation déjà `RELEASED`.
- Libération : réponse `204`, y compris si la libération a déjà eu lieu ; restauration du stock une seule fois lors de la future implémentation.
- Erreurs métier : format `application/problem+json` prévu dans le contrat.

Ces comportements **ne sont pas encore implémentés**.

## 6. Interfaces avec les autres microservices

- **M1 — User Service :** `clubId` identifie le club organisateur, sans clé étrangère interservices.
- **M3 — Order Service :** `orderId` relie logiquement une réservation à une commande ; le contrat de réservation prévoit `unitPrice` dans sa réponse.
- **Autres services :** l'orchestration des commandes, le paiement, l'émission de tickets et les notifications sont gérés dans l'architecture globale, pas dans ce squelette M2.

Le contrat entre M2 et M3 devra être confirmé avec le responsable M3 avant l'implémentation des échanges interservices.

## 7. Vérifications réalisées et scénarios futurs

**Réalisé / constaté :**
- Application démarrée dans IntelliJ avec H2 initialisée et port 8082.
- Swagger UI accessible avec les 11 opérations réparties entre événements, catégories et réservations.
- Compilation Maven réussie (`BUILD SUCCESS`, résultat communiqué après reconstruction du projet).

**Scénarios prévus, non exécutés :**
1. Création d'un événement valide et rejet des champs obligatoires manquants.
2. Publication / annulation d'un événement et contrôle des transitions autorisées.
3. Création d'une catégorie et vérification de sa capacité et de son prix.
4. Réservation de places disponibles et diminution du stock.
5. Refus d'une réservation si les places sont insuffisantes.
6. Rejeu identique d'une réservation sans double prélèvement de stock.
7. Rejeu avec quantité différente produisant un conflit.
8. Confirmation d'une réservation et refus de confirmer une réservation libérée.
9. Libération répétée sans double restitution du stock.
10. Consultation des réservations d'un événement.

**Aucun de ces scénarios ne doit être présenté comme un test passé en Validation 1.**

## 8. Limites et étapes suivantes

La Validation 1 porte sur la structure, les modèles et les contrats. Restent à développer : contrôleurs REST métier, services métier, validations, gestion atomique du stock, transitions d'état, idempotence applicative, gestion des erreurs, tests fonctionnels et intégration interservices (notamment OpenFeign / RabbitMQ selon le plan d'équipe). Le passage de H2 à MySQL est également prévu ultérieurement.

## 9. Démonstration de Validation 1

1. Montrer la structure du projet dans IntelliJ (entités, repositories, contrôleurs et services réservés).
2. Montrer la compilation réussie et le démarrage Spring Boot.
3. Ouvrir `http://localhost:8082/swagger-ui/index.html` et présenter les 11 opérations documentées.
4. Expliquer que Swagger affiche le **contrat cible**, pas des endpoints fonctionnels : ne pas présenter `Try it out` comme preuve d'implémentation.

**Conclusion :** Event Service possède un squelette Spring Boot compilable, un modèle JPA et un contrat OpenAPI documenté. La logique métier et les tests des endpoints relèvent des validations suivantes.
