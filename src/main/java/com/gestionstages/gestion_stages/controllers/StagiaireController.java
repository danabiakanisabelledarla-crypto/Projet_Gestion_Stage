package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.entities.*;
import com.gestionstages.gestion_stages.repositories.*;
import com.gestionstages.gestion_stages.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/stagiaire")
public class StagiaireController {

    private final StageRepository stageRepository;
private final TacheRepository tacheRepository;
private final LivrableRepository livrableRepository;
private final JournalBordRepository journalBordRepository;
private final StagiaireRepository stagiaireRepository;

private static final String DOSSIER_UPLOAD = "uploads/";

public StagiaireController(StageRepository stageRepository,
                            TacheRepository tacheRepository,
                            LivrableRepository livrableRepository,
                            JournalBordRepository journalBordRepository,
                            StagiaireRepository stagiaireRepository) {
    this.stageRepository = stageRepository;
    this.tacheRepository = tacheRepository;
    this.livrableRepository = livrableRepository;
    this.journalBordRepository = journalBordRepository;
    this.stagiaireRepository = stagiaireRepository;
}

    private Optional<Stage> getStage(CustomUserDetails userDetails) {
    Integer utilisateurId = userDetails.getUtilisateur().getId();
    return stagiaireRepository.findByUtilisateurId(utilisateurId)
            .flatMap(stagiaire -> stageRepository.findByStagiaireId(stagiaire.getId()));
}

    @GetMapping("/dashboard")
    public String afficherDashboard(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     Model model) {
        Optional<Stage> stageOpt = getStage(userDetails);
        String nomComplet = userDetails.getUtilisateur().getPrenom()
                + " " + userDetails.getUtilisateur().getNom();
        model.addAttribute("nomComplet", nomComplet);

        if (stageOpt.isPresent()) {
            Stage stage = stageOpt.get();
            model.addAttribute("stage", stage);
            List<Tache> taches = tacheRepository.findByStageId(stage.getId());
            model.addAttribute("nombreTaches", taches.size());

            int totalLivrables = 0;
            for (Tache t : taches) {
                totalLivrables += livrableRepository.findByTacheId(t.getId()).size();
            }
            model.addAttribute("nombreLivrables", totalLivrables);
            model.addAttribute("nombreJours",
                    journalBordRepository
                            .findByStageIdOrderByDateActiviteDesc(stage.getId()).size());
        } else {
            model.addAttribute("stage", null);
            model.addAttribute("nombreTaches", 0);
            model.addAttribute("nombreLivrables", 0);
            model.addAttribute("nombreJours", 0);
        }
        return "stagiaire/dashboard";
    }

    @GetMapping("/journal")
    public String afficherJournal(@AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model,
                                   @RequestParam(required = false) String succes) {
        Optional<Stage> stageOpt = getStage(userDetails);

        if (stageOpt.isPresent()) {
            Stage stage = stageOpt.get();
            model.addAttribute("stage", stage);

            List<Tache> taches = tacheRepository.findByStageId(stage.getId());
            model.addAttribute("taches", taches);

            List<Livrable> livrables = new ArrayList<>();
            for (Tache t : taches) {
                livrables.addAll(livrableRepository.findByTacheId(t.getId()));
            }
            model.addAttribute("livrables", livrables);
        } else {
            model.addAttribute("stage", null);
            model.addAttribute("taches", new ArrayList<>());
            model.addAttribute("livrables", new ArrayList<>());
        }

        if (succes != null) model.addAttribute("succes", succes);
        return "stagiaire/journal";
    }

    @PostMapping("/journal/ajouter")
    public String ajouterJournal(@AuthenticationPrincipal CustomUserDetails userDetails,
                                  @RequestParam String dateActivite,
                                  @RequestParam String travauxRealises,
                                  @RequestParam(required = false) String difficultes) {
        getStage(userDetails).ifPresent(stage -> {
            JournalBord journal = new JournalBord(stage,
                    LocalDate.parse(dateActivite), travauxRealises);
            journal.setDifficultes(difficultes);
            journalBordRepository.save(journal);
        });
        return "redirect:/stagiaire/journal?succes=Journal enregistre avec succes.";
    }

    @PostMapping("/livrables/deposer")
public String deposerLivrable(@AuthenticationPrincipal CustomUserDetails userDetails,
                               @RequestParam Integer tacheId,
                               @RequestParam String titre,
                               @RequestParam MultipartFile fichier) {
    try {
        Files.createDirectories(Paths.get(DOSSIER_UPLOAD));
        String nomFichier = "livrable_" + tacheId + "_" + fichier.getOriginalFilename();
        Path chemin = Paths.get(DOSSIER_UPLOAD + nomFichier);
        Files.write(chemin, fichier.getBytes());

        tacheRepository.findById(tacheId).ifPresent(tache -> {
            Livrable livrable = new Livrable(tache, titre, null, chemin.toString());
            livrableRepository.save(livrable);
        });
    } catch (IOException e) {
        System.err.println("Erreur upload livrable : " + e.getMessage());
    }
    return "redirect:/stagiaire/journal?succes=Livrable depose avec succes.";
}

@GetMapping("/taches")
public String afficherTaches(@AuthenticationPrincipal CustomUserDetails userDetails,
                              Model model) {
    Optional<Stage> stageOpt = getStage(userDetails);
    List<Tache> taches = new ArrayList<>();

    if (stageOpt.isPresent()) {
        taches = tacheRepository.findByStageId(stageOpt.get().getId());
    }
    model.addAttribute("taches", taches);
    return "stagiaire/taches";
}

@GetMapping("/livrables")
public String afficherLivrables(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 Model model) {
    Optional<Stage> stageOpt = getStage(userDetails);
    List<Livrable> livrables = new ArrayList<>();

    if (stageOpt.isPresent()) {
        List<Tache> taches = tacheRepository.findByStageId(stageOpt.get().getId());
        for (Tache t : taches) {
            livrables.addAll(livrableRepository.findByTacheId(t.getId()));
        }
    }
    model.addAttribute("livrables", livrables);
    return "stagiaire/livrables";
}

@GetMapping("/rapport")
public String afficherRapport(@AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model) {
    Optional<Stage> stageOpt = getStage(userDetails);
    model.addAttribute("stage", stageOpt.orElse(null));
    return "stagiaire/rapport";
}
@PostMapping("/rapport/deposer")
public String deposerRapport(@AuthenticationPrincipal CustomUserDetails userDetails,
                              @RequestParam String titre,
                              @RequestParam MultipartFile fichier) {
    try {
        Files.createDirectories(Paths.get(DOSSIER_UPLOAD));
        String nomFichier = "rapport_" + userDetails.getUtilisateur().getId()
                + "_" + fichier.getOriginalFilename();
        Path chemin = Paths.get(DOSSIER_UPLOAD + nomFichier);
        Files.write(chemin, fichier.getBytes());
        System.out.println(">>> Rapport depose : " + chemin);
    } catch (IOException e) {
        System.err.println("Erreur upload rapport : " + e.getMessage());
    }
    return "redirect:/stagiaire/rapport?succes=Rapport depose avec succes.";
}
}