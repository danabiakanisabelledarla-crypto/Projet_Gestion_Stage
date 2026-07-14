package com.gestionstages.gestion_stages.dto;

import java.time.LocalDate;

public class EvenementPlanning {

    private final LocalDate date;
    private final String titre;
    private final String type;

    public EvenementPlanning(LocalDate date, String titre, String type) {
        this.date = date;
        this.titre = titre;
        this.type = type;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getTitre() {
        return titre;
    }
    public String getType() {
        return type;
    }
}//Ça sert à fusionner proprement les échéances de tâches + les évaluations
//  + la fin de stage en une seule liste triable par date, pour le calendrier