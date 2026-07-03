CREATE DATABASE IF NOT EXISTS gestion_stages
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE gestion_stages;

CREATE TABLE roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    libelle VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE utilisateurs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    role_id INT NOT NULL,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    mot_de_passe VARCHAR(255) NOT NULL,
    telephone VARCHAR(30),
    statut ENUM('actif', 'inactif') NOT NULL DEFAULT 'actif',
    date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_utilisateurs_roles
        FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE demandes_stage (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    ecole VARCHAR(150) NOT NULL,
    filiere VARCHAR(150) NOT NULL,
    niveau VARCHAR(100) NOT NULL,
    duree_souhaitee VARCHAR(50) NOT NULL,
    statut ENUM('en_attente', 'acceptee', 'refusee') NOT NULL DEFAULT 'en_attente',
    commentaire TEXT,
    date_demande DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE encadreurs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id INT NOT NULL UNIQUE,
    specialite VARCHAR(150),
    fonction VARCHAR(150),
    CONSTRAINT fk_encadreurs_utilisateurs
        FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id)
);

CREATE TABLE stagiaires (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id INT NOT NULL UNIQUE,
    demande_stage_id INT NOT NULL UNIQUE,
    matricule VARCHAR(50) NOT NULL UNIQUE,
    date_admission DATE NOT NULL,
    statut ENUM('actif', 'termine', 'abandonne') NOT NULL DEFAULT 'actif',
    CONSTRAINT fk_stagiaires_utilisateurs
        FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id),
    CONSTRAINT fk_stagiaires_demandes
        FOREIGN KEY (demande_stage_id) REFERENCES demandes_stage(id)
);

CREATE TABLE services (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(150) NOT NULL,
    description TEXT
);

CREATE TABLE projets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(200) NOT NULL,
    description TEXT,
    date_debut DATE,
    date_fin DATE
);

CREATE TABLE stages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    stagiaire_id INT NOT NULL UNIQUE,
    encadreur_id INT NOT NULL,
    service_id INT NOT NULL,
    projet_id INT,
    numero_stage VARCHAR(50) NOT NULL UNIQUE,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    duree VARCHAR(50) NOT NULL,
    statut ENUM('en_cours', 'termine', 'suspendu') NOT NULL DEFAULT 'en_cours',
    CONSTRAINT fk_stages_stagiaires
        FOREIGN KEY (stagiaire_id) REFERENCES stagiaires(id),
    CONSTRAINT fk_stages_encadreurs
        FOREIGN KEY (encadreur_id) REFERENCES encadreurs(id),
    CONSTRAINT fk_stages_services
        FOREIGN KEY (service_id) REFERENCES services(id),
    CONSTRAINT fk_stages_projets
        FOREIGN KEY (projet_id) REFERENCES projets(id)
);

CREATE TABLE documents (
    id INT AUTO_INCREMENT PRIMARY KEY,
    demande_stage_id INT,
    stage_id INT,
    nom_fichier VARCHAR(255) NOT NULL,
    type_document VARCHAR(100) NOT NULL,
    chemin_fichier VARCHAR(255) NOT NULL,
    date_depot DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_documents_demandes
        FOREIGN KEY (demande_stage_id) REFERENCES demandes_stage(id),
    CONSTRAINT fk_documents_stages
        FOREIGN KEY (stage_id) REFERENCES stages(id)
);

CREATE TABLE objectifs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    stage_id INT NOT NULL,
    libelle VARCHAR(200) NOT NULL,
    description TEXT,
    ordre INT NOT NULL,
    statut ENUM('non_commence', 'en_cours', 'atteint') NOT NULL DEFAULT 'non_commence',
    CONSTRAINT fk_objectifs_stages
        FOREIGN KEY (stage_id) REFERENCES stages(id)
);

CREATE TABLE taches (
    id INT AUTO_INCREMENT PRIMARY KEY,
    stage_id INT NOT NULL,
    titre VARCHAR(200) NOT NULL,
    description TEXT,
    date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_limite DATE NOT NULL,
    statut ENUM('a_faire', 'en_cours', 'terminee', 'en_retard') NOT NULL DEFAULT 'a_faire',
    CONSTRAINT fk_taches_stages
        FOREIGN KEY (stage_id) REFERENCES stages(id)
);

CREATE TABLE journaux_bord (
    id INT AUTO_INCREMENT PRIMARY KEY,
    stage_id INT NOT NULL,
    date_activite DATE NOT NULL,
    travaux_realises TEXT NOT NULL,
    difficultes TEXT,
    observations TEXT,
    CONSTRAINT fk_journaux_stages
        FOREIGN KEY (stage_id) REFERENCES stages(id),
    CONSTRAINT uq_journal_stage_date UNIQUE (stage_id, date_activite)
);

CREATE TABLE livrables (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tache_id INT NOT NULL,
    titre VARCHAR(200) NOT NULL,
    description TEXT,
    fichier VARCHAR(255) NOT NULL,
    date_depot DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut ENUM('depose', 'valide', 'rejete', 'correction_demandee') NOT NULL DEFAULT 'depose',
    commentaire_encadreur TEXT,
    CONSTRAINT fk_livrables_taches
        FOREIGN KEY (tache_id) REFERENCES taches(id)
);

CREATE TABLE evaluations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    stage_id INT NOT NULL,
    type_evaluation ENUM('continue', 'finale') NOT NULL,
    date_evaluation DATE NOT NULL,
    appreciation TEXT,
    CONSTRAINT fk_evaluations_stages
        FOREIGN KEY (stage_id) REFERENCES stages(id)
);

CREATE TABLE criteres_evaluation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    libelle VARCHAR(150) NOT NULL,
    description TEXT
);

CREATE TABLE notes_evaluation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    evaluation_id INT NOT NULL,
    critere_id INT NOT NULL,
    note DECIMAL(4,2) NOT NULL,
    commentaire TEXT,
    CONSTRAINT fk_notes_evaluations
        FOREIGN KEY (evaluation_id) REFERENCES evaluations(id),
    CONSTRAINT fk_notes_criteres
        FOREIGN KEY (critere_id) REFERENCES criteres_evaluation(id),
    CONSTRAINT ck_note_intervalle CHECK (note >= 0 AND note <= 20),
    CONSTRAINT uq_note_evaluation_critere UNIQUE (evaluation_id, critere_id)
);

CREATE TABLE archives (
    id INT AUTO_INCREMENT PRIMARY KEY,
    stage_id INT NOT NULL UNIQUE,
    date_archivage DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    motif TEXT,
    CONSTRAINT fk_archives_stages
        FOREIGN KEY (stage_id) REFERENCES stages(id)
);

INSERT INTO roles (libelle, description) VALUES
('ADMINISTRATEUR', 'Gestion globale de la plateforme'),
('RESPONSABLE_STAGE', 'Traitement des demandes et affectation des stagiaires'),
('ENCADREUR', 'Suivi, validation et evaluation des stagiaires'),
('STAGIAIRE', 'Consultation des taches, journal de bord et depot des livrables');

INSERT INTO criteres_evaluation (libelle, description) VALUES
('Assiduite', 'Presence, ponctualite et regularite'),
('Qualite', 'Qualite du travail fourni'),
('Respect des delais', 'Capacite a respecter les echeances'),
('Technique', 'Maitrise technique'),
('Autonomie', 'Capacite a travailler avec peu d assistance'),
('Communication', 'Capacite a communiquer clairement');
