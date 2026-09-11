# Gestion des taches

Client lourd JavaFX pour gerer des taches quotidiennes de travail, avec
report des taches non terminees et recherche plein texte. Aucune donnee
ne quitte la machine : tout est stocke en local.

## Fonctionnalites

- **Style Windows 11 (Fluent Design)** : fond neutre type Mica, cartes
  blanches aux coins arrondis avec ombre douce, accent bleu Windows 11 sur
  les boutons principaux, cases a cocher et champs actifs, et champs de
  saisie avec soulignement plutot que des cadres pleins.
- **Vue par jour** : navigation entre les jours (precedent / suivant /
  aujourd'hui) pour afficher les taches du jour selectionne.
- **Vue semaine** : bascule "Jour" / "Semaine" dans la barre du haut pour
  afficher les taches de toute une semaine (lundi a dimanche) au lieu d'un
  seul jour, chaque tache affichant un badge avec sa date. Les boutons
  precedent/suivant avancent alors d'une semaine entiere.
- **Vue Kanban (vue par defaut au lancement)** : bascule "Kanban" dans la
  barre du haut pour afficher les taches du jour selectionne en trois
  colonnes (A faire / En cours / Terminee) au lieu d'une liste unique,
  chaque colonne affichant son nombre de taches. Le panneau de detail
  lateral est alors entierement retire de la fenetre (les colonnes
  occupent toute la largeur disponible). Chaque tache y est representee
  par une carte au visuel post-it (fond jaune pale, ombre portee) qui
  rend directement visibles, sans avoir a ouvrir son detail : un extrait
  de sa description (tronque a 100 caracteres), et jusqu'a 5 de ses
  sous-taches encore actives (non terminees) sous forme de cases a cocher
  directement actionnables depuis la carte (une sous-tache cochee/decochee
  ainsi est enregistree et journalisee immediatement, comme depuis le
  formulaire de detail) ; une fois cochee, une sous-tache disparait de
  cette liste (le badge "x/y" reste la pour suivre la progression
  d'ensemble) ; au-dela de 5 sous-taches actives, un "+ N autre(s)"
  indique le reste. Les cartes se glissent-deposent librement : deposer
  une carte sur une autre l'insere juste a cet endroit, que ce soit pour
  la reordonner manuellement au sein de sa colonne ou pour la deplacer
  vers une autre colonne (ce qui change alors son statut, y compris pour
  la reclasser dans "A faire") ; la deposer dans l'espace vide d'une
  colonne l'ajoute simplement a la fin de celle-ci. Une carte n'a pas de
  case a cocher pour la marquer terminee directement (contrairement aux
  vues Jour/Semaine) : il faut la glisser dans la colonne "Terminee", ou
  passer par son detail. Le menu (⋮) d'une carte propose "Modifier", qui
  ouvre son detail complet dans une popin (voir ci-dessous).
  Les cartes n'ont pas de bouton de suppression (contrairement aux vues
  Jour/Semaine) ; supprimer une tache reste possible depuis la vue Jour
  ou Semaine. La navigation par jour, le filtre de priorite et le tri
  par priorite restent actifs dans cette vue.
- **Calendrier de selection** : un champ de date dans la barre du haut
  permet de sauter directement a n'importe quel jour (ou a la semaine qui
  le contient, en vue semaine) sans avoir a cliquer plusieurs fois sur
  precedent/suivant.
- **Ajout / modification / suppression** de taches. L'ajout se fait via un
  formulaire dedie qui s'ouvre dans une popin ; la modification se fait
  directement dans le panneau de detail (voir ci-dessous), ouvert via le
  bouton "Modifier" de la tache (case a cocher directe pour marquer une
  tache comme terminee, sans avoir a ouvrir son detail).
- **Priorites** : chaque tache a un niveau (Haute / Moyenne / Basse),
  affiche sous forme de badge colore dans la liste et le detail, et
  filtrable via le menu deroulant de la barre du haut. Une case "Trier
  par priorite" permet de trier la liste affichee par priorite (Haute
  d'abord) au lieu de l'ordre manuel ; le glisser-deposer est alors
  desactive puisqu'il n'y a plus d'ordre manuel a modifier.
- **Statut "En cours"** : en plus de terminee/non terminee, une tache
  peut etre marquee "En cours" (menu deroulant "Statut" du formulaire),
  affiche sous forme de badge distinct dans la liste et le detail.
- **Les taches terminees passent en fin de liste** : quel que soit
  l'affichage (jour, semaine, recherche, tri par priorite), les taches
  terminees sont toujours regroupees a la fin de la liste.
