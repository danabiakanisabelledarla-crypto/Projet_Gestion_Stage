package com.gestionstages.gestion_stages.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, unique = true, length = 120)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    public Permission() {
    }

    public Permission(String code, String nom, String description) {
        this.code = code;
        this.nom = nom;
        this.description = description;
    }

    public Integer getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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
