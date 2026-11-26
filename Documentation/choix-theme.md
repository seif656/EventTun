# Choix de la thématique — EventTun

**Projet :** EventTun — Plateforme web distribuée de gestion des événements universitaires  
**Module :** Architecture microservices  
**Livrable :** Choix de la thématique et analyse fonctionnelle initiale  
**Étape :** Validation 1  
**Responsabilité collective :** Équipe EventTun  
**Contribution M2 :** Analyse fonctionnelle et diagramme de cas d'utilisation général

## 1. Présentation du sujet

EventTun est un projet de plateforme web distribuée destiné à faciliter l'organisation et la participation aux événements universitaires : conférences, ateliers, formations, hackathons et activités culturelles.

La plateforme vise à réunir dans un même système la publication des événements, la consultation des informations, la réservation des places, le paiement simulé, l'émission des billets numériques et leur contrôle à l'entrée. Le projet est conçu selon une architecture microservices afin de répartir les responsabilités métier entre plusieurs applications indépendantes.

## 2. Contexte et constat

Dans un environnement universitaire, les clubs et les organisateurs peuvent utiliser plusieurs outils distincts pour communiquer sur leurs événements et gérer les inscriptions : réseaux sociaux, formulaires en ligne, feuilles de calcul et messageries.

Cette dispersion peut entraîner :

- des informations difficiles à retrouver ou à mettre à jour ;
- une visibilité limitée sur le nombre de places encore disponibles ;
- un suivi manuel des inscriptions et des commandes ;
- des difficultés à vérifier les billets et à éviter leur réutilisation ;
- un manque de coordination entre les différentes étapes du parcours d'inscription.

Ces difficultés constituent le problème métier auquel EventTun cherche à répondre. Elles servent de motivation au projet ; elles ne constituent pas les résultats d'une enquête de terrain.

## 3. Problématique

**Comment concevoir une plateforme web distribuée permettant de centraliser et de fiabiliser la gestion des événements universitaires, des réservations, des paiements simulés et des billets numériques, tout en assurant une séparation claire des responsabilités et une communication cohérente entre les microservices ?**

Cette problématique comporte deux dimensions complémentaires :

1. **Fonctionnelle :** simplifier le parcours des étudiants, des organisateurs et des contrôleurs.
2. **Technique :** organiser les fonctionnalités en services autonomes, définir leurs interfaces et anticiper les problèmes de communication et de cohérence des données.

## 4. Objectifs du projet

### 4.1. Objectifs fonctionnels

- Permettre aux étudiants de consulter les événements et leurs détails.
- Permettre aux organisateurs de créer et de gérer les événements et les catégories de billets.
- Gérer les quantités de places et les réservations associées aux commandes.
- Simuler le paiement et prévoir la gestion des remboursements.
- Produire des billets numériques identifiables par un code unique.
- Permettre la vérification et la validation des billets à l'entrée.
- Prévoir l'envoi de notifications liées aux événements métier, notamment la confirmation d'une commande.

### 4.2. Objectifs techniques

- Concevoir une architecture répartie en cinq microservices métier et un service commun de notification.
- Définir une responsabilité et un modèle de données propres à chaque microservice.
- Documenter les API REST et les contrats d'échange avec OpenAPI/Swagger.
- Distinguer les communications synchrones nécessaires aux décisions de commande et les messages asynchrones adaptés aux notifications.
- Anticiper la gestion des erreurs, des appels répétés, des réservations concurrentes et des compensations.
- Organiser le travail de l'équipe dans un dépôt GitHub commun avec une documentation technique et des diagrammes UML.

## 5. Acteurs et besoins

| Acteur | Besoins principaux |
|---|---|
| **Étudiant** | Consulter les événements, choisir une catégorie, réserver des places, effectuer un paiement simulé, consulter ses commandes et billets. |
| **Organisateur** | Gérer un club, créer et publier des événements, définir les catégories, prix et capacités, suivre les réservations. |
| **Contrôleur** | Vérifier un billet et enregistrer sa validation à l'entrée. |
| **Administrateur** | Superviser les utilisateurs et les clubs, gérer les comptes selon les droits prévus. |

Ces besoins sont représentés dans le **diagramme de cas d'utilisation général** du projet, disponible dans `Documentation/Diagrams/eventtun-usecase-global.png`.

## 6. Fonctionnalités principales envisagées

### Gestion des utilisateurs et des clubs

Le système doit permettre de représenter les profils des utilisateurs, leurs rôles et les clubs universitaires. L'authentification centralisée par Keycloak appartient à l'architecture cible et sera intégrée ultérieurement.

