package com.gestionstages.gestion_stages.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 200)
    private String objet;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "destinataire_type", nullable = false, length = 50)
    private String destinataireType;

    @Column(length = 150)
    private String destinataireEmail;

    @Column(nullable = false, length = 20)
    private String priorite = "normale";

    @Column(name = "date_envoi", nullable = false)
    private LocalDateTime dateEnvoi = LocalDateTime.now();

    @Column(nullable = false, length = 20)
    private String statut = "envoyee";

    @Column(name = "auteur_nom", length = 100)
    private String auteurNom;

    public Notification() {}

    public Notification(String objet, String message, String destinataireType, String priorite, String auteurNom) {
        this.objet = objet;
        this.message = message;
        this.destinataireType = destinataireType;
        this.priorite = priorite;
        this.auteurNom = auteurNom;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getObjet() { return objet; }
    public void setObjet(String objet) { this.objet = objet; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getDestinataireType() { return destinataireType; }
    public void setDestinataireType(String destinataireType) { this.destinataireType = destinataireType; }
    public String getDestinataireEmail() { return destinataireEmail; }
    public void setDestinataireEmail(String destinataireEmail) { this.destinataireEmail = destinataireEmail; }
    public String getPriorite() { return priorite; }
    public void setPriorite(String priorite) { this.priorite = priorite; }
    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getAuteurNom() { return auteurNom; }
    public void setAuteurNom(String auteurNom) { this.auteurNom = auteurNom; }
}
