package com.gestionstages.gestion_stages.entities;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "projets")
public class Projet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "type_projet", length = 100)
    private String typeProjet;

    @Column(length = 500)
    private String technologies;

    @Column(name = "lien_github", length = 500)
    private String lienGithub;

    @Column(name = "lien_demo", length = 500)
    private String lienDemo;

    // Constructeurs
    public Projet() {
    }

    public Projet(String titre, String description, LocalDate dateDebut, LocalDate dateFin) {
        this.titre = titre;
        this.description = description;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
    }

    // Getters et Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public String getTypeProjet() { return typeProjet; }
    public void setTypeProjet(String typeProjet) { this.typeProjet = typeProjet; }
    public String getTechnologies() { return technologies; }
    public void setTechnologies(String technologies) { this.technologies = technologies; }
    public String getLienGithub() { return lienGithub; }
    public void setLienGithub(String lienGithub) { this.lienGithub = lienGithub; }
    public String getLienDemo() { return lienDemo; }
    public void setLienDemo(String lienDemo) { this.lienDemo = lienDemo; }
}
