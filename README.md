# Étude de cas TMA - Prise de rendez-vous au guichet

## Contexte
Le système permet aux usagers de prendre rendez-vous en ligne pour retirer leur passeport, carte d'identité ou permis de conduire au guichet d'un centre de services.

## Structure du projet

Ce projet est codé par moi en Spring Boot et contient :

- **src/main/java/etude_cas_asin/asin_test/**
  - `model/` : Modèles de données (RendezVous, ServiceType, Statut)
  - `dto/` : Objets de transfert de données (DTOs)
  - `service/` : Service des règles de gestion (ReglesGestionService)
  - `exception/` : Exceptions métier personnalisées

- **src/test/java/etude_cas_asin/asin_test/**
  - `service/` : Tests unitaires du service de règles de gestion

- **requetes.sql** : Requêtes SQL d'impact pour analyser la base de données

- **base_rendez_vous.sql** : Export de la base de données fournie

## Lancer les tests

```bash
./mvnw test
```

Ou avec Maven directement :

```bash
mvn test
```

Les tests sont situés dans `src/test/java/etude_cas_asin/asin_test/service/ReglesGestionServiceTest.java`.

## Analyse des tickets

### TMA-101 : Rendez-vous accepté un samedi

**Qualification** : ANOMALIE

**Diagnostic** : L'application ne vérifie pas si le jour de la semaine est un samedi ou un dimanche avant d'accepter un rendez-vous. La règle stipule que "Le centre reçoit du lundi au vendredi".

**Correction** : Ajout d'une vérification dans `ReglesGestionService.verifierWeekend()` qui rejette les rendez-vous planifiés un samedi ou un dimanche avec le message "Le centre est fermé le week-end." (code HTTP 409).

**Test** : `testTMA101_RefuserRendezVousSamedi()` - Tente de créer un rendez-vous un samedi et vérifie qu'une exception 409 est levée avec le bon message.

### TMA-102 : 4 rendez-vous sur le même créneau

**Qualification** : ANOMALIE

**Diagnostic** : L'application ne vérifie pas la capacité maximale du créneau (3 usagers maximum). Le 4ème rendez-vous a été accepté alors que 3 existaient déjà.

**Correction** : Ajout d'une vérification dans `ReglesGestionService.verifierCapaciteCreneau()` qui compte les rendez-vous non annulés sur le créneau et rejette si >= 3 avec le message "Ce créneau est complet." (code HTTP 409).

**Test** : `testTMA102_LimiterTroisParCreneau()` - Crée 3 rendez-vous sur le même créneau, puis tente un 4ème et vérifie qu'il est rejeté.

### TMA-103 : Erreur 500 pour NPI invalide

**Qualification** : ANOMALIE

**Diagnostic** : L'application ne valide pas le format du NPI avant traitement. Quand un NPI contient des lettres, une erreur interne (500) est levée au lieu d'une erreur 400 avec un message explicite.

**Correction** : Ajout d'une validation dans `ReglesGestionService.validerNPI()` qui vérifie que le NPI contient exactement 10 chiffres avec une regex `\\d{10}`. Renvoie une erreur 400 avec le message "Le NPI doit comporter exactement 10 chiffres."

**Test** : `testTMA103_ValiderNPIMalforme()` - Tente de créer un rendez-vous avec un NPI contenant une lettre et vérifie qu'une exception 400 est levée.

### TMA-104 : Annulation refusée à tort

**Qualification** : ANOMALIE

**Diagnostic** : L'application calcule incorrectement le délai avant le rendez-vous. Le 17/11 à 18h pour un rendez-vous le 19/11 à 10h, il y a 40h d'écart, mais l'application l'a refusé en pensant qu'il y avait moins de 24h. Le calcul doit se faire en heures absolues, pas en jours calendaires.

**Correction** : Utilisation de `ChronoUnit.HOURS.between()` pour calculer précisément le nombre d'heures entre maintenant et le rendez-vous. L'annulation est autorisée si >= 24 heures.

**Test** : `testTMA104_AnnulationPlusDe24HeuresAvant()` - Simule une annulation à 40h avant le rendez-vous et vérifie qu'elle est acceptée.

### TMA-105 : Modification d'un rendez-vous annulé

**Qualification** : ANOMALIE

**Diagnostic** : L'application ne vérifie pas si le statut actuel est déjà définitif avant d'accepter un changement. Un rendez-vous annulé (statut définitif) a pu être modifié en "honoré".

**Correction** : Ajout d'une vérification dans `ReglesGestionService.changerStatut()` qui refuse tout changement de statut si le statut actuel est déjà définitif (HONORE, ABSENT, ANNULE). Message : "Ce rendez-vous est déjà clôturé." (code HTTP 409).

**Test** : `testTMA105_RefuserChangementStatutDefinitif()` - Tente de changer le statut d'un rendez-vous annulé et vérifie qu'il est rejeté.

### TMA-106 : Mauvais ordre de tri des rendez-vous

**Qualification** : ANOMALIE

**Diagnostic** : L'application ne trie pas les rendez-vous du plus récent au plus ancien comme spécifié. Le prochain rendez-vous n'apparaît pas en tête de liste.

**Correction** : Ajout d'un tri par `dateHeure` descendant dans `ReglesGestionService.listerRendezVousUsager()` avant la pagination.

**Test** : `testTMA106_TrierRendezVousParDateDecroissante()` - Crée plusieurs rendez-vous avec des dates différentes et vérifie qu'ils sont retournés dans l'ordre décroissant.

### TMA-107 : Usager avec rendez-vous à venir

**Qualification** : COMPORTEMENT CONFORME

**Justification** : La règle stipule "Un usager ne peut avoir qu'un seul rendez-vous à venir, tous services confondus". L'usager a déjà un rendez-vous à venir, donc l'application a raison de refuser le second.

**Test** : `testTMA107_RefuserRendezVousQuandDejaUnAVenir()` - Simule un usager avec un rendez-vous à venir et tente d'en prendre un second, vérifie qu'il est rejeté.

## Requêtes SQL d'impact

Le fichier `requetes.sql` contient les requêtes pour analyser l'impact des anomalies dans la base de données :

1. **Rendez-vous confirmés à venir pris un samedi ou un dimanche** : Permet de prévenir les usagers concernés
2. **Créneaux avec plus de 3 rendez-vous non annulés** : Identifie les créneaux surbookés
3. **Rendez-vous passés passés de "annulé" à un autre statut** : Identifie les changements de statut anormaux
4. **Analyse générale des statuts** : Vue d'ensemble de la répartition
5. **Analyse par service** : Répartition par type de service
6. **Rendez-vous confirmés à venir par jour** : Planning futur
7. **Usagers avec plusieurs rendez-vous à venir** : Détection d'anomalies potentielles

Pour exécuter ces requêtes :
POSTGRESQL est utilisé pour ce document. Neamoins: 
```bash
# Avec SQLite
sqlite3 rdv.db < requetes.sql

# Avec PostgreSQL
psql -d rdv -f requetes.sql

# Avec MySQL
mysql -u user -p database < requetes.sql
```

## Résultats des requêtes que j'ai faite

Sur la base de données fournie :

1. **Rendez-vous le week-end** : Aucun rendez-vous confirmé à venir n'est prévu un samedi ou dimanche dans l'export.
2. **Créneaux surbookés** : La requête identifie les créneaux avec plus de 3 rendez-vous (doit être exécutée sur la base complète).
3. **Changements de statut anormaux** : La requête identifie les rendez-vous passés qui ont été modifiés après annulation.
4. **Statistiques** : Répartition des statuts et services pour analyse.

## Architecture du module de règles de gestion que j'ai émise

Le module `ReglesGestionService` est indépendant de toute base de données et framework spécifique :

- Reçoit l'heure courante en paramètre pour être testable
- Utilise une map en mémoire pour stocker les rendez-vous (peut être remplacé par un repository)
- Applique toutes les règles de gestion spécifiées
- Renvoie les messages d'erreur prévus avec les codes HTTP appropriés
- Gère la pagination pour la liste des rendez-vous

## CONSTAT PEUT ETRE IMPORTANT : Anomalies non signalées

En examinant l'export et les traces, voici une anomalie supplémentaire détectée :

**Absence de validation du format de date/heure entrant** : Le système devrait valider que l'heure correspond à un créneau de 30 minutes (00 ou 30). Cette validation a été ajoutée dans `verifierCreneauValide()`.

## Historique Git que j'ai créé

Les commits ont été effectués régulièrement tout au long du développement :

- Création des modèles (ServiceType, Statut, RendezVous)
- Création des DTOs
- Création de l'exception métier
- Implémentation du service de règles de gestion
- Écriture des tests unitaires
- Création des requêtes SQL d'impact
- Rédaction du README

## Technologies utilisées

- Java 21
- Spring Boot 4.1.2


## Pierre Mauril Mafoya GUDE (pierremauril01@gmail.com)


