package com.gestionstages.gestion_stages.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "demande_stage_id", nullable = true)
    private DemandeStage demandeStage;

    @ManyToOne
    @JoinColumn(name = "stage_id", nullable = true)
    private Stage stage;

    @Column(name = "nom_fichier", nullable = false, length = 255)
    private String nomFichier;

    @Column(name = "type_document", nullable = false, length = 100)
    private String typeDocument;

    @Column(name = "chemin_fichier", nullable = false, length = 255)
    private String cheminFichier;

    @Column(name = "date_depot", nullable = false, updatable = false)
    private LocalDateTime dateDepot = LocalDateTime.now();

    // Constructeurs
    public Document() {
    }

    public Document(String nomFichier, String typeDocument, String cheminFichier) {
        this.nomFichier = nomFichier;
        this.typeDocument = typeDocument;
        this.cheminFichier = cheminFichier;
    }

    // Getters et Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public DemandeStage getDemandeStage() {
        return demandeStage;
    }

    public void setDemandeStage(DemandeStage demandeStage) {
        this.demandeStage = demandeStage;
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public String getNomFichier() {
        return nomFichier;
    }

    public void setNomFichier(String nomFichier) {
        this.nomFichier = nomFichier;
    }

    public String getTypeDocument() {
        return typeDocument;
    }

    public void setTypeDocument(String typeDocument) {
        this.typeDocument = typeDocument;
    }

    public String getCheminFichier() {
        return cheminFichier;
    }

    public void setCheminFichier(String cheminFichier) {
        this.cheminFichier = cheminFichier;
    }

    public LocalDateTime getDateDepot() {
        return dateDepot;
    }

    public void setDateDepot(LocalDateTime dateDepot) {
        this.dateDepot = dateDepot;
    }
}