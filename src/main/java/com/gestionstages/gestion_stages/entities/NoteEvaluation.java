package com.gestionstages.gestion_stages.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "notes_evaluation")
public class NoteEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;

    @ManyToOne
    @JoinColumn(name = "critere_id", nullable = false)
    private CritereEvaluation critere;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal note;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    // Constructeurs
    public NoteEvaluation() {
    }

    public NoteEvaluation(Evaluation evaluation, CritereEvaluation critere, BigDecimal note) {
        this.evaluation = evaluation;
        this.critere = critere;
        this.note = note;
    }

    // Getters et Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Evaluation getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(Evaluation evaluation) {
        this.evaluation = evaluation;
    }

    public CritereEvaluation getCritere() {
        return critere;
    }

    public void setCritere(CritereEvaluation critere) {
        this.critere = critere;
    }

    public BigDecimal getNote() {
        return note;
    }

    public void setNote(BigDecimal note) {
        this.note = note;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }
}