package com.gestionstages.gestion_stages.entities;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "stagiaires")
public class Stagiaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "utilisateur_id", nullable = false, unique = true)
    private Utilisateur utilisateur;

    @OneToOne
    @JoinColumn(name = "demande_stage_id", nullable = false, unique = true)
    private DemandeStage demandeStage;

    @Column(nullable = false, unique = true, length = 50)
    private String matricule;

    @Column(name = "date_admission", nullable = false)
    private LocalDate dateAdmission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutStagiaire statut = StatutStagiaire.actif;

    @Column(name = "progression")
    private Integer progression = 0;

    @Column(length = 150)
    private String specialite;

    @Column(name = "notifications_email")
    private Boolean notificationsEmail = true;

    @Column(name = "notifications_systeme")
    private Boolean notificationsSysteme = true;

    @Column(name = "rappel_taches")
    private Boolean rappelTaches = true;

    @Column(name = "mode_sombre")
    private Boolean modeSombre = false;

    @Column(length = 5)
    private String langue = "fr";

    public enum StatutStagiaire {
        actif, termine, abandonne
    }

    public Stagiaire() {
    }

    public Stagiaire(Utilisateur utilisateur, DemandeStage demandeStage, String matricule, LocalDate dateAdmission) {
        this.utilisateur = utilisateur;
        this.demandeStage = demandeStage;
        this.matricule = matricule;
        this.dateAdmission = dateAdmission;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public DemandeStage getDemandeStage() {
        return demandeStage;
    }

    public void setDemandeStage(DemandeStage demandeStage) {
        this.demandeStage = demandeStage;
    }

    public String getMatricule() {
        return matricule;
    }

    public void setMatricule(String matricule) {
        this.matricule = matricule;
    }

    public LocalDate getDateAdmission() {
        return dateAdmission;
    }

    public void setDateAdmission(LocalDate dateAdmission) {
        this.dateAdmission = dateAdmission;
    }

    public StatutStagiaire getStatut() {
        return statut;
    }

    public void setStatut(StatutStagiaire statut) {
        this.statut = statut;
    }

    public Integer getProgression() {
        return progression;
    }

    public void setProgression(Integer progression) {
        this.progression = progression;
    }

    public String getSpecialite() {
        return specialite;
    }

    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }

    public Boolean getNotificationsEmail() {
        return notificationsEmail == null ? Boolean.TRUE : notificationsEmail;
    }

    public void setNotificationsEmail(Boolean notificationsEmail) {
        this.notificationsEmail = notificationsEmail;
    }

    public Boolean getNotificationsSysteme() {
        return notificationsSysteme == null ? Boolean.TRUE : notificationsSysteme;
    }

    public void setNotificationsSysteme(Boolean notificationsSysteme) {
        this.notificationsSysteme = notificationsSysteme;
    }

    public Boolean getRappelTaches() {
        return rappelTaches == null ? Boolean.TRUE : rappelTaches;
    }

    public void setRappelTaches(Boolean rappelTaches) {
        this.rappelTaches = rappelTaches;
    }

    public Boolean getModeSombre() {
        return modeSombre == null ? Boolean.FALSE : modeSombre;
    }

    public void setModeSombre(Boolean modeSombre) {
        this.modeSombre = modeSombre;
    }

    public String getLangue() {
        return langue == null || langue.isBlank() ? "fr" : langue;
    }

    public void setLangue(String langue) {
        this.langue = "en".equalsIgnoreCase(langue) ? "en" : "fr";
    }
}
