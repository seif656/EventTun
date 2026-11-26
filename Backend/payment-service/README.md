# Payment Service (M4)

Paiements **simulés** et remboursements pour EventTun. Aucun paiement bancaire réel.

| | |
|---|---|
| Port | 8084 |
| Base | `payments_db` (MySQL cible, H2 en mémoire pour le démarrage local) |
| Entités | `Payment`, `Refund` |
| Appelé par | Order Service (synchrone, REST/OpenFeign) |
| Appelle | Aucun service |

## Lancement

Prérequis : Java 17+ et Maven (ou IntelliJ IDEA).

```bash
mvn spring-boot:run
```

- Swagger UI : http://localhost:8084/swagger-ui.html
- Santé : http://localhost:8084/actuator/health
- Console H2 : http://localhost:8084/h2-console (JDBC URL : `jdbc:h2:mem:payments_db`, user `sa`)

## Endpoints

| Méthode | URI | Codes |
|---|---|---|
| POST | `/api/payments` | 201, 200 (rejeu identique), 400, 409 |
| GET | `/api/payments/{id}` | 200, 404 |
| GET | `/api/payments?orderId=` | 200 |
| POST | `/api/payments/{id}/refunds` | 201, 200 (rejeu identique), 400, 404, 409 |
| GET | `/api/payments/{id}/refunds` | 200, 404 |

## État (validation 1)

- Conçu et documenté : entités, contrat OpenAPI, règles d'idempotence.
- Développé : structure Controller / Service / Repository, endpoints de lecture.
- À venir : simulation du paiement et remboursement (renvoient 501 pour l'instant), passage à MySQL, tests métier.
