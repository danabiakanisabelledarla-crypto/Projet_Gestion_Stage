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

    public enum StatutStagiaire {
        actif, termine, abandonne
    }

    // Constructeurs
    public Stagiaire() {
    }

    public Stagiaire(Utilisateur utilisateur, DemandeStage demandeStage, String matricule, LocalDate dateAdmission) {
        this.utilisateur = utilisateur;
        this.demandeStage = demandeStage;
        this.matricule = matricule;
        this.dateAdmission = dateAdmission;
    }

    // Getters et Setters
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
}