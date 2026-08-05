package com.gestionstages.gestion_stages.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "responsable_preferences")
public class ResponsablePreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false, unique = true)
    private Utilisateur utilisateur;

    private boolean notificationsEmail = true;
    private boolean notificationsSysteme = true;
    private boolean modeSombre = false;
    private boolean alertesSoutenance = true;
    private boolean alertesRapport = true;

    public ResponsablePreference() {}

    public ResponsablePreference(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public Integer getId() { return id; }
    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur utilisateur) { this.utilisateur = utilisateur; }
    public boolean isNotificationsEmail() { return notificationsEmail; }
    public void setNotificationsEmail(boolean notificationsEmail) { this.notificationsEmail = notificationsEmail; }
    public boolean isNotificationsSysteme() { return notificationsSysteme; }
    public void setNotificationsSysteme(boolean notificationsSysteme) { this.notificationsSysteme = notificationsSysteme; }
    public boolean isModeSombre() { return modeSombre; }
    public void setModeSombre(boolean modeSombre) { this.modeSombre = modeSombre; }
    public boolean isAlertesSoutenance() { return alertesSoutenance; }
    public void setAlertesSoutenance(boolean alertesSoutenance) { this.alertesSoutenance = alertesSoutenance; }
    public boolean isAlertesRapport() { return alertesRapport; }
    public void setAlertesRapport(boolean alertesRapport) { this.alertesRapport = alertesRapport; }
}
