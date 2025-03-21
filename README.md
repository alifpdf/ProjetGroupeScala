# Projet Finance Scala

Ce projet est une application web permettant de suivre en temps réel les prix des cryptomonnaies, gérer les investissements et visualiser les données sous forme de graphiques dynamiques.  

## Fonctionnalités  

- Suivi des prix des cryptomonnaies en temps réel
- Notifications des événements marquants
- Visualisation des données avec Chart.js
- Gestion et stratégies d'investissement
- Intégration de l'API OKX pour récupérer les données du marché
- Historique des achats réalisés
- Taux de rendement des différents investissements réalisés

---

## Technologies Utilisées  

### Back-end  
- **Akka** : Gestion des acteurs et programmation asynchrone  
- **Scala Build Tool (sbt)** : Outil de build pour le projet Scala  
- **PostgreSQL** : Base de données relationnelle  
- **JDBC** : Interface pour exécuter des requêtes SQL  
- **Slick** : Bibliothèque Scala pour interagir avec PostgreSQL  
- **Play Framework** : Framework web pour créer l'API backend  
- **Okx API & Marketstack** : Récupération des données du marché crypto  

### Front-end  
- **Node.js** : Gestion des dépendances et exécution du projet React  
- **React.js** : Framework JavaScript pour le développement de l'interface utilisateur  
- **Lucide-react** : Icônes modernes pour une meilleure UI/UX  
- **Chart.js** : Visualisation de données financières avec des graphiques interactifs  

### Temps réel & Interactivité  
- **WebSocket** : Notifications et mises à jour en temps réel  
- **Akka Actors** : Gestion des opérations CRUD et événements asynchrones  

---

## Installation & Configuration  

### Prérequis  
- **Node.js** & **npm**  
- **Scala & sbt**  
- **PostgreSQL** installé et configuré
- **Docker**
