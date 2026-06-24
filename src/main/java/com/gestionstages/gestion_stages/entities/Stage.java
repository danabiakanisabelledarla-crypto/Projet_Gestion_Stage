package com.gestionstages.gestion_stages.entities;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "stages")
public class Stage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "stagiaire_id", nullable = false, unique = true)
    private Stagiaire stagiaire;

    @ManyToOne
    @JoinColumn(name = "encadreur_id", nullable = false)
    private Encadreur encadreur;

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntreprise service;

    @ManyToOne
    @JoinColumn(name = "projet_id", nullable = true)
    private Projet projet;

    @Column(name = "numero_stage", nullable = false, unique = true, length = 50)
    private String numeroStage;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(nullable = false, length = 50)
    private String duree;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutStage statut = StatutStage.en_cours;

    public enum StatutStage {
        en_cours, termine, suspendu
    }

    // Constructeurs
    public Stage() {
    }

    public Stage(Stagiaire stagiaire, Encadreur encadreur, ServiceEntreprise service, String numeroStage,
                 LocalDate dateDebut, LocalDate dateFin, String duree) {
        this.stagiaire = stagiaire;
        this.encadreur = encadreur;
        this.service = service;
        this.numeroStage = numeroStage;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.duree = duree;
    }

    // Getters et Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Stagiaire getStagiaire() {
        return stagiaire;
    }

    public void setStagiaire(Stagiaire stagiaire) {
        this.stagiaire = stagiaire;
    }

    public Encadreur getEncadreur() {
        return encadreur;
    }

    public void setEncadreur(Encadreur encadreur) {
        this.encadreur = encadreur;
    }

    public ServiceEntreprise getService() {
        return service;
    }

    public void setService(ServiceEntreprise service) {
        this.service = service;
    }

    public Projet getProjet() {
        return projet;
    }

    public void setProjet(Projet projet) {
        this.projet = projet;
    }

    public String getNumeroStage() {
        return numeroStage;
    }

    public void setNumeroStage(String numeroStage) {
        this.numeroStage = numeroStage;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public String getDuree() {
        return duree;
    }

    public void setDuree(String duree) {
        this.duree = duree;
    }

    public StatutStage getStatut() {
        return statut;
    }

    public void setStatut(StatutStage statut) {
        this.statut = statut;
    }
}