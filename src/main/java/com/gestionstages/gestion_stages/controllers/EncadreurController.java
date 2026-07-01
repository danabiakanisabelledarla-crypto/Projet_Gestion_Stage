package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.entities.Encadreur;
import com.gestionstages.gestion_stages.entities.Stage;
import com.gestionstages.gestion_stages.entities.Tache;
import com.gestionstages.gestion_stages.entities.Livrable;
import com.gestionstages.gestion_stages.repositories.EncadreurRepository;
import com.gestionstages.gestion_stages.repositories.StageRepository;
import com.gestionstages.gestion_stages.repositories.TacheRepository;
import com.gestionstages.gestion_stages.repositories.LivrableRepository;
import com.gestionstages.gestion_stages.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/encadreur")
public class EncadreurController {

    private final EncadreurRepository encadreurRepository;
    private final StageRepository stageRepository;
    private final TacheRepository tacheRepository;
    private final LivrableRepository livrableRepository;

    public EncadreurController(EncadreurRepository encadreurRepository,
                                StageRepository stageRepository,
                                TacheRepository tacheRepository,
                                LivrableRepository livrableRepository) {
        this.encadreurRepository = encadreurRepository;
        this.stageRepository = stageRepository;
        this.tacheRepository = tacheRepository;
        this.livrableRepository = livrableRepository;
    }

    @GetMapping("/dashboard")
    public String afficherDashboard(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     Model model) {

        Integer utilisateurId = userDetails.getUtilisateur().getId();

        // Chercher la fiche encadreur liée à cet utilisateur
        Optional<Encadreur> encadreurOpt = encadreurRepository
                .findByUtilisateurId(utilisateurId);

        int nombreStagiaires = 0;
        int nombreTaches = 0;
        int nombreLivrables = 0;

        if (encadreurOpt.isPresent()) {
            Encadreur encadreur = encadreurOpt.get();

            // Récupérer les stages de cet encadreur
            List<Stage> mesStages = stageRepository
                    .findByEncadreurId(encadreur.getId());
            nombreStagiaires = mesStages.size();

            // Pour chaque stage, compter les tâches et livrables
            for (Stage stage : mesStages) {
                List<Tache> taches = tacheRepository
                        .findByStageId(stage.getId());
                nombreTaches += taches.size();

                for (Tache tache : taches) {
                    nombreLivrables += livrableRepository
                            .findByTacheId(tache.getId()).size();
                }
            }
        }

        model.addAttribute("nombreStagiaires", nombreStagiaires);
        model.addAttribute("nombreTaches", nombreTaches);
        model.addAttribute("nombreLivrables", nombreLivrables);

        return "encadreur/dashboard";
    }
}