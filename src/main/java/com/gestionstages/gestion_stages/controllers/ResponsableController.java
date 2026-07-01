package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.entities.DemandeStage;
import com.gestionstages.gestion_stages.repositories.DemandeStageRepository;
import com.gestionstages.gestion_stages.repositories.StageRepository;
import com.gestionstages.gestion_stages.repositories.StagiaireRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/responsable")
public class ResponsableController {

    private final DemandeStageRepository demandeStageRepository;
    private final StagiaireRepository stagiaireRepository;
    private final StageRepository stageRepository;

    public ResponsableController(DemandeStageRepository demandeStageRepository,
                                  StagiaireRepository stagiaireRepository,
                                  StageRepository stageRepository) {
        this.demandeStageRepository = demandeStageRepository;
        this.stagiaireRepository = stagiaireRepository;
        this.stageRepository = stageRepository;
    }

    @GetMapping("/dashboard")
    public String afficherDashboard(Model model) {
        long demandesEnAttente = demandeStageRepository
                .findByStatut(DemandeStage.StatutDemande.en_attente).size();
        long totalStagiaires = stagiaireRepository.count();
        long totalStages = stageRepository.count();

        model.addAttribute("demandesEnAttente", demandesEnAttente);
        model.addAttribute("totalStagiaires", totalStagiaires);
        model.addAttribute("totalStages", totalStages);

        return "responsable/dashboard";
    }

    @GetMapping("/demandes")
    public String afficherDemandes(Model model) {
        List<DemandeStage> demandes = demandeStageRepository.findAll();
        model.addAttribute("demandes", demandes);
        return "responsable/demandes";
    }

    @GetMapping("/demandes/accepter/{id}")
    public String accepterDemande(@PathVariable Integer id) {
        demandeStageRepository.findById(id).ifPresent(demande -> {
            demande.setStatut(DemandeStage.StatutDemande.acceptee);
            demandeStageRepository.save(demande);
        });
        return "redirect:/responsable/demandes";
    }

    @GetMapping("/demandes/refuser/{id}")
    public String refuserDemande(@PathVariable Integer id) {
        demandeStageRepository.findById(id).ifPresent(demande -> {
            demande.setStatut(DemandeStage.StatutDemande.refusee);
            demandeStageRepository.save(demande);
        });
        return "redirect:/responsable/demandes";
    }

    @GetMapping("/demandes/attente/{id}")
    public String mettreEnAttente(@PathVariable Integer id) {
        demandeStageRepository.findById(id).ifPresent(demande -> {
            demande.setStatut(DemandeStage.StatutDemande.en_attente);
            demandeStageRepository.save(demande);
        });
        return "redirect:/responsable/demandes";
    }
}