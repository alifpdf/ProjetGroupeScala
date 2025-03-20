### Rapport d’Architecture du Projet Scala - Gestion de Portefeuilles d’Investissement en Cryptomonnaies

### Introduction
Ce document présente l’architecture complète de la plateforme développée pour gérer des portefeuilles d’investissement en cryptomonnaies. Le projet vise à offrir une solution robuste, scalable et réactive permettant :
La visualisation en temps réel des performances
Les calculs financiers (NAV, Sharpe Ratio)
Une communication temps réel via WebSockets

### Attribution des rôles
Back-end :
ALI SEKANDER Alif: Base de données, Acteur, Akka stream, Websocket
Provent Amaury:Acteur, Base de données, Web socket
Bedué Lucas: Main, Docker, Base de données
Front-end:
Test du bon fonctionnement du back-end appliqué au front-end: ALI SEKANDER Alif, PROVENT Amaury
React JS: ALI SEKANDER Alif, PROVENT Amaury
Calcul Financière: PROVENT Amaury
Login et Inscription: ALI SEKANDER Alif
Décoration site:BEDUE Lucas, PROVENT Amaury
 

### Architecture Backend - Scala & Akka
Le backend est construit en Scala, exploitant les puissants modules Akka pour la gestion des flux, la concurrence et la réactivité.
 Principaux Composants Backend :
Akka Streams
Mise à jour automatique des portefeuilles selon les prix récupérés de l’API
Envoi de flux JSON vers le frontend (via WebSocket)
Akka Actors
InvestmentActor : Ajout / Suppression / Mise à jour d’investissements
UtilisateurActor : Gestion des utilisateurs, soldes et authentification
ProductsActor : Gestion des produits liés aux investissements
SocketActor : Envoi de notifications privées ou broadcast aux utilisateurs
Fichier Db:
DBInvestment : Ajout / Suppression / Mise à jour d’investissements
DBUtilisateur : Gestion des utilisateurs, soldes et authentification
DBProducts : Gestion des produits liés aux investissement
DBNotification

### WebSocketsServor
WebSocketServer expose l’API REST et la WebSocket
Reponse/requête One shot pour les requête concernant une communication avec la base de données
Reponse/requête One shot pour les requête concernant une calcul financière
Route d’investissement, calculs financiers et notifications
Communication bidirectionnelle backend/frontend pour la mise à jour des actions sans besoin de recharger la page
Récupération des Prix
OkxAPI : Récupération des prix BTC/ETH/DOGE depuis OKX Exchange
MarketstackDataFetcher : Récupération d’historique de prix pour l’analyse



### Base de Données - PostgreSQL avec Slick
Modélisation relationnelle :
Users : Données utilisateurs, solde
Investments : Historique des investissements
Products : Détail des produits financiers liés aux investissements
Notifications : Journalisation des messages envoyés
 Exécution automatisée du script SQL
Chargement des tables au démarrage via Main.scala

### Frontend - ReactJS Moderne & Connecté
Le frontend propose une interface interactive et réactive en ReactJS, pilotée par WebSocket et REST.
###Principaux Composants :
App.js
Navigation entre les pages (Login, Inscription, Graphiques)
Connexion WebSocket automatique pour écouter les mises à jour et notifications
LoginForm.js
Authentification via /api/login
Centre de notifications :
Réception temps réel via WebSocket
Suppression des notifications
Pagination intégrée
InvestmentStrategies.js
Calculs financiers avancés :
NAV (Valeur liquidative nette)
Sharpe Ratio
Volatilité
Détection automatique de stratégie d’investissement recommandée (Défensive, Équilibrée, Agressive)
Synchronisation WebSocket et récupération dynamique des prix crypto


### RealTimeCharts.js
Affiche en temps réel :
La répartition des actifs BTC, ETH, DOGE
Les courbes d’évolution des prix et du portefeuille


### Calcul de Stratégies d'Investissement

Rendement Simple
rendement simple d’un actif sur une période donnée

Rt​=Pt−Pt-1​​Pt-1​​×100
Rt​ = Rendement sur la période t
Pt​ = Prix de l’actif à la fin de la période
Pt-1 = Prix de l’actif au début de la période

Rendement sur plusieurs périodes
rendement sur plusieurs périodes successives

Rcumulé=i=1n1+Ri-1
Ri​ = Rendement sur chaque période
n = Nombre de périodes

Volatilité
mesure du risque qui représente la variation des rendements

 =1nRi-R2
 = Volatilité
Ri = Rendement sur chaque période
R = Rendement moyen







### Sharpe Ratio
mesurer la performance ajustée au risque

S = Rp-Rfp
S   = Ratio de Sharpe
Rp = Rendement moyen du portefeuille
Rf = Rendement sans risque
p = Volatilité (écart type des rendements)

S>1 : Bonne performance ajustée au risque 
S>2 : Très bon portefeuille 
S<1 : Risque trop élevé pour le rendement obtenu


### Valeur nette du portefeuille (Net Asset Value)
la valeur totale des actifs d’un portefeuille après soustraction des dettes éventuelles

NAV =QiPi-D
Qi = Quantité de l’actif i
Pi = Prix actuel de l’actif iii
D = Dettes éventuelles (ex : marges, prêts)


### Technologie utiliser pour le Projet

Projet en Akka
scala build tools (sbt) pour builds
postgres pour les base de données
jdbc pour les requêtes SQL
bibliothèque Slick pour la connexion à la base de données
node.js et React  pour le web
Play Framework pour l’applis web
Chart.js pour la visualisation dynamique de graphique
WebSocket pour les notifications en temps réel vers les utilisateurs
Lucide-react pour le visuel
Okx API pour la relation au crypto
opération CRUD (Create, Read, Update, Delete) avec les Actor
