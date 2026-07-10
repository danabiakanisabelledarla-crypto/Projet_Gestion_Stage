package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.repositories.StagiaireRepository;
import com.gestionstages.gestion_stages.repositories.EncadreurRepository;
import com.gestionstages.gestion_stages.entities.Stage;
import com.gestionstages.gestion_stages.repositories.DemandeStageRepository;
import com.gestionstages.gestion_stages.repositories.StageRepository;
import com.gestionstages.gestion_stages.repositories.TacheRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
@RequestMapping("/admin")
public class AdminController {

    private final DemandeStageRepository demandeStageRepository;
    private final StageRepository stageRepository;
    private final TacheRepository tacheRepository;
    private final StagiaireRepository stagiaireRepository;
    private final EncadreurRepository encadreurRepository;

    public AdminController(DemandeStageRepository demandeStageRepository, StageRepository stageRepository,
                            TacheRepository tacheRepository, StagiaireRepository stagiaireRepository, EncadreurRepository encadreurRepository) {
        this.demandeStageRepository = demandeStageRepository;
        this.stageRepository = stageRepository;
        this.tacheRepository = tacheRepository;
        this.stagiaireRepository = stagiaireRepository;
        this.encadreurRepository = encadreurRepository;
    }

    @GetMapping("/dashboard")
    public String afficherDashboard(Model model) {
        model.addAttribute("activePage", "dashboard");

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
    @GetMapping("/demandes")
public String afficherDemandes(Model model) {
    model.addAttribute("activePage", "demandes");
    model.addAttribute("demandes", demandeStageRepository.findAll());
    return "admin/demandes";
}

@GetMapping({"/stagiaires", "/stagiaire"})
public String afficherStagiaires(Model model) {
    model.addAttribute("activePage", "stagiaires");
    model.addAttribute("stagiaires", stagiaireRepository.findAll());
    return "admin/stagiaires";
}

@GetMapping("/encadreurs")
public String afficherEncadreurs(Model model) {
    model.addAttribute("activePage", "encadreurs");
    model.addAttribute("encadreurs", encadreurRepository.findAll());
    return "admin/encadreurs";
}

@GetMapping("/statistiques")
public String afficherStatistiques(Model model) {
    model.addAttribute("activePage", "statistiques");
    model.addAttribute("nombreDemandes", demandeStageRepository.count());
    model.addAttribute("nombreStagiaires", stagiaireRepository.count());
    model.addAttribute("nombreEncadreurs", encadreurRepository.count());
    model.addAttribute("nombreStages", stageRepository.count());
    model.addAttribute("nombreTaches", tacheRepository.count());
    model.addAttribute("stagesEnCours", stageRepository.findByStatut(Stage.StatutStage.en_cours).size());
    model.addAttribute("stagesTermines", stageRepository.findByStatut(Stage.StatutStage.termine).size());

    return "admin/statistiques";
}
    
}