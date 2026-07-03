# Modele relationnel et base de donnees MySQL

## Sujet

**Conception et developpement d'une plateforme numerique de gestion et de suivi des stages**

## Regles de transformation

Le diagramme de classes est transforme en modele relationnel selon les principes suivants :

- chaque classe devient une table ;
- chaque identifiant devient une cle primaire ;
- chaque relation entre deux classes devient une cle etrangere ;
- les relations de type `1,n` sont representees par une cle etrangere du cote `n` ;
- les documents et livrables sont stockes sous forme de chemins de fichiers.

## Liste des tables

### 1. roles

| Champ | Type | Contrainte |
|---|---|---|
| id | INT | PRIMARY KEY, AUTO_INCREMENT |
| libelle | VARCHAR(50) | NOT NULL, UNIQUE |
| description | TEXT | NULL |

### 2. utilisateurs

| Champ | Type | Contrainte |
|---|---|---|
| id | INT | PRIMARY KEY, AUTO_INCREMENT |
| role_id | INT | FOREIGN KEY |
| nom | VARCHAR(100) | NOT NULL |
| prenom | VARCHAR(100) | NOT NULL |
| email | VARCHAR(150) | NOT NULL, UNIQUE |
| mot_de_passe | VARCHAR(255) | NOT NULL |
| telephone | VARCHAR(30) | NULL |
| statut | ENUM | actif, inactif |
| date_creation | DATETIME | DEFAULT CURRENT_TIMESTAMP |

### 3. demandes_stage

| Champ | Type | Contrainte |
|---|---|---|
| id | INT | PRIMARY KEY, AUTO_INCREMENT |
| nom | VARCHAR(100) | NOT NULL |
| prenom | VARCHAR(100) | NOT NULL |
| ecole | VARCHAR(150) | NOT NULL |
| filiere | VARCHAR(150) | NOT NULL |
| niveau | VARCHAR(100) | NOT NULL |
| duree_souhaitee | VARCHAR(50) | NOT NULL |
| statut | ENUM | en_attente, acceptee, refusee |
| commentaire | TEXT | NULL |
| date_demande | DATETIME | DEFAULT CURRENT_TIMESTAMP |

### 4. documents

| Champ | Type | Contrainte |
|---|---|---|
| id | INT | PRIMARY KEY, AUTO_INCREMENT |
| demande_stage_id | INT | FOREIGN KEY, NULL |
| stage_id | INT | FOREIGN KEY, NULL |
| nom_fichier | VARCHAR(255) | NOT NULL |
| type_document | VARCHAR(100) | NOT NULL |
| chemin_fichier | VARCHAR(255) | NOT NULL |
| date_depot | DATETIME | DEFAULT CURRENT_TIMESTAMP |

### 5. encadreurs

| Champ | Type | Contrainte |
|---|---|---|
| id | INT | PRIMARY KEY, AUTO_INCREMENT |
| utilisateur_id | INT | FOREIGN KEY, UNIQUE |
| specialite | VARCHAR(150) | NULL |
| fonction | VARCHAR(150) | NULL |

### 6. stagiaires

| Champ | Type | Contrainte |
|---|---|---|
| id | INT | PRIMARY KEY, AUTO_INCREMENT |
| utilisateur_id | INT | FOREIGN KEY, UNIQUE |
| demande_stage_id | INT | FOREIGN KEY, UNIQUE |
| matricule | VARCHAR(50) | NOT NULL, UNIQUE |
| date_admission | DATE | NOT NULL |
| statut | ENUM | actif, termine, abandonne |

### 7. services

| Champ | Type | Contrainte |
|---|---|---|
| id | INT | PRIMARY KEY, AUTO_INCREMENT |
| nom | VARCHAR(150) | NOT NULL |
| description | TEXT | NULL |

### 8. projets

| Champ | Type | Contrainte |
|---|---|---|
| id | INT | PRIMARY KEY, AUTO_INCREMENT |
| titre | VARCHAR(200) | NOT NULL |
| description | TEXT | NULL |
| date_debut | DATE | NULL |
| date_fin | DATE | NULL |

### 9. stages

| Champ | Type | Contrainte |
|---|---|---|
| id | INT | PRIMARY KEY, AUTO_INCREMENT |
| stagiaire_id | INT | FOREIGN KEY, UNIQUE |
| encadreur_id | INT | FOREIGN KEY |
| service_id | INT | FOREIGN KEY |
| projet_id | INT | FOREIGN KEY, NULL |
| numero_stage | VARCHAR(50) | NOT NULL, UNIQUE |
| date_debut | DATE | NOT NULL |
| date_fin | DATE | NOT NULL |
| duree | VARCHAR(50) | NOT NULL |
| statut | ENUM | en_cours, termine, suspendu |

### 10. objectifs

| Champ | Type | Contrainte |
|---|---|---|
| id | INT | PRIMARY KEY, AUTO_INCREMENT |
| stage_id | INT | FOREIGN KEY |
| libelle | VARCHAR(200) | NOT NULL |
| description | TEXT | NULL |
| ordre | INT | NOT NULL |
| statut | ENUM | non_commence, en_cours, atteint |

### 11. taches

| Champ | Type | Contrainte |
|---|---|---|
| id | INT | PRIMARY KEY, AUTO_INCREMENT |
| stage_id | INT | FOREIGN KEY |
| titre | VARCHAR(200) | NOT NULL |
| description | TEXT | NULL |
| date_creation | DATETIME | DEFAULT CURRENT_TIMESTAMP |
| date_limite | DATE | NOT NULL |
| statut | ENUM | a_faire, en_cours, terminee, en_retard |

### 12. journaux_bord

| Champ | Type | Contrainte |
|---|---|---|
| id | INT | PRIMARY KEY, AUTO_INCREMENT |
| stage_id | INT | FOREIGN KEY |
| date_activite | DATE | NOT NULL |
| travaux_realises | TEXT | NOT NULL |
| difficultes | TEXT | NULL |
| observations | TEXT | NULL |

### 13. livrables

| Champ | Type | Contrainte |
|---|---|---|
| id | INT | PRIMARY KEY, AUTO_INCREMENT |
| tache_id | INT | FOREIGN KEY |
| titre | VARCHAR(200) | NOT NULL |
| description | TEXT | NULL |
| fichier | VARCHAR(255) | NOT NULL |
| date_depot | DATETIME | DEFAULT CURRENT_TIMESTAMP |
| statut | ENUM | depose, valide, rejete, correction_demandee |
| commentaire_encadreur | TEXT | NULL |

### 14. evaluations

| Champ | Type | Contrainte |
|---|---|---|
| id | INT | PRIMARY KEY, AUTO_INCREMENT |
| stage_id | INT | FOREIGN KEY |
| type_evaluation | ENUM | continue, finale |
| date_evaluation | DATE | NOT NULL |
| appreciation | TEXT | NULL |

### 15. criteres_evaluation

| Champ | Type | Contrainte |
|---|---|---|
| id | INT | PRIMARY KEY, AUTO_INCREMENT |
| libelle | VARCHAR(150) | NOT NULL |
| description | TEXT | NULL |

### 16. notes_evaluation

| Champ | Type | Contrainte |
|---|---|---|
| id | INT | PRIMARY KEY, AUTO_INCREMENT |
| evaluation_id | INT | FOREIGN KEY |
| critere_id | INT | FOREIGN KEY |
| note | DECIMAL(4,2) | NOT NULL |
| commentaire | TEXT | NULL |

### 17. archives

| Champ | Type | Contrainte |
|---|---|---|
| id | INT | PRIMARY KEY, AUTO_INCREMENT |
| stage_id | INT | FOREIGN KEY, UNIQUE |
| date_archivage | DATETIME | DEFAULT CURRENT_TIMESTAMP |
| motif | TEXT | NULL |

## Relations principales

- `roles.id` vers `utilisateurs.role_id`
- `utilisateurs.id` vers `encadreurs.utilisateur_id`
- `utilisateurs.id` vers `stagiaires.utilisateur_id`
- `demandes_stage.id` vers `stagiaires.demande_stage_id`
- `demandes_stage.id` vers `documents.demande_stage_id`
- `stagiaires.id` vers `stages.stagiaire_id`
- `encadreurs.id` vers `stages.encadreur_id`
- `services.id` vers `stages.service_id`
- `projets.id` vers `stages.projet_id`
- `stages.id` vers `objectifs.stage_id`
- `stages.id` vers `taches.stage_id`
- `stages.id` vers `journaux_bord.stage_id`
- `stages.id` vers `documents.stage_id`
- `taches.id` vers `livrables.tache_id`
- `stages.id` vers `evaluations.stage_id`
- `evaluations.id` vers `notes_evaluation.evaluation_id`
- `criteres_evaluation.id` vers `notes_evaluation.critere_id`
- `stages.id` vers `archives.stage_id`

## Remarque

La table `documents` est volontairement generale. Elle peut contenir les CV, lettres de motivation, rapports, attestations et autres fichiers lies soit a une demande de stage, soit a un stage deja cree.
