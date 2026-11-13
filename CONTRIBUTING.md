# Contribution — EventTun

## Règles d'équipe

- **Chaque membre est propriétaire de son service** dans `Backend/` et doit laisser un historique de commits vérifiable.
- Ne pas pousser directement sur `main` ; utiliser sa branche puis une Pull Request (PR).
- Avant de démarrer, `git pull origin main` depuis `main`, puis créer la branche : `git switch -c feature/<service>`.
- Éviter de modifier les fichiers des autres services sans concertation.
- Garder les **mêmes conventions REST** et les mêmes noms de dossiers avec la casse exacte.
- Ne jamais committer de mots de passe, jetons, secrets, fichiers `.env` ou configuration locale personnelle.
- Le service Notification est un travail collectif (cinq contributions identifiables), M5 coordonne.

## Branches

- M1 : `feature/user-service`
- M2 : `feature/event-service`
- M3 : `feature/order-service`
- M4 : `feature/payment-service`
- M5 : `feature/ticket-service`

Pour une mise à jour :

```bash
git switch main
git pull origin main
git switch feature/user-service
git merge main
# après développement
git add Backend/user-service
git commit -m "feat(user): describe changes"
git push -u origin feature/user-service
```

## Messages de commit

- `chore: initialize repository`
- `feat(user): add User and Club entities`
- `docs(event): document REST endpoints`
- `docs: add global use case diagram`
- `fix(payment): correct service configuration`

## Avant d'ouvrir une PR

- [ ] Les modifications concernent la bonne zone de responsabilité.
- [ ] Le microservice compile et démarre (lorsqu'un `pom.xml` est présent).
- [ ] Ports et noms de base conformes à la convention.
- [ ] Entités, API et réponses d'erreur documentées.
- [ ] Aucun secret ou fichier compilé versionné.
- [ ] La PR indique ce qui a été **fait**, ce qui est **conçu**, et ce qui reste à faire.

## API partagée

- URI au pluriel, minuscules, sans verbe ; `GET`, `POST`, `PUT`, `PATCH`, `DELETE`.
- JSON `camelCase` ; dates ISO 8601 ; pagination et filtres dans la query string.
- Réponses prévues : `200`, `201`, `204`, `400`, `401`, `403`, `404`, `409`, `500`, `503`.
- Erreurs au format Problem Details (`application/problem+json`) ; pas de traces techniques exposées.
- Les références entre services sont des identifiants ; **pas de jointure SQL entre bases**.
- Détails métier et interactions : voir le document de plan officiel de l'équipe.
