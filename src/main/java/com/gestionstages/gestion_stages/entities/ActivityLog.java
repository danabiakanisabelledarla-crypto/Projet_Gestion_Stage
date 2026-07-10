package com.gestionstages.gestion_stages.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "journal_activites")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 200)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "utilisateur_nom", length = 150)
    private String utilisateurNom;

    @Column(name = "date_activite", nullable = false)
    private LocalDateTime dateActivite = LocalDateTime.now();

    public ActivityLog() {}

    public ActivityLog(String action, String details, String utilisateurNom) {
        this.action = action;
        this.details = details;
        this.utilisateurNom = utilisateurNom;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getUtilisateurNom() { return utilisateurNom; }
    public void setUtilisateurNom(String utilisateurNom) { this.utilisateurNom = utilisateurNom; }
    public LocalDateTime getDateActivite() { return dateActivite; }
    public void setDateActivite(LocalDateTime dateActivite) { this.dateActivite = dateActivite; }
}
