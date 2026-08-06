package com.gestionstages.gestion_stages.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "admin_preferences")
public class AdminPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false, unique = true)
    private Utilisateur utilisateur;

    private boolean notificationsEmail = true;
    private boolean notificationsSysteme = true;
    private boolean rappelTaches = true;
    private boolean modeSombre;
    private String langue = "fr";

    public AdminPreference() {}

    public AdminPreference(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public Integer getId() { return id; }
    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur utilisateur) { this.utilisateur = utilisateur; }
    public boolean isNotificationsEmail() { return notificationsEmail; }
    public void setNotificationsEmail(boolean notificationsEmail) { this.notificationsEmail = notificationsEmail; }
    public boolean isNotificationsSysteme() { return notificationsSysteme; }
    public void setNotificationsSysteme(boolean notificationsSysteme) { this.notificationsSysteme = notificationsSysteme; }
    public boolean isRappelTaches() { return rappelTaches; }
    public void setRappelTaches(boolean rappelTaches) { this.rappelTaches = rappelTaches; }
    public boolean isModeSombre() { return modeSombre; }
    public void setModeSombre(boolean modeSombre) { this.modeSombre = modeSombre; }
    public String getLangue() { return langue; }
    public void setLangue(String langue) { this.langue = langue; }
}
