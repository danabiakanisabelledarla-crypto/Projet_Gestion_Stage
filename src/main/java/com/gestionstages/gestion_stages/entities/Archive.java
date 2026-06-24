package com.gestionstages.gestion_stages.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "archives")
public class Archive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "stage_id", nullable = false, unique = true)
    private Stage stage;

    @Column(name = "date_archivage", nullable = false, updatable = false)
    private LocalDateTime dateArchivage = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String motif;

    // Constructeurs
    public Archive() {
    }

    public Archive(Stage stage, String motif) {
        this.stage = stage;
        this.motif = motif;
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

    public LocalDateTime getDateArchivage() {
        return dateArchivage;
    }

    public void setDateArchivage(LocalDateTime dateArchivage) {
        this.dateArchivage = dateArchivage;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }
}