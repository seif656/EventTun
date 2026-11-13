# EventTun — Plateforme de gestion des événements universitaires

Projet pédagogique Spring Boot en architecture microservices — **Validation 1**.

## Objectif
Permettre aux étudiants de consulter des événements universitaires, réserver des billets, effectuer des paiements **simulés** et utiliser des billets numériques. Les organisateurs gèrent les événements et les contrôleurs vérifient les billets.

## Services et responsables

| Membre | Service | Port | Base cible | Travail collectif |
|---|---|---:|---|---|
| M1 | `user-service` | 8081 | MySQL (`users_db`) | Dépôt GitHub, intégration et suivi |
| M2 | `event-service` | 8082 | MySQL (`events_db`) | Use Case et analyse fonctionnelle |
| M3 | `order-service` | 8083 | MySQL (`orders_db`) | Interactions et séquence d'achat |
| M4 | `payment-service` | 8084 | MySQL (`payments_db`) | Architecture globale |
| M5 | `ticket-service` | 8085 | MongoDB (`tickets_db`) | Rapport, PowerPoint, coordination Notification |
| Tous | `notification-service` | à définir | Aucune exigée en V1 | Squelette commun coordonné par M5 |

## Validation 1 — périmètre

**À livrer** : dépôt public, branches et commits individuels, GitHub Projects, **six squelettes Spring Boot compilables et démarrables** (cinq individuels et Notification commun), au moins deux entités par service individuel (trois dans Event), contrats REST Swagger/OpenAPI, diagrammes, fiches techniques, rapport et présentation.

**Non exigés à ce stade** : logique métier complète, paiements réels, frontend complet, tests fonctionnels métier et déploiement opérationnel de Keycloak, Gateway, Eureka, Config Server, RabbitMQ et Docker. Ces composants font partie de l'architecture cible.

> Ce dépôt contient d'abord **la structure de démarrage**. Les dossiers des services ne sont pas encore des applications Spring Boot. Chaque membre doit générer, committer et vérifier son propre projet pour garantir la traçabilité du travail.

## Structure

```text
EventTun/
├── Backend/
│   ├── user-service/
│   ├── event-service/
│   ├── order-service/
│   ├── payment-service/
│   ├── ticket-service/
│   └── notification-service/
├── Frontend/
│   └── eventtun-web/
├── Documentation/
│   ├── Presentation/
│   ├── Diagrams/
│   ├── Technique/
│   ├── OpenAPI/
│   └── choix-theme.md
├── .github/
├── .gitignore
├── CONTRIBUTING.md
└── README.md
```

## Démarrage du dépôt (M1)

1. Créer un dépôt GitHub **public** nommé `EventTun`, branche par défaut `main` ; **ne pas cocher** « Add a README », `.gitignore` ou « Choose a license » puisque ces fichiers sont fournis ici.
2. Télécharger le kit, extraire le dossier `EventTun-Starter`, puis l'ouvrir dans un terminal.
3. Remplacer `VOTRE_PSEUDO` dans les commandes :

```bash
git init -b main
git add .
git commit -m "chore: initialize EventTun repository structure"
git remote add origin https://github.com/VOTRE_PSEUDO/EventTun.git
git push -u origin main
```

4. Dans **Settings → Collaborators** (ou **Collaborators and teams**), inviter les quatre membres et le professeur selon les consignes.
5. Ouvrir **Projects** et créer un tableau `EventTun — Validation 1` (Backlog, Todo, In progress, Review, Done).
6. Créer cinq issues (« Initialiser ... ») et une issue collective Notification ; attribuer à chaque membre son travail.
7. Chaque membre crée sa branche, initialise son Spring Boot, committe et ouvre une Pull Request.

Voir [GUIDE_GIT_EVENTTUN.md](GUIDE_GIT_EVENTTUN.md) et [CONTRIBUTING.md](CONTRIBUTING.md).

## Workflow Git commun

```bash
git clone https://github.com/VOTRE_PSEUDO/EventTun.git
cd EventTun
git switch -c feature/user-service    # M1 uniquement, exemple
git add Backend/user-service
git commit -m "feat(user): initialize Spring Boot service"
git push -u origin feature/user-service
```

Créer ensuite une Pull Request vers `main`. **Ne pas développer directement sur `main`.**

Branches recommandées : `feature/user-service`, `feature/event-service`, `feature/order-service`, `feature/payment-service`, `feature/ticket-service`. Pour Notification, utiliser une branche commune ou des PR dédiées, selon la décision d'équipe.

## Configuration et Swagger

Les projets seront créés avec Java, Spring Boot, Maven, Spring Web, validation et springdoc-openapi. Les services M1–M4 utilisent Spring Data JPA ; M5 Spring Data MongoDB. Une configuration locale H2 peut servir au démarrage M1–M4 sans MySQL (la cible reste MySQL). Les paramètres exacts de versions sont à convenir entre les membres.

Swagger UI une fois chaque service généré/configuré/lancé :
- User : http://localhost:8081/swagger-ui/index.html
- Event : http://localhost:8082/swagger-ui/index.html
- Order : http://localhost:8083/swagger-ui/index.html
- Payment : http://localhost:8084/swagger-ui/index.html
- Ticket : http://localhost:8085/swagger-ui/index.html

## Documents

- `Documentation/Diagrams/` : Use Case, architecture, séquence et cinq diagrammes de classes.
- `Documentation/Technique/` : cinq fiches techniques et rapport.
- `Documentation/OpenAPI/` : contrats API de chaque service.
- `Documentation/Presentation/` : support oral collectif (10 diapositives maximum suivant le plan).
- `Documentation/choix-theme.md` : justification de la thématique.

**Attention** : ne présenter que comme *conçus* les composants qui ne sont pas encore implémentés ou testés. La date définitive de validation doit être vérifiée sur Blackboard.
