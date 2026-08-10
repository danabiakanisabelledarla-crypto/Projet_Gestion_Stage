package com.gestionstages.gestion_stages.services;

import com.gestionstages.gestion_stages.entities.Utilisateur;
import com.gestionstages.gestion_stages.repositories.DocumentRepository;
import com.gestionstages.gestion_stages.repositories.UtilisateurRepository;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class SecurityScanService {

    public record Finding(String level, String title, String details) {
    }

    public record Report(int score, Instant scannedAt, List<Finding> findings,
                         long checkedUsers, long checkedDocuments, long activeSessions) {
    }

    private final UtilisateurRepository utilisateurRepository;
    private final DocumentRepository documentRepository;
    private final SessionRegistry sessionRegistry;
    private final BackupService backupService;
    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;

    public SecurityScanService(UtilisateurRepository utilisateurRepository,
                               DocumentRepository documentRepository,
                               SessionRegistry sessionRegistry,
                               BackupService backupService,
                               JdbcTemplate jdbcTemplate,
                               Environment environment) {
        this.utilisateurRepository = utilisateurRepository;
        this.documentRepository = documentRepository;
        this.sessionRegistry = sessionRegistry;
        this.backupService = backupService;
        this.jdbcTemplate = jdbcTemplate;
        this.environment = environment;
    }

    public Report scan() {
        List<Finding> findings = new ArrayList<>();
        List<Utilisateur> users = utilisateurRepository.findAll();
        long documents = documentRepository.count();
        int score = 100;

        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (!Files.isWritable(Paths.get("data").toAbsolutePath().normalize())) {
                findings.add(new Finding("critical", "Stockage non inscriptible",
                        "Le dossier de la base ne permet pas les écritures."));
                score -= 35;
            }
        } catch (Exception exception) {
            findings.add(new Finding("critical", "Base de données indisponible",
                    exception.getMessage()));
            score -= 40;
        }

        long missingFiles = documentRepository.findAll().stream()
                .filter(document -> document.getCheminFichier() == null
                        || !Files.exists(Paths.get(document.getCheminFichier())))
                .count();
        if (missingFiles > 0) {
            findings.add(new Finding("high", "Fichiers manquants",
                    missingFiles + " document(s) référencent un fichier absent."));
            score -= Math.min(20, (int) missingFiles * 2);
        }

        long locked = users.stream()
                .filter(user -> user.getStatut() == Utilisateur.StatutUtilisateur.inactif)
                .count();
        if (locked > 0) {
            findings.add(new Finding("medium", "Comptes inactifs",
                    locked + " compte(s) sont actuellement bloqués."));
            score -= Math.min(10, (int) locked);
        }

        long protectedUsers = users.stream().filter(Utilisateur::isTwoFactorEnabled).count();
        if (protectedUsers < users.size()) {
            findings.add(new Finding("medium", "Couverture 2FA incomplète",
                    protectedUsers + " compte(s) sur " + users.size() + " utilisent la 2FA."));
            score -= 10;
        }

        String mailUser = environment.getProperty("spring.mail.username", "");
        if (mailUser.isBlank()) {
            findings.add(new Finding("low", "Messagerie non configurée",
                    "Les alertes de sécurité par email ne peuvent pas être envoyées."));
            score -= 5;
        }

        try {
            List<Path> backups = backupService.list();
            if (backups.isEmpty()) {
                findings.add(new Finding("high", "Aucune sauvegarde",
                        "Créez une sauvegarde avant toute opération sensible."));
                score -= 15;
            } else {
                Instant lastBackup = Files.getLastModifiedTime(backups.get(0)).toInstant();
                if (Duration.between(lastBackup, Instant.now()).toDays() > 7) {
                    findings.add(new Finding("medium", "Sauvegarde ancienne",
                            "La dernière sauvegarde date de plus de sept jours."));
                    score -= 8;
                }
            }
        } catch (Exception exception) {
            findings.add(new Finding("medium", "Sauvegardes non vérifiables",
                    exception.getMessage()));
            score -= 8;
        }

        long sessions = sessionRegistry.getAllPrincipals().stream()
                .mapToLong(principal -> sessionRegistry.getAllSessions(principal, false).size())
                .sum();
        if (findings.isEmpty()) {
            findings.add(new Finding("ok", "Aucune anomalie détectée",
                    "Les contrôles de base, fichiers, sauvegardes, comptes et sessions sont conformes."));
        }
        return new Report(Math.max(0, score), Instant.now(), List.copyOf(findings),
                users.size(), documents, sessions);
    }
}
