package com.gestionstages.gestion_stages.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 255)
    private String sujet;

    @ManyToMany
    @JoinTable(name = "conversation_participants",
            joinColumns = @JoinColumn(name = "conversation_id"),
            inverseJoinColumns = @JoinColumn(name = "utilisateur_id"))
    private Set<Utilisateur> participants = new HashSet<>();

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "dernier_message")
    private LocalDateTime dernierMessage;

    public Conversation() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getSujet() { return sujet; }
    public void setSujet(String sujet) { this.sujet = sujet; }

    public Set<Utilisateur> getParticipants() { return participants; }
    public void setParticipants(Set<Utilisateur> participants) { this.participants = participants; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDernierMessage() { return dernierMessage; }
    public void setDernierMessage(LocalDateTime dernierMessage) { this.dernierMessage = dernierMessage; }
}
