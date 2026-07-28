# Application de Gestion des Vendeurs (Projet 6)

Cette application est un système de gestion permettant de manipuler les données relatives aux vendeurs via une interface graphique interactive. Elle prend en charge les opérations CRUD de base ainsi qu'une fonctionnalité de recherche.

---

## 📌 Table des Matières
- [Description du Projet](#-description-du-projet)
- [Structure de la Base de Données](#-structure-de-la-base-de-données)
- [Fonctionnalités Principales](#-fonctionnalités-principales)
- [Technologies Utilisées](#-technologies-utilisées)
- [Installation et Configuration](#-installation-et-configuration)
- [Utilisation](#-utilisation)

---

## 📝 Description du Projet

L'objectif de ce projet est d'offrir une solution simple et efficace pour la gestion de l'annuaire des vendeurs. L'application permet d'ajouter, d'afficher, de modifier, de supprimer et de rechercher des enregistrements de vendeurs, en incluant leurs informations personnelles et leur photo de profil.

---

## 🗄️ Structure de la Base de Données

L'application manipule la table **VENDEUR** avec le schéma suivant :

| Champ | Type | Description |
| :--- | :--- | :--- |
| **idvend** | Clé Primaire | Identifiant unique du vendeur |
| **nom** | Chaîne de caractères | Nom complet du vendeur |
| **datenais** | Date | Date de naissance du vendeur |
| **photo** | Texte / BLOB / URL | Chemin ou données de la photo du vendeur |

---

## 🚀 Fonctionnalités Principales

### 1. Gestion CRUD (Create, Read, Update, Delete)
* **Créer (Create) :** Ajouter un nouveau vendeur avec son nom, sa date de naissance et sa photo.
* **Afficher (Read) :** Consulter la liste complète des vendeurs enregistrés.
* **Modifier (Update) :** Mettre à jour les informations d'un vendeur existant.
* **Supprimer (Delete) :** Retirer un vendeur de la base de données.

### 2. Module de Recherche
* Interface dédiée pour rechercher rapidement un vendeur spécifique par son ID, son nom ou d'autres critères.

---

## 🛠️ Technologies Utilisées

*(Ajustez cette section selon le langage et le SGBD que vous avez utilisés)*

* **Langage de programmation :** Python / Java / PHP / C# (À compléter)
* **Base de données :** MySQL / PostgreSQL / SQLite / Oracle (À compléter)
* **Interface graphique / Framework :** Tkinter / Swing / React / HTML-CSS (À compléter)

---

## ⚙️ Installation et Configuration

1. **Cloner le dépôt :**
   ```bash
   git clone [https://github.com/votre-utilisateur/projet-gestion-vendeurs.git](https://github.com/votre-utilisateur/projet-gestion-vendeurs.git)
   cd projet-gestion-vendeurs
