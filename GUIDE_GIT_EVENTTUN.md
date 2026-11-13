# M1 — Guide pratique pour publier le dépôt et lancer le travail

## 0. Prérequis

- Avoir un compte GitHub et installer Git.
- Configurer `git config --global user.name "Votre Nom"` et `git config --global user.email "email@example.com"` (adresse GitHub de votre choix).
- Préférer une connexion GitHub par Git Credential Manager (HTTPS) ou SSH ; ne jamais envoyer de mot de passe dans le dépôt.

## 1. Créer le dépôt sur GitHub

1. Ouvrir `https://github.com/new`.
2. Nom : `EventTun` ; description : `Plateforme universitaire de gestion d'événements — microservices Spring Boot`.
3. Visibilité : **Public**.
4. Ne pas initialiser avec un README/.gitignore/license (présents dans le kit).
5. Cliquer **Create repository**.

## 2. Premier push (terminal Git Bash ou PowerShell)

Ouvrir le terminal dans le dossier extrait qui contient `README.md` et `Backend/` :

```bash
git init -b main
git add .
git commit -m "chore: initialize EventTun repository structure"
git remote add origin https://github.com/VOTRE_PSEUDO/EventTun.git
git push -u origin main
```

Si `git init -b main` n'est pas accepté : `git init`, puis `git branch -M main`.

Si le dépôt GitHub contient déjà un README, **ne pas pousser en force** : récupérer les changements ou recréer le dépôt vide.

## 3. Inviter et organiser

- GitHub → dépôt → **Settings → Collaborators / Collaborators and teams** → inviter les **quatre autres membres et le professeur** (la procédure est soumise aux autorisations du compte).
- Les collaborateurs doivent accepter l'invitation.
- GitHub **Projects** : créer un tableau `EventTun — Validation 1` avec les colonnes `Backlog`, `Todo`, `In progress`, `Review`, `Done`.
- Créer les issues suivantes et les assigner :
  - M1 — Initialiser User Service et la gestion du dépôt.
  - M2 — Initialiser Event Service + Use Case général.
  - M3 — Initialiser Order Service + diagramme de séquence.
  - M4 — Initialiser Payment Service + architecture globale.
  - M5 — Initialiser Ticket Service + assembler la documentation.
  - Tous / M5 coordination — Initialiser Notification Service.

## 4. Premier jour pour chaque membre

```bash
git clone https://github.com/VOTRE_PSEUDO/EventTun.git
cd EventTun
git switch -c feature/event-service
# générer Spring Boot dans Backend/event-service/
git add Backend/event-service
git commit -m "feat(event): initialize Spring Boot application"
git push -u origin feature/event-service
```

Ensuite GitHub → **Compare & pull request** → base `main` → demander une revue à M1.

## 5. Vérifications M1

- Ouvrir le dépôt en navigation privée : il doit être consultable publiquement.
- Vérifier `Backend`, `Frontend`, `Documentation/Presentation`, `Documentation/Diagrams`, `README.md` et `.gitignore`.
- Vérifier les cinq branches / contributions individuelles et les PR.
- Vérifier la présence et le démarrage effectif des **six** applications avant la Validation 1 ; **ce kit n'est qu'une base de départ**.
- Récupérer les contrats Swagger/OpenAPI, les diagrammes et le rapport.
- Vérifier la date exacte et les consignes finales sur Blackboard.
