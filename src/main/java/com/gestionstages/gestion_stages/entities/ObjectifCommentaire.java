package com.gestionstages.gestion_stages.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "objectif_commentaires")
public class ObjectifCommentaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "objectif_id", nullable = false)
    private Objectif objectif;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encadreur_id", nullable = false)
    private Encadreur encadreur;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "date_commentaire", nullable = false)
    private LocalDateTime dateCommentaire = LocalDateTime.now();

    public ObjectifCommentaire() {
    }

    public ObjectifCommentaire(Objectif objectif, Encadreur encadreur, String message) {
        this.objectif = objectif;
        this.encadreur = encadreur;
        this.message = message;
    }

    public Integer getId() {
        return id;
    }

    public Objectif getObjectif() {
        return objectif;
    }

    public Encadreur getEncadreur() {
        return encadreur;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getDateCommentaire() {
        return dateCommentaire;
    }
}
