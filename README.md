# Gestion des taches

Client lourd JavaFX pour gerer des taches quotidiennes de travail, avec
report des taches non terminees et recherche plein texte. Aucune donnee
ne quitte la machine : tout est stocke en local.

## Fonctionnalites

- **Vue par jour** : navigation entre les jours (precedent / suivant /
  aujourd'hui) pour afficher les taches du jour selectionne.
- **Ajout / modification / suppression** de taches (titre, description,
  priorite, date), avec case a cocher pour marquer une tache comme terminee.
- **Priorites** : chaque tache a un niveau (Haute / Moyenne / Basse),
  affiche sous forme de badge colore dans la liste et le detail, et
  filtrable via le menu deroulant de la barre du haut.
- **Detail de tache** : cliquer sur une tache affiche son detail complet
  (titre, date, statut, priorite, description) dans un panneau a droite.
- **Classement manuel** : dans la vue par jour (sans filtre de priorite),
  les taches peuvent etre glissees-deposees pour changer leur ordre.
- **Taches recurrentes** : une tache peut etre configuree en Quotidienne,
  Hebdomadaire ou Jours ouvres ; la marquer comme terminee cree
  automatiquement la prochaine occurrence (non terminee) a la bonne date.
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

## Configuration IDE

Le projet est un module Maven standard (pas de `module-info.java`,
classpath simple) : n'importe quel IDE qui sait importer un `pom.xml`
resout automatiquement les dependances JavaFX, aucune configuration
manuelle de module-path n'est necessaire.

### IntelliJ IDEA

1. `File > Open...` puis selectionner le dossier du projet (le
   `pom.xml` a la racine est detecte automatiquement).
2. Attendre la resolution des dependances Maven (barre de progression
   en bas a droite).
3. Ouvrir `src/main/java/com/gestiontache/MainApp.java` et cliquer sur
   le triangle vert a cote de `public static void main` (ou clic droit
   > `Run 'MainApp.main()'`).
4. Pour lancer les tests : clic droit sur `src/test/java` >
   `Run 'All Tests'`, ou l'onglet Maven > `Lifecycle > test`.

### Visual Studio Code

1. Installer les extensions **Extension Pack for Java** et
   **Maven for Java** (toutes deux de Microsoft/Red Hat).
2. Ouvrir le dossier du projet : VS Code detecte le `pom.xml` et
   propose d'importer le projet Maven.
3. Lancer via la palette de commandes `Maven: Execute Commands` >
   `javafx:run`, ou depuis un terminal integre avec `mvn javafx:run`.
4. Lancer directement `MainApp` depuis l'explorateur Java n'est
   possible qu'apres un premier `mvn compile` (pour que les
   dependances JavaFX soient indexees).

### Eclipse

1. `File > Import... > Maven > Existing Maven Projects`, selectionner
   le dossier du projet.
2. Clic droit sur le projet > `Run As > Maven build...`, avec comme
   Goals `javafx:run`.
3. Pour un lancement direct de `MainApp`, s'assurer que le plugin
   **m2e** a bien telecharge les dependances (clic droit sur le
   projet > `Maven > Update Project`).

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
