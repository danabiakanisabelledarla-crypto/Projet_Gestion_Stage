package com.gestionstages.gestion_stages.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class EvenementPlanning {

    private final LocalDate date;
    private final String titre;
    private final String type;
    private final LocalTime heure;
    private final String description;

    public EvenementPlanning(LocalDate date, String titre, String type) {
        this(date, titre, type, null, null);
    }

    public EvenementPlanning(LocalDate date, String titre, String type,
                             LocalTime heure, String description) {
        this.date = date;
        this.titre = titre;
        this.type = type;
        this.heure = heure;
        this.description = description;
    }

    public LocalDate getDate() { return date; }
    public String getTitre() { return titre; }
    public String getType() { return type; }
    public LocalTime getHeure() { return heure; }
    public String getDescription() { return description; }
}
