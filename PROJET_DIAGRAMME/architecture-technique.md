# Architecture technique du projet

## Sujet

**Conception et developpement d'une plateforme numerique de gestion et de suivi des stages**

## Objectif

L'architecture technique decrit les technologies utilisees, l'organisation du projet et la maniere dont les differentes parties de l'application communiquent entre elles.

## Choix technologiques

### Backend

Le backend sera developpe avec **Java Spring Boot**.

Spring Boot permet de creer une application web robuste, organisee et facile a connecter a une base de donnees MySQL.

### Frontend

Le frontend sera developpe avec :

- HTML ;
- CSS ;
- Bootstrap ;
- Thymeleaf.

Thymeleaf permet de generer des pages HTML directement depuis Spring Boot.

### Base de donnees

La base de donnees utilisee sera **MySQL**.

Elle stockera les utilisateurs, les roles, les demandes de stage, les stagiaires, les taches, les journaux de bord, les livrables, les evaluations et les archives.

### Securite

La securite sera geree avec **Spring Security**.

Les roles prevus sont :

- ADMINISTRATEUR ;
- RESPONSABLE_STAGE ;
- ENCADREUR ;
- STAGIAIRE.

Chaque utilisateur accede uniquement aux pages correspondant a son role.

## Architecture generale

L'application suit une architecture en couches :

1. **Couche presentation**
   - pages HTML ;
   - formulaires ;
   - tableaux de bord ;
   - affichage des donnees.

2. **Couche controleur**
   - reception des requetes ;
   - orientation vers les bonnes pages ;
   - appel des services.

3. **Couche service**
   - logique metier ;
   - traitement des demandes ;
   - admission des stagiaires ;
   - affectation ;
   - validation des livrables ;
   - evaluation.

4. **Couche repository**
   - communication avec la base de donnees ;
   - operations d'ajout, modification, suppression et recherche.

5. **Couche base de donnees**
   - stockage permanent dans MySQL.

## Organisation du projet Spring Boot

```text
gestion-stages/
|-- src/
|   |-- main/
|   |   |-- java/
|   |   |   |-- com/
|   |   |   |   |-- gestionstages/
|   |   |   |   |   |-- GestionStagesApplication.java
|   |   |   |   |   |-- entities/
|   |   |   |   |   |-- repositories/
|   |   |   |   |   |-- services/
|   |   |   |   |   |-- controllers/
|   |   |   |   |   |-- security/
|   |   |-- resources/
|   |   |   |-- templates/
|   |   |   |   |-- auth/
|   |   |   |   |-- admin/
|   |   |   |   |-- responsable/
|   |   |   |   |-- encadreur/
|   |   |   |   |-- stagiaire/
|   |   |   |-- static/
|   |   |   |   |-- css/
|   |   |   |   |-- js/
|   |   |   |   |-- images/
|   |   |   |-- application.properties
|-- pom.xml
```

## Packages principaux

### entities

Contient les classes Java qui representent les tables de la base de donnees.

Exemples :

- Utilisateur ;
- Role ;
- DemandeStage ;
- Stagiaire ;
- Stage ;
- Tache ;
- Livrable ;
- Evaluation.

### repositories

Contient les interfaces permettant de communiquer avec MySQL.

Exemples :

- UtilisateurRepository ;
- DemandeStageRepository ;
- StageRepository ;
- TacheRepository ;
- EvaluationRepository.

### services

Contient la logique metier de l'application.

Exemples :

- traiter une demande de stage ;
- creer un compte stagiaire ;
- affecter un encadreur ;
- valider un livrable ;
- calculer les statistiques.

### controllers

Contient les classes qui gerent les pages web.

Exemples :

- AuthController ;
- AdminController ;
- DemandeStageController ;
- StagiaireController ;
- EncadreurController ;
- EvaluationController.

### security

Contient la configuration de la connexion, des roles et des autorisations.

## Modules a developper

### Module 1 : Authentification

- connexion ;
- deconnexion ;
- gestion des roles ;
- restriction d'acces selon le profil.

### Module 2 : Gestion des candidatures

- depot d'une demande ;
- consultation des demandes ;
- acceptation ;
- refus ;
- mise en attente.

### Module 3 : Gestion des stagiaires

- creation du compte stagiaire ;
- generation du matricule ;
- fiche stagiaire ;
- affectation a un encadreur, un service et un projet.

### Module 4 : Gestion des taches

- creation des taches ;
- modification ;
- suivi de l'avancement ;
- consultation par le stagiaire.

### Module 5 : Journal de bord

- saisie quotidienne des activites ;
- consultation par l'encadreur ;
- suivi des difficultes.

### Module 6 : Gestion documentaire

- depot du CV ;
- depot de la lettre de motivation ;
- depot des livrables ;
- depot du rapport final ;
- conservation des fichiers.

### Module 7 : Evaluation

- evaluation continue ;
- evaluation finale ;
- notes par critere ;
- appreciation de l'encadreur.

### Module 8 : Statistiques

- nombre de stagiaires ;
- stages en cours ;
- stages termines ;
- demandes acceptees ou refusees ;
- taches realisees.

### Module 9 : Archivage

- cloture du stage ;
- archivage des documents ;
- conservation de l'historique.

## Flux technique principal

1. L'utilisateur ouvre la plateforme.
2. Il se connecte avec son email et son mot de passe.
3. Spring Security verifie son identite et son role.
4. Le controleur redirige l'utilisateur vers son espace.
5. Les services executent les traitements demandes.
6. Les repositories recuperent ou enregistrent les donnees dans MySQL.
7. Les pages Thymeleaf affichent les resultats.

## Conclusion

Cette architecture permet de separer clairement l'affichage, la logique metier et l'acces aux donnees. Elle facilite le developpement progressif de la plateforme et rend le projet plus simple a maintenir.
