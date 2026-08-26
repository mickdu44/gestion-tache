# TODO

Liste des evolutions et corrections envisagees pour l'application.

## Evolutions fonctionnelles

- [x] Priorites (haute, moyenne, basse) sur les taches, visibles dans la liste et le detail, filtrables via la barre du haut. Reste a discuter : etiquettes/categories personnalisees au-dela des 3 niveaux fixes.
- [x] Texte enrichi dans la description des taches (gras, italique, listes a puces).
- [ ] Taches recurrentes (quotidienne, hebdomadaire, jours ouvres...).
- [ ] Vue "semaine" ou "liste de toutes les taches en retard" en plus de la vue par jour.
- [ ] Export / import des taches (CSV, iCal) pour sauvegarde ou partage.
- [ ] Statistiques simples (taches terminees par jour/semaine, taux de completion).
- [ ] Annulation (undo) apres suppression d'une tache.
- [ ] Theme sombre / clair configurable.
- [ ] Tri et filtres supplementaires dans les resultats de recherche (par date, par statut).
- [ ] Supprimer le tag en cours et agrandir l'affichage latéral pour la description sur la moitié de l'écran
- [ ] Afficher des boutons pour facilité la mise en page du texte enrichie et ajouter la possibilité de mettre un lien url et une pièce jointe
- [ ] Sous-taches / checklist a l'interieur d'une tache.
- [ ] Les tâches non terminées sont automatiquement reportées au lendemain
- [x] Stockage journalier : un fichier JSON par jour (`~/.gestion-tache/days/AAAA-MM-JJ.json`) au lieu d'un unique fichier global ; migration automatique depuis l'ancien format au premier lancement.

## Corrections / robustesse technique

- [ ] Gerer l'acces concurrent aux fichiers journaliers de `~/.gestion-tache/days/` (verrou ou detection d'ecriture concurrente si plusieurs instances de l'app sont lancees).
- [ ] Sauvegarde/versionning automatique du fichier de donnees (ex. copie horodatee avant chaque ecriture) pour eviter une perte de donnees en cas de fichier corrompu.
- [ ] Limiter la longueur du titre/description dans le formulaire d'ajout/edition et afficher un message d'erreur explicite en cas de donnees invalides.
- [ ] Ajouter des tests pour les controleurs JavaFX (actuellement seule la couche `TaskService` est testee).
- [ ] Mettre en place une pipeline CI (GitHub Actions) executant `mvn test` sur chaque pull request vers `develop`.
- [ ] Packaging natif (jpackage) pour distribuer l'application sans necessiter un JDK installe (installeurs Windows/macOS/Linux).
- [ ] Internationalisation (l'interface est actuellement uniquement en francais).

## Idees a discuter

- [ ] Synchronisation optionnelle entre plusieurs machines (fichier partage, cloud) tout en gardant le stockage local par defaut.