- **Detail de tache editable** : en vue Jour/Semaine, le bouton "Modifier"
  d'une tache affiche son detail complet (titre, priorite, recurrence,
  statut, date, description, sous-taches, pieces jointes) dans un panneau
  lateral qui occupe la moitie de la fenetre, et chaque champ y est
  directement modifiable. En vue Kanban, le crayon (✎) d'une carte ouvre
  ce meme detail dans une popin au premier plan (modale : le reste de la
  fenetre est bloque tant qu'elle est ouverte), dimensionnee a la moitie
  de la largeur et de la hauteur de l'ecran. Dans les deux cas,
  selectionner ou cliquer une tache sans passer par ce bouton n'ouvre
  plus son detail, pour eviter de l'ouvrir par accident en parcourant ou
  en reordonnant la liste. Le titre et la description sont enregistres
  quand on quitte le champ ; les autres champs (priorite, recurrence,
  statut, date, sous-taches, pieces jointes) sont enregistres
  immediatement. Plus besoin de rouvrir une fenetre pour corriger une
  tache existante.
- **Texte enrichi, avec apercu Markdown** : la description accepte une mise
  en forme simple — `**gras**`, `*italique*`, des lignes commencant par
  `- ` pour une liste a puces, et `[texte](url)` pour un lien cliquable.
  En consultation, la description s'affiche deja mise en forme (gras,
  italique, puces, lien cliquable) plutot que sous sa syntaxe brute ;
  cliquer dessus bascule en modification pour voir/editer le texte brut
  avec sa barre d'outils (boutons G, I, Liste, Lien), et cliquer ailleurs
  revient a l'apercu forme. Ce comportement est le meme dans le formulaire
  d'ajout et dans le panneau de detail.
- **Pieces jointes** : chaque tache peut avoir un ou plusieurs fichiers
  locaux attaches (bouton "Ajouter une piece jointe" dans le formulaire
  d'ajout ou dans le panneau de detail) ; ils apparaissent comme des liens
  cliquables qui les ouvrent avec l'application par defaut du systeme.
- **Sous-taches / checklist** : chaque tache peut avoir une liste de
  sous-taches cochables, ajoutees, cochees et supprimees directement
  depuis le panneau de detail (ou depuis le formulaire d'ajout pour une
  nouvelle tache), avec un badge de progression (ex. "2/5") visible dans
  la liste.
- **Classement manuel** : dans la vue par jour (sans filtre de priorite),
  les taches peuvent etre glissees-deposees pour changer leur ordre.
- **Historique des changements** : chaque tache garde un journal date de
  ses modifications (creation, titre, description, priorite, recurrence,
  statut, date, sous-taches, pieces jointes, marquage termine/non
  termine, report), visible dans une section "Historique" en bas du
  panneau de detail, du plus recent au plus ancien.
- **Taches recurrentes** : une tache peut etre configuree en Quotidienne,
  Hebdomadaire ou Jours ouvres ; la marquer comme terminee cree
  automatiquement la prochaine occurrence (non terminee) a la bonne date.
- **Statistiques** : bouton "Statistiques" affichant le nombre total de
  taches, le taux de completion, et un graphique des taches terminees
  sur les 7 derniers jours.
- **Report automatique des taches non terminees** :
  - bouton "Reporter les taches non terminees a demain" pour reporter
    au lendemain les taches non terminees du jour affiche ;
  - au demarrage, les taches non terminees de jours precedents sont
    automatiquement reportees a aujourd'hui, sans confirmation requise.
- **Recherche** : la barre de recherche filtre l'ensemble des taches
  (tous les jours confondus, tous fichiers confondus) dont le **titre**
  ou la **description** contient les mots-cles saisis (insensible a la
  casse).
- **Stockage local, un fichier JSON par jour** : les taches sont
  enregistrees dans `~/.gestion-tache/days/AAAA-MM-JJ.json`, un
  fichier par journee contenant l'ensemble de ses taches. Une
  modification (ajout, edition, suppression, report) ne reecrit que
  le(s) fichier(s) du/des jour(s) concerne(s) ; un jour vide n'a pas
  de fichier. Aucune base de donnees ni serveur distant.

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

Les taches sont lues/ecrites dans `~/.gestion-tache/days/`, un fichier
JSON par jour nomme `AAAA-MM-JJ.json` (ex. `2026-03-10.json`) contenant
la liste des taches de cette journee. Supprimer ce dossier reinitialise
l'application (perte de toutes les taches).

Si une ancienne installation avait deja produit un fichier unique
`~/.gestion-tache/tasks.json`, il est automatiquement reparti en
fichiers journaliers au premier lancement suivant la mise a jour ;
l'ancien fichier est conserve tel quel (non supprime) a titre de
sauvegarde.

## Structure du projet

```
src/main/java/com/gestiontache/
  MainApp.java                     Point d'entree JavaFX
  Launcher.java                    Point d'entree pour le jar shade (java -jar)
  model/Task.java, SubTask.java,
    TaskStatus.java                Modele d'une tache, ses sous-taches et son statut
  repository/TaskRepository.java   Persistance JSON locale (un fichier par jour)
  service/TaskService.java         Logique metier (recherche, report, CRUD)
  controller/                      Controleurs JavaFX (FXML)
  util/DescriptionFormatter.java   Rendu du texte enrichi (gras, italique, liens)
src/main/resources/com/gestiontache/
  main.fxml, task-dialog.fxml      Vues
  style.css                        Style de l'application
  icon.png                         Icone de l'application
src/test/java/com/gestiontache/    Tests unitaires (JUnit 5)
```

## Contribuer

Le projet suit le workflow **Gitflow** (`main` / `develop` /
`feature/*` / `release/*` / `hotfix/*`) : voir [CONTRIBUTING.md](CONTRIBUTING.md)
pour le detail des conventions.

Les evolutions et corrections a venir sont listees dans [TODO.md](TODO.md).