### Gestion des événements

Les organisateurs doivent pouvoir créer, consulter, modifier et publier des événements. Chaque événement peut posséder plusieurs catégories de billets, caractérisées notamment par un prix, une capacité et une quantité disponible.

### Gestion des commandes et des réservations

Lorsqu'un étudiant passe une commande, le système doit vérifier la disponibilité des places et gérer leur réservation. Les places retenues doivent pouvoir être confirmées ou libérées selon le résultat du parcours de commande.

### Paiement simulé

Le paiement est simulé dans le cadre du projet académique : aucune opération bancaire réelle n'est nécessaire. Le système doit prévoir les résultats acceptés ou refusés et les remboursements simulés lorsque la logique métier le nécessite.

### Billets numériques et contrôle d'accès

Après une commande confirmée, des billets numériques peuvent être émis avec un code unique. Le contrôleur doit pouvoir vérifier leur validité et empêcher la réutilisation d'un billet déjà consommé. La génération graphique de QR codes est une évolution possible, non un prérequis de la première validation.

### Notifications

Un service commun de notification est prévu pour traiter des événements métier tels que `OrderConfirmed`. La transmission asynchrone via RabbitMQ est envisagée dans l'architecture cible ; elle n'est pas requise comme intégration opérationnelle en Validation 1.

## 7. Justification du choix de la thématique

EventTun a été retenu parce que son domaine métier est compréhensible, réaliste et suffisamment riche pour illustrer les principes d'une architecture microservices.

**Pertinence fonctionnelle :** les événements universitaires font intervenir plusieurs acteurs et un parcours complet, depuis la publication jusqu'au contrôle des billets.

**Découpage naturel :** la gestion des utilisateurs, des événements, des commandes, des paiements et des billets correspond à des responsabilités métier distinctes.

**Travail collaboratif :** cinq microservices métier permettent une répartition claire du développement entre les cinq membres de l'équipe.

**Intérêt pédagogique :** le sujet permet d'étudier les API REST, les bases indépendantes, la communication interservices, l'idempotence, les transactions distribuées et la résilience.

**Évolutivité :** les services pourront évoluer séparément, sous réserve de respecter les contrats d'interface et de maîtriser la complexité distribuée.

## 8. Découpage en microservices

| Microservice | Responsabilité principale | Base cible | Port prévu |
|---|---|---|---|
| **User Service — M1** | Profils utilisateurs et clubs | MySQL — `users_db` | 8081 |
| **Event Service — M2** | Événements, catégories de billets et réservations de places | MySQL — `events_db` | 8082 |
| **Order Service — M3** | Commandes et coordination du parcours d'achat | MySQL — `orders_db` | 8083 |
| **Payment Service — M4** | Paiements et remboursements simulés | MySQL — `payments_db` | 8084 |
| **Ticket Service — M5** | Émission, consultation et validation des billets | MongoDB — `tickets_db` | 8085 |
| **Notification Service — commun** | Traitement des notifications métier | Pas de base persistante imposée en Validation 1 | À définir |

Chaque service métier est propriétaire de ses données. Les autres services doivent utiliser ses interfaces plutôt que modifier directement sa base.

### Focus sur Event Service — M2

Event Service est responsable de trois entités métier :

- `Event` : informations et statut de l'événement ;
- `TicketCategory` : catégories, prix, capacités et places disponibles ;
- `SeatReservation` : réservations associées aux commandes et à leurs statuts.

Ce découpage permet de séparer la description d'un événement, son offre de billets et les places effectivement retenues. Le modèle M2 et ses contrats REST sont détaillés dans sa documentation technique individuelle et dans son diagramme de classes.

## 9. Architecture et choix technologiques

| Élément | Choix | Utilité |
|---|---|---|
| Backend | Java et Spring Boot | Développer les microservices. |
| API | HTTP REST, JSON, OpenAPI/Swagger | Définir et documenter les échanges. |
| Persistance relationnelle | Spring Data JPA, MySQL | Gérer les données structurées de M1 à M4. |
| Développement local | H2 | Démarrer les squelettes JPA sans serveur MySQL. |
| Persistance documentaire | MongoDB | Gérer les données de billets de M5. |
| Frontend cible | React | Fournir une interface web. |
| Appels synchrones cibles | REST et OpenFeign | Obtenir les réponses nécessaires au traitement des commandes. |
| Messages asynchrones cibles | RabbitMQ | Découpler le traitement des notifications. |
| Routage cible | API Gateway | Offrir un point d'entrée aux clients. |
| Découverte cible | Eureka | Localiser les instances des services. |
| Configuration cible | Spring Cloud Config Server | Centraliser certaines configurations. |
| Sécurité cible | Keycloak, JWT, Spring Security | Gérer l'identité, les rôles et les accès. |
| Déploiement cible | Docker / Docker Compose | Préparer un environnement multi-composants. |
| Suivi et résilience cibles | Actuator, timeouts, Resilience4j | Observer les services et gérer certaines défaillances. |

