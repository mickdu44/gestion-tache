# Gestion des taches

Client lourd JavaFX pour gerer des taches quotidiennes de travail.

## Fonctionnalites

- **Vue par jour** : navigation entre les jours (precedent/suivant/aujourd'hui) pour afficher les taches du jour.
- **Ajout / modification / suppression** de taches (titre, description, date), avec case a cocher pour marquer une tache comme terminee.
- **Report des taches non terminees** :
  - bouton "Reporter les taches non terminees a demain" pour reporter les taches non terminees du jour affiche au lendemain ;
  - au demarrage, si des taches non terminees de jours precedents existent, l'application propose de les reporter automatiquement a aujourd'hui.
- **Recherche** : la barre de recherche filtre l'ensemble des taches (tous les jours confondus) dont le **titre** ou la **description** contient les mots-cles saisis.
- **Stockage local** : les taches sont enregistrees dans un fichier JSON local (`~/.gestion-tache/tasks.json`), sans base de donnees ni serveur distant.

## Prerequis

- Java 21+
- Maven 3.9+

## Lancer l'application

```bash
mvn javafx:run
```

## Construire un jar executable

```bash
mvn package
java -jar target/gestion-tache-1.0.0.jar
```

## Lancer les tests

```bash
mvn test
```

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
```
