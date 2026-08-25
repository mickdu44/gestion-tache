# TODO

Liste des evolutions et corrections envisagees pour l'application.

## Evolutions fonctionnelles

- [x] Priorites (haute, moyenne, basse) sur les taches, visibles dans la liste et le detail, filtrables via la barre du haut. Reste a discuter : etiquettes/categories personnalisees au-dela des 3 niveaux fixes.
- [ ] text enrichi dans la description des tâches
- [ ] Taches recurrentes (quotidienne, hebdomadaire, jours ouvres...).
- [ ] Vue "semaine" ou "liste de toutes les taches en retard" en plus de la vue par jour.
- [x] Export / import CSV des taches (titre, description, date, priorite, statut) pour sauvegarde ou partage. Reste a discuter : format iCal.
- [ ] Statistiques simples (taches terminees par jour/semaine, taux de completion).
- [ ] Annulation (undo) apres suppression d'une tache.
- [ ] Theme sombre / clair configurable.
- [ ] Tri et filtres supplementaires dans les resultats de recherche (par date, par statut).

## Corrections / robustesse technique

- [ ] Gerer l'acces concurrent au fichier `tasks.json` (verrou ou detection d'ecriture concurrente si plusieurs instances de l'app sont lancees).
- [ ] Sauvegarde/versionning automatique du fichier de donnees (ex. copie horodatee avant chaque ecriture) pour eviter une perte de donnees en cas de fichier corrompu.
- [ ] Limiter la longueur du titre/description dans le formulaire d'ajout/edition et afficher un message d'erreur explicite en cas de donnees invalides.
- [ ] Ajouter des tests pour les controleurs JavaFX (actuellement seule la couche `TaskService` est testee).
- [ ] Mettre en place une pipeline CI (GitHub Actions) executant `mvn test` sur chaque pull request vers `develop`.
- [ ] Packaging natif (jpackage) pour distribuer l'application sans necessiter un JDK installe (installeurs Windows/macOS/Linux).
- [ ] Internationalisation (l'interface est actuellement uniquement en francais).

## Idees a discuter

- [ ] Synchronisation optionnelle entre plusieurs machines (fichier partage, cloud) tout en gardant le stockage local par defaut.
- [ ] Sous-taches / checklist a l'interieur d'une tache.
