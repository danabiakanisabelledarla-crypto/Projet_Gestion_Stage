package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.entities.Stage;
import com.gestionstages.gestion_stages.repositories.DemandeStageRepository;
import com.gestionstages.gestion_stages.repositories.StageRepository;
import com.gestionstages.gestion_stages.repositories.TacheRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final DemandeStageRepository demandeStageRepository;
    private final StageRepository stageRepository;
    private final TacheRepository tacheRepository;

    public AdminController(DemandeStageRepository demandeStageRepository, StageRepository stageRepository,
                            TacheRepository tacheRepository) {
        this.demandeStageRepository = demandeStageRepository;
        this.stageRepository = stageRepository;
        this.tacheRepository = tacheRepository;
    }

    @GetMapping("/dashboard")
    public String afficherDashboard(Model model) {

        long nombreDemandes = demandeStageRepository.count();
        long stagesEnCours = stageRepository.findByStatut(Stage.StatutStage.en_cours).size();
        long stagesTermines = stageRepository.findByStatut(Stage.StatutStage.termine).size();
        long nombreTaches = tacheRepository.count();

        model.addAttribute("nombreDemandes", nombreDemandes);
        model.addAttribute("stagesEnCours", stagesEnCours);
        model.addAttribute("stagesTermines", stagesTermines);
        model.addAttribute("nombreTaches", nombreTaches);

        return "admin/dashboard";
    }
}