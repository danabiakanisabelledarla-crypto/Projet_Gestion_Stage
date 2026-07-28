package com.gestionstages.gestion_stages.entities;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "evenements_personnels")
public class EvenementPersonnel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    @Column(nullable = false, length = 100)
    private String motif;

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 50)
    private String typeCouleur = "personnel";

    private LocalTime heure;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String lieu;

    @Column(length = 30)
    private String priorite = "moyenne";

    @Column(length = 30)
    private String rappel = "1_heure";

    public EvenementPersonnel() {}

    public EvenementPersonnel(Stage stage, String motif, LocalDate date, String typeCouleur) {
        this.stage = stage;
        this.motif = motif;
        this.date = date;
        this.typeCouleur = typeCouleur;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Stage getStage() { return stage; }
    public void setStage(Stage stage) { this.stage = stage; }
    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getTypeCouleur() { return typeCouleur; }
    public void setTypeCouleur(String typeCouleur) { this.typeCouleur = typeCouleur; }
    public LocalTime getHeure() { return heure; }
    public void setHeure(LocalTime heure) { this.heure = heure; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }
    public String getPriorite() { return priorite; }
    public void setPriorite(String priorite) { this.priorite = priorite; }
    public String getRappel() { return rappel; }
    public void setRappel(String rappel) { this.rappel = rappel; }
}
