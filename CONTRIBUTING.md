# Workflow Gitflow

Ce projet suit le modele **Gitflow**. Deux branches permanentes :

- `main` — code de production. Chaque commit sur `main` correspond a une
  version livree et est tague (`vX.Y.Z`).
- `develop` — branche d'integration. Toutes les fonctionnalites terminees
  et validees sont fusionnees ici avant une release.

## Branches temporaires

| Type | Part de | Fusionne dans | Nommage |
|---|---|---|---|
| Fonctionnalite | `develop` | `develop` | `feature/<nom-court>` |
| Release | `develop` | `main` **et** `develop` | `release/x.y.z` |
| Correctif urgent | `main` | `main` **et** `develop` | `hotfix/x.y.z` |

### Feature

```bash
git checkout develop
git pull origin develop
git checkout -b feature/ma-fonctionnalite
# ... commits ...
git push -u origin feature/ma-fonctionnalite
# Pull request vers develop, puis suppression de la branche apres fusion.
```

### Release

```bash
git checkout develop
git checkout -b release/1.1.0
# corrections mineures, mise a jour de version/README
git checkout main && git merge --no-ff release/1.1.0 && git tag -a v1.1.0 -m "Version 1.1.0"
git checkout develop && git merge --no-ff release/1.1.0
git branch -d release/1.1.0
git push origin main develop --tags
```

### Hotfix

```bash
git checkout main
git checkout -b hotfix/1.0.1
# correction du bug
git checkout main && git merge --no-ff hotfix/1.0.1 && git tag -a v1.0.1 -m "Correctif 1.0.1"
git checkout develop && git merge --no-ff hotfix/1.0.1
git branch -d hotfix/1.0.1
git push origin main develop --tags
```

## Regles

- Jamais de commit direct sur `main` (uniquement via merge de `release/*` ou `hotfix/*`).
- Une pull request est requise pour fusionner une branche `feature/*` dans `develop`.
- Chaque merge vers `main` est tague en suivant [semver](https://semver.org/lang/fr/).
- `mvn test` doit passer avant toute fusion dans `develop` ou `main`.
