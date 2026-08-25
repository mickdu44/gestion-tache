# Gestion des taches

Client lourd JavaFX pour gerer des taches quotidiennes de travail, avec
report des taches non terminees et recherche plein texte. Aucune donnee
ne quitte la machine : tout est stocke en local.

## Fonctionnalites

- **Vue par jour** : navigation entre les jours (precedent / suivant /
  aujourd'hui) pour afficher les taches du jour selectionne.
- **Ajout / modification / suppression** de taches (titre, description,
  date), avec case a cocher pour marquer une tache comme terminee.
- **Report des taches non terminees** :
  - bouton "Reporter les taches non terminees a demain" pour reporter
    au lendemain les taches non terminees du jour affiche ;
  - au demarrage, si des taches non terminees de jours precedents
    existent, l'application propose de les reporter automatiquement a
    aujourd'hui.
- **Recherche** : la barre de recherche filtre l'ensemble des taches
  (tous les jours confondus) dont le **titre** ou la **description**
  contient les mots-cles saisis (insensible a la casse).
- **Stockage local** : les taches sont enregistrees dans un fichier
  JSON local (`~/.gestion-tache/tasks.json`), sans base de donnees ni
  serveur distant. Chaque modification (ajout, edition, suppression,
  report) est persistee immediatement.

## Prerequis

- Java 21+ (JDK)
- Maven 3.9+

## Build

Compiler le projet et executer les tests :

```bash
mvn test
```

Construire un jar executable (JavaFX embarque, autonome) :

```bash
mvn package
```

Le jar est genere dans `target/gestion-tache-1.0.0.jar`.

## Lancer l'application

En mode developpement (via le plugin Maven JavaFX) :

```bash
mvn javafx:run
```

Ou, apres un `mvn package`, en executant directement le jar :

```bash
java -jar target/gestion-tache-1.0.0.jar
```

## Donnees

Les taches sont lues/ecrites dans `~/.gestion-tache/tasks.json` au
format JSON. Supprimer ce fichier reinitialise l'application (perte de
toutes les taches).

## Structure du projet

```
src/main/java/com/gestiontache/
  MainApp.java                     Point d'entree JavaFX
  Launcher.java                    Point d'entree pour le jar shade (java -jar)
  model/Task.java                  Modele d'une tache
  repository/TaskRepository.java   Persistance JSON locale
  service/TaskService.java         Logique metier (recherche, report, CRUD)
  controller/                      Controleurs JavaFX (FXML)
src/main/resources/com/gestiontache/
  main.fxml, task-dialog.fxml      Vues
  style.css                        Style de l'application
src/test/java/com/gestiontache/    Tests unitaires (JUnit 5)
```

## Contribuer

Le projet suit le workflow **Gitflow** (`main` / `develop` /
`feature/*` / `release/*` / `hotfix/*`) : voir [CONTRIBUTING.md](CONTRIBUTING.md)
pour le detail des conventions.

Les evolutions et corrections a venir sont listees dans [TODO.md](TODO.md).