**Précision :** ce tableau présente la conception cible ; il ne signifie pas que toutes ces technologies sont déjà installées, connectées ou opérationnelles en Validation 1.

## 10. Scénario métier de référence

1. L'étudiant consulte un événement et choisit une catégorie de billets.
2. Order Service crée une commande en attente et vérifie les informations nécessaires.
3. Order Service demande à Event Service de retenir les places disponibles.
4. Event Service répond avec le résultat de la réservation et les informations nécessaires à la commande.
5. Order Service demande à Payment Service de simuler le paiement.
6. Si le paiement est refusé, Order Service coordonne la libération des places.
7. Si le paiement est accepté, Order Service demande la confirmation de la réservation.
8. Après confirmation, Order Service demande à Ticket Service d'émettre les billets.
9. Une fois la commande confirmée, un événement `OrderConfirmed` peut être publié pour Notification Service.
10. À l'entrée, un contrôleur peut faire vérifier et valider un billet par Ticket Service.

Ce scénario décrit le **fonctionnement prévu**. Les cas d'échec, les reprises et les compensations devront être implémentés et testés lors des étapes suivantes.

## 11. Risques et mesures envisagées

| Risque | Exemple dans EventTun | Mesure prévue |
|---|---|---|
| Sur-réservation | Deux commandes demandent la dernière place. | Vérification de disponibilité et décrément atomique du stock. |
| Appel répété | Une requête est renvoyée après un timeout. | Idempotence et contraintes d'unicité. |
| Service indisponible | Payment Service ne répond plus. | Timeout, gestion des erreurs et mécanismes de résilience. |
| Incohérence distribuée | Paiement refusé après réservation. | Saga coordonnée par Order Service et compensation. |
| Données dispersées | Une commande a besoin d'informations sur un événement. | API entre services ou conservation contrôlée de données nécessaires. |
| Accès non autorisé | Un étudiant tente de modifier un événement. | Authentification et autorisation par rôles. |
| Notification non traitée | Notification Service est temporairement indisponible. | Messagerie asynchrone avec configuration adaptée de la livraison et des reprises. |
| Complexité d'intégration | Des services utilisent des contrats incompatibles. | Documentation OpenAPI, conventions communes et tests d'intégration futurs. |

Ces mesures sont des **objectifs de conception** ; leur efficacité devra être vérifiée par des tests lors des validations ultérieures.

## 12. Périmètre de la Validation 1

La première validation porte principalement sur la **conception et l'initialisation technique** du projet :

- choix et justification de la thématique ;
- identification des acteurs et des fonctionnalités ;
- découpage en microservices et répartition des responsabilités ;
- modèles d'entités et diagrammes UML ;
- squelettes Spring Boot compilables et démarrables ;
- contrats d'API REST documentés avec OpenAPI/Swagger ;
- organisation du dépôt GitHub et documentation individuelle et collective.

**Hors du périmètre fonctionnel complet de cette validation :** intégration opérationnelle de tous les services, exécution réelle du parcours de réservation et de paiement, sécurité complète, déploiement distribué, messagerie RabbitMQ opérationnelle et tests métier de bout en bout.

Pour M2, le projet Spring Boot, les entités JPA, les repositories et la documentation OpenAPI constituent le socle préparé. Les controllers métier et les règles de réservation restent à implémenter.

## 13. Résultats attendus et perspectives

À l'issue de la phase de conception, l'équipe doit disposer d'un découpage métier cohérent, de responsabilités identifiées, d'un premier modèle de données par service et de contrats API permettant de préparer le développement collaboratif.

Les validations suivantes pourront porter sur l'implémentation des opérations métier, les échanges interservices, la gestion des échecs, la sécurité, l'interface web, les tests et le déploiement intégré.

## 14. Conclusion

Le choix d'EventTun répond à un besoin identifiable de centralisation de la gestion des événements universitaires. Il offre également un cas d'étude pertinent pour appliquer les concepts d'architecture microservices : autonomie des services, séparation des données, communication REST, messagerie asynchrone, cohérence distribuée et résilience.

La première validation établit les fondations fonctionnelles et techniques. La réalisation et la vérification du fonctionnement complet de la plateforme constituent les étapes suivantes du projet.
