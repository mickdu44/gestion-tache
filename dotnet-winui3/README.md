# Gestion des taches — client .NET / WinUI 3

Portage du client lourd JavaFX vers **.NET 8 + WinUI 3** (Windows App SDK),
sous forme d'application de bureau Windows non empaquetee (pas de MSIX).

## Perimetre (MVP)

Cette premiere version couvre le coeur de l'application :

- Vue par jour, navigation precedent / suivant / aujourd'hui.
- Calendrier (`CalendarDatePicker`) pour sauter directement a une date.
- Ajout / modification / suppression de taches (titre, description,
  priorite, date), case a cocher pour marquer une tache terminee.
- Priorites (Haute / Moyenne / Basse), filtrables via un menu deroulant.
- Recherche plein texte (titre + description) sur l'ensemble des jours.
- Report des taches non terminees : bouton manuel "au lendemain", et
  report automatique et silencieux des taches en retard vers aujourd'hui
  au demarrage.
- Stockage local, un fichier JSON par jour.

Les evolutions plus recentes de la version JavaFX (texte enrichi, pieces
jointes, sous-taches, recurrence, statistiques, vue semaine) **ne sont pas
portees dans ce MVP** ; elles pourront faire l'objet d'iterations suivantes
si ce client .NET est retenu.

## Compatibilite des donnees avec le client JavaFX

Ce client lit et ecrit dans **le meme dossier** que le client JavaFX :
`~/.gestion-tache/days/AAAA-MM-JJ.json` (un fichier JSON par jour, meme
noms de champs : `id`, `title`, `description`, `date`, `completed`,
`createdAt`, `order`, `priority`). Les deux clients peuvent donc pointer
sur les memes donnees.

Les champs plus recents du modele Java (`recurrence`, `subtasks`,
`attachments`) ne sont pas geres par l'UI de ce MVP, mais ne sont **pas
perdus** : `TaskItem` les conserve tels quels (`[JsonExtensionData]`) et
les reecrit intacts si une tache du meme jour est modifiee depuis ce
client. Seuls les champs geres par ce MVP peuvent etre modifies depuis
cette UI.

## Prerequis

- **Windows 10 (build 17763+) ou Windows 11.**
- [.NET 8 SDK](https://dotnet.microsoft.com/download/dotnet/8.0).
- [Windows App SDK](https://learn.microsoft.com/windows/apps/windows-app-sdk/set-up-your-development-environment)
  (installe automatiquement via NuGet a la restauration du projet).
- Visual Studio 2022 (17.8+) avec la charge de travail **".NET Desktop
  Development"** et le composant **"Windows App SDK C# Templates"**, ou
  la CLI `dotnet` seule (voir ci-dessous).

## Build et lancement

Depuis Visual Studio 2022 :

1. Ouvrir `dotnet-winui3/GestionTaches.sln`.
2. Selectionner une plateforme de demarrage (x64 recommande).
3. `F5` pour compiler et lancer.

En ligne de commande (PowerShell, sur Windows) :

```powershell
cd dotnet-winui3\GestionTaches
dotnet restore
dotnet build -c Debug -p:Platform=x64
dotnet run -c Debug -p:Platform=x64
```

## Structure du projet

```
dotnet-winui3/
  GestionTaches.sln
  GestionTaches/
    GestionTaches.csproj        Projet WinUI3 non empaquete (net8.0-windows)
    app.manifest                Declarations DPI / compatibilite OS
    App.xaml(.cs)                Point d'entree de l'application
    MainWindow.xaml(.cs)          Fenetre principale (vue jour, liste, recherche)
    TaskRow.cs                   Wrapper d'affichage d'une tache pour le ListView
    Models/
      Priority.cs                Enum de priorite (Haute/Moyenne/Basse)
      TaskItem.cs                 Modele d'une tache (memes champs JSON que Java)
    Services/
      TaskRepository.cs           Persistance JSON locale (un fichier par jour)
      TaskService.cs               Logique metier (recherche, report, CRUD)
    Dialogs/
      TaskEditDialog.xaml(.cs)     Formulaire d'ajout/edition (ContentDialog)
```

## Limite connue de cette contribution

Ce projet a ete redige dans un environnement Linux qui ne dispose pas des
outils WinUI 3 (le compilateur XAML et les outils `Microsoft.Windows.SDK.BuildTools`
sont Windows uniquement) : **il n'a donc pas pu etre compile ni execute
avant cette livraison.** Le code suit les conventions standard d'un projet
WinUI 3 non empaquete (structure de fichiers, `x:Bind`, `ContentDialog`,
etc.), mais merite une premiere compilation sur une machine Windows pour
detecter d'eventuelles erreurs avant tout usage.
