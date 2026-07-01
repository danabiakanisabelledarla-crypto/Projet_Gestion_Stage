package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.repositories.JournalBordRepository;
import com.gestionstages.gestion_stages.repositories.LivrableRepository;
import com.gestionstages.gestion_stages.repositories.StagiaireRepository;
import com.gestionstages.gestion_stages.repositories.TacheRepository;
import com.gestionstages.gestion_stages.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/stagiaire")
public class StagiaireController {

    private final StagiaireRepository stagiaireRepository;
    private final TacheRepository tacheRepository;
    private final LivrableRepository livrableRepository;
    private final JournalBordRepository journalBordRepository;

    public StagiaireController(StagiaireRepository stagiaireRepository,
                                TacheRepository tacheRepository,
                                LivrableRepository livrableRepository,
                                JournalBordRepository journalBordRepository) {
        this.stagiaireRepository = stagiaireRepository;
        this.tacheRepository = tacheRepository;
        this.livrableRepository = livrableRepository;
        this.journalBordRepository = journalBordRepository;
    }

    @GetMapping("/dashboard")
    public String afficherDashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Integer utilisateurId = userDetails.getUtilisateur().getId();
        String nomStagiaire = userDetails.getUtilisateur().getPrenom();

        model.addAttribute("nomStagiaire", nomStagiaire);

        stagiaireRepository.findByUtilisateurId(utilisateurId).ifPresent(stagiaire -> {
            stagiaire.getDemandeStage();
            model.addAttribute("stagiaire", stagiaire);
        });

        if (!model.containsAttribute("stagiaire")) {
            model.addAttribute("stagiaire", null);
        }

        return "stagiaire/dashboard";
    }
}