package com.gestionstages.gestion_stages.entities;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "journaux_bord")
public class JournalBord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    @Column(name = "date_activite", nullable = false)
    private LocalDate dateActivite;

    @Column(name = "travaux_realises", columnDefinition = "TEXT", nullable = false)
    private String travauxRealises;

    @Column(columnDefinition = "TEXT")
    private String difficultes;

    @Column(columnDefinition = "TEXT")
    private String observations;

    // Constructeurs
    public JournalBord() {
    }

    public JournalBord(Stage stage, LocalDate dateActivite, String travauxRealises) {
        this.stage = stage;
        this.dateActivite = dateActivite;
        this.travauxRealises = travauxRealises;
    }

    // Getters et Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public LocalDate getDateActivite() {
        return dateActivite;
    }

    public void setDateActivite(LocalDate dateActivite) {
        this.dateActivite = dateActivite;
    }

    public String getTravauxRealises() {
        return travauxRealises;
    }

    public void setTravauxRealises(String travauxRealises) {
        this.travauxRealises = travauxRealises;
    }

    public String getDifficultes() {
        return difficultes;
    }

    public void setDifficultes(String difficultes) {
        this.difficultes = difficultes;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }
}