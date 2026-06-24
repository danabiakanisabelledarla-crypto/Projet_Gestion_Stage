package com.gestionstages.gestion_stages.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "services")
public class ServiceEntreprise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Constructeurs
    public ServiceEntreprise() {
    }

    public ServiceEntreprise(String nom, String description) {
        this.nom = nom;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}