package com.gestionstages.gestion_stages.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "demandes_stage")
public class DemandeStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(nullable = false, length = 150)
    private String ecole;

    @Column(nullable = false, length = 150)
    private String filiere;

    @Column(nullable = false, length = 100)
    private String niveau;

    @Column(name = "duree_souhaitee", nullable = false, length = 50)
    private String dureeSouhaitee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDemande statut = StatutDemande.en_attente;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "date_demande", nullable = false, updatable = false)
    private LocalDateTime dateDemande = LocalDateTime.now();

    public enum StatutDemande {
        en_attente, acceptee, refusee
    }

    // Constructeurs
    public DemandeStage() {
    }

    public DemandeStage(String nom, String prenom, String ecole, String filiere, String niveau, String dureeSouhaitee) {
        this.nom = nom;
        this.prenom = prenom;
        this.ecole = ecole;
        this.filiere = filiere;
        this.niveau = niveau;
        this.dureeSouhaitee = dureeSouhaitee;
    }

    // Getters et Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEcole() {
        return ecole;
    }

    public void setEcole(String ecole) {
        this.ecole = ecole;
    }

    public String getFiliere() {
        return filiere;
    }

    public void setFiliere(String filiere) {
        this.filiere = filiere;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public String getDureeSouhaitee() {
        return dureeSouhaitee;
    }

    public void setDureeSouhaitee(String dureeSouhaitee) {
        this.dureeSouhaitee = dureeSouhaitee;
    }

    public StatutDemande getStatut() {
        return statut;
    }

    public void setStatut(StatutDemande statut) {
        this.statut = statut;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public LocalDateTime getDateDemande() {
        return dateDemande;
    }

    public void setDateDemande(LocalDateTime dateDemande) {
        this.dateDemande = dateDemande;
    }
}