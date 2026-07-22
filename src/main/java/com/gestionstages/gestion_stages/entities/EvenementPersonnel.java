package com.gestionstages.gestion_stages.entities;

import jakarta.persistence.*;
import java.time.LocalDate;

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
}
