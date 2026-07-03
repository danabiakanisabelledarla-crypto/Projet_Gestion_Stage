package com.gestionstages.gestion_stages.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "livrables")
public class Livrable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "tache_id", nullable = false)
    private Tache tache;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 255)
    private String fichier;

    @Column(name = "date_depot", nullable = false, updatable = false)
    private LocalDateTime dateDepot = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutLivrable statut = StatutLivrable.depose;

    @Column(name = "commentaire_encadreur", columnDefinition = "TEXT")
    private String commentaireEncadreur;

    public enum StatutLivrable {
        depose, valide, rejete, correction_demandee
    }

    // Constructeurs
    public Livrable() {
    }

    public Livrable(Tache tache, String titre, String description, String fichier) {
        this.tache = tache;
        this.titre = titre;
        this.description = description;
        this.fichier = fichier;
    }

    // Getters et Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Tache getTache() {
        return tache;
    }

    public void setTache(Tache tache) {
        this.tache = tache;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFichier() {
        return fichier;
    }

    public void setFichier(String fichier) {
        this.fichier = fichier;
    }

    public LocalDateTime getDateDepot() {
        return dateDepot;
    }

    public void setDateDepot(LocalDateTime dateDepot) {
        this.dateDepot = dateDepot;
    }

    public StatutLivrable getStatut() {
        return statut;
    }

    public void setStatut(StatutLivrable statut) {
        this.statut = statut;
    }

    public String getCommentaireEncadreur() {
        return commentaireEncadreur;
    }

    public void setCommentaireEncadreur(String commentaireEncadreur) {
        this.commentaireEncadreur = commentaireEncadreur;
    }

    public void setNomFichier(String nomFichier) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setNomFichier'");
    }
}