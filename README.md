# 📚 Bibliothèque — Cégep

Application Java de gestion de bibliothèque scolaire avec interface JavaFX, persistance JSON et gestion de la concurrence.

---

## Table des matières

- [Prérequis](#prérequis)
- [Installation et exécution](#installation-et-exécution)
- [Structure du projet](#structure-du-projet)
- [Fonctionnalités](#fonctionnalités)
- [Utilisation de l'application](#utilisation-de-lapplication)
- [Lancer les tests](#lancer-les-tests)
- [Importer un fichier MARC](#importer-un-fichier-marc)
- [Sauvegarde et chargement](#sauvegarde-et-chargement)

---

## Prérequis

- **Java 17** ou supérieur
- **Maven 3.8+**
- **IntelliJ IDEA** (recommandé)

---

## Installation et exécution

### Avec IntelliJ IDEA

1. Cloner le dépôt :
```bash
git clone <url-du-depot>
cd bibliotheque
```

2. Ouvrir IntelliJ → **File > Open** → sélectionner le dossier `bibliotheque/`
3. Attendre que Maven télécharge les dépendances
4. Lancer `src/main/java/ca/cegep/biblio/gui/App.java` avec le bouton ▶️

### Avec Maven (terminal)

```bash
cd bibliotheque
mvn clean javafx:run
```

---

## Structure du projet

```
bibliotheque/
| pom.xml
| README.md
| .gitignore
| src/
    | main/java/ca/cegep/biblio/
        | model/          → Classes métier (Exemplaire, Usager, Emprunt, enums)
        | service/        → Logique métier (BibliothequeService, EmpruntService, etc.)
        | persistence/    → Sauvegarde/chargement JSON (JsonPersistence)
        | concurrency/    → Gestion des threads (RepairScheduler, SimulationManager)
        | util/           → Utilitaires (DateUtils, IdGenerator)
        | gui/            → Interface JavaFX (App, contrôleurs, vues FXML)
    | main/resources/
        | styles.css
    | test/java/ca/cegep/biblio/
        | unit/           → Tests unitaires
        | concurrency/    → Tests de simulation multithread
| data/
    | cegep.iso2709       → Fichier MARC source
    | sauvegarde.json     → Fichier de sauvegarde généré automatiquement
```

---

## Fonctionnalités

### Gestion des livres
- Ajouter, modifier et supprimer des exemplaires
- Chaque ouvrage peut avoir plusieurs exemplaires physiques distincts
- Statuts : `DISPONIBLE`, `EMPRUNTE`, `A_REPARER`
- États physiques : `NEUF`, `BON`, `USE`, `A_REPARER`
- Filtre en temps réel dans le tableau

### Gestion des usagers
- Trois types avec règles différentes :

| Type       | Max livres | Durée emprunt |
|------------|-----------|---------------|
| Étudiant   | 3         | 14 jours      |
| Professeur | 6         | 30 jours      |
| Visiteur   | 1         | 7 jours       |

- Suppression refusée si l'usager a des emprunts actifs

### Gestion des emprunts
- Emprunt avec vérification automatique des règles
- Retour avec saisie de l'état physique
- Détection automatique des emprunts en retard
- Compteur global cumulatif des emprunts (AtomicInteger)

### Réparation automatique
- Un exemplaire retourné en état `A_REPARER` est bloqué 3 jours ouvrables
- Chaque jour à 8h00, les exemplaires dont la réparation est terminée redeviennent `DISPONIBLE` automatiquement
- Les weekends sont exclus du calcul

### Recherche
- Recherche par titre, auteur ou ISBN
- Résultats triés alphabétiquement
- Autocomplétion des ISBNs
- Affichage de la disponibilité et de la date de retour la plus proche si tous les exemplaires sont empruntés

### Concurrence
- Les collections sont protégées avec `synchronized`
- Les threads sont gérés via `ExecutorService`
- Aucune collection concurrente (ConcurrentHashMap, etc.) n'est utilisée

---

## Utilisation de l'application

### Démarrage
L'application s'ouvre en **plein écran**. Les données de la dernière session sont chargées automatiquement depuis `data/sauvegarde.json`.

### Navigation
La barre de navigation en haut donne accès aux 4 sections :
- **📖 Livres** — gestion du catalogue
- **👤 Usagers** — gestion des membres
- **🔄 Emprunts** — emprunts, retours et retards
- **🔍 Recherche** — recherche dans le catalogue

### Emprunter un livre
1. Aller dans **🔄 Emprunts** → onglet **➕ Nouvel emprunt**
2. Sélectionner l'usager dans la liste
3. Taper l'ISBN (l'autocomplétion propose les ISBNs disponibles)
4. La disponibilité s'affiche en temps réel
5. Cliquer **✅ Confirmer l'emprunt**

### Retourner un livre
1. Aller dans **🔄 Emprunts** → onglet **🔄 Retour**
2. Sélectionner l'usager
3. Sélectionner l'exemplaire parmi ses emprunts en cours
4. Indiquer l'état physique (`NEUF`, `BON`, `USE`, `A_REPARER`)
5. Cliquer **✅ Confirmer le retour**

> ⚠️ Les retours ne sont acceptés que les jours ouvrables (lundi au vendredi).

### Sauvegarder
- Cliquer **💾 Sauvegarder** dans la navbar à tout moment
- La sauvegarde s'effectue aussi **automatiquement** à la fermeture de l'application
- Le fichier est enregistré dans `data/sauvegarde.json`

### Fermer l'application
Cliquer le bouton **✕** en haut à droite — la sauvegarde est effectuée automatiquement avant la fermeture.

---

## Lancer les tests

```bash
mvn test
```

Les tests couvrent :
- CRUD des exemplaires et usagers
- Règles d'emprunt par type d'usager
- Saturation et libération des exemplaires
- Calcul des jours ouvrables pour les réparations
- Persistance JSON (sauvegarde et chargement)
- Recherche et statistiques
- Simulation multithread (race conditions, AtomicInteger)

---

## Importer un fichier MARC

Le fichier `data/cegep.iso2709` est au format ISO 2709 (MARC). Il est parsé au démarrage si aucune sauvegarde n'existe, ou via le menu d'import dans l'application.

Chaque notice génère entre 1 et 4 exemplaires selon la distribution suivante :
- 50% → 1 exemplaire
- 30% → 2 exemplaires
- 15% → 3 exemplaires
- 5%  → 4 exemplaires

---

## Sauvegarde et chargement

L'état complet (exemplaires, usagers, emprunts) est sauvegardé dans un seul fichier JSON :

```
data/sauvegarde.json
```

Le fichier est :
- **chargé automatiquement** au démarrage
- **sauvegardé automatiquement** à la fermeture
- **sauvegardable manuellement** via le bouton 💾

---

## Dépendances principales

| Dépendance | Version | Usage |
|---|---|---|
| JavaFX | 21 | Interface graphique |
| Gson | 2.10.1 | Sérialisation JSON |
| JUnit Jupiter | 5.10.0 | Tests unitaires |

---

## Auteur

Projet réalisé dans le cadre du cours de Programmation 2 — Cégep.
