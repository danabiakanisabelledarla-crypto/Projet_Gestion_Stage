package com.gestionstages.gestion_stages.controllers;
import java.math.BigDecimal;
import java.util.Map;

import com.gestionstages.gestion_stages.entities.*;
import com.gestionstages.gestion_stages.repositories.*;
import com.gestionstages.gestion_stages.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
    private final CritereEvaluationRepository critereRepository;
    private final EvaluationRepository evaluationRepository;
    private final NoteEvaluationRepository noteRepository;
    
    private final ObjectifRepository objectifRepository;
    public EncadreurController(EncadreurRepository encadreurRepository,
                           StageRepository stageRepository,
                           TacheRepository tacheRepository,
                           LivrableRepository livrableRepository,
                           ObjectifRepository objectifRepository,
                           CritereEvaluationRepository critereRepository,
                           EvaluationRepository evaluationRepository,
                           NoteEvaluationRepository noteRepository) {
            this.encadreurRepository = encadreurRepository;
            this.stageRepository = stageRepository;
            this.tacheRepository = tacheRepository;
            this.livrableRepository = livrableRepository;
            this.critereRepository = critereRepository;
            this.evaluationRepository = evaluationRepository;
            this.noteRepository = noteRepository;
            this.objectifRepository = objectifRepository;
}

    private Encadreur getEncadreur(CustomUserDetails userDetails) {
        return encadreurRepository
                .findByUtilisateurId(userDetails.getUtilisateur().getId())
                .orElse(null);
    }

    @GetMapping("/dashboard")
    public String afficherDashboard(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     Model model) {
        Encadreur encadreur = getEncadreur(userDetails);
        int nombreStagiaires = 0;
        int nombreTaches = 0;
        int nombreLivrables = 0;

        if (encadreur != null) {
            List<Stage> mesStages = stageRepository.findByEncadreurId(encadreur.getId());
            nombreStagiaires = mesStages.size();
            for (Stage stage : mesStages) {
                List<Tache> taches = tacheRepository.findByStageId(stage.getId());
                nombreTaches += taches.size();
                for (Tache tache : taches) {
                    nombreLivrables += livrableRepository.findByTacheId(tache.getId()).size();
                }
            }
        }

        model.addAttribute("nombreStagiaires", nombreStagiaires);
        model.addAttribute("nombreTaches", nombreTaches);
        model.addAttribute("nombreLivrables", nombreLivrables);
        return "encadreur/dashboard";
    }

    @GetMapping("/taches")
    public String afficherTaches(@AuthenticationPrincipal CustomUserDetails userDetails,
                                  Model model) {
        Encadreur encadreur = getEncadreur(userDetails);
        List<Stage> mesStages = new ArrayList<>();
        List<Tache> toutesLesTaches = new ArrayList<>();

        if (encadreur != null) {
            mesStages = stageRepository.findByEncadreurId(encadreur.getId());
            for (Stage stage : mesStages) {
                toutesLesTaches.addAll(tacheRepository.findByStageId(stage.getId()));
            }
        }

        model.addAttribute("mesStages", mesStages);
        model.addAttribute("toutesLesTaches", toutesLesTaches);
        return "encadreur/taches";
    }

    @PostMapping("/taches/creer")
    public String creerTache(@RequestParam Integer stageId,
                              @RequestParam String titre,
                              @RequestParam(required = false) String description,
                              @RequestParam String dateLimite,
                              Model model) {
        stageRepository.findById(stageId).ifPresent(stage -> {
            Tache tache = new Tache(stage, titre, description,
                    LocalDate.parse(dateLimite));
            tacheRepository.save(tache);
        });
        return "redirect:/encadreur/taches?succes=true";
    }
    @GetMapping("/evaluations")
public String afficherEvaluations(@AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model,
                                   @RequestParam(required = false) String succes) {
    Encadreur encadreur = getEncadreur(userDetails);
    List<Stage> mesStages = new ArrayList<>();
    List<Evaluation> evaluations = new ArrayList<>();

    if (encadreur != null) {
        mesStages = stageRepository.findByEncadreurId(encadreur.getId());
        for (Stage stage : mesStages) {
            evaluations.addAll(evaluationRepository.findByStageId(stage.getId()));
        }
    }

    model.addAttribute("mesStages", mesStages);
    model.addAttribute("criteres", critereRepository.findAll());
    model.addAttribute("evaluations", evaluations);
    if (succes != null) model.addAttribute("succes", succes);

    return "encadreur/evaluations";
}

@PostMapping("/evaluations/creer")
public String creerEvaluation(@RequestParam Integer stageId,
                               @RequestParam String typeEvaluation,
                               @RequestParam String dateEvaluation,
                               @RequestParam(required = false) String appreciation,
                               @RequestParam Map<String, String> allParams) {
    stageRepository.findById(stageId).ifPresent(stage -> {
        Evaluation evaluation = new Evaluation(
                stage,
                Evaluation.TypeEvaluation.valueOf(typeEvaluation),
                LocalDate.parse(dateEvaluation));
        evaluation.setAppreciation(appreciation);
        evaluationRepository.save(evaluation);

        // Enregistrer les notes par critere
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            if (entry.getKey().startsWith("note_") && !entry.getValue().isEmpty()) {
                try {
                    Integer critereId = Integer.parseInt(
                            entry.getKey().replace("note_", ""));
                    BigDecimal note = new BigDecimal(entry.getValue());

                    critereRepository.findById(critereId).ifPresent(critere -> {
                        NoteEvaluation noteEval = new NoteEvaluation(
                                evaluation, critere, note);
                        noteRepository.save(noteEval);
                    });
                } catch (NumberFormatException ignored) {}
            }
        }
    });

    return "redirect:/encadreur/evaluations?succes=true";
}
@GetMapping("/objectifs")
public String afficherObjectifs(@AuthenticationPrincipal CustomUserDetails userDetails,
                                Model model,
                                @RequestParam(required = false) String succes) {
    Encadreur encadreur = getEncadreur(userDetails);
    List<Stage> mesStages = new ArrayList<>();
    List<Objectif> objectifs = new ArrayList<>();

    if (encadreur != null) {
        mesStages = stageRepository.findByEncadreurId(encadreur.getId());
        for (Stage stage : mesStages) {
            objectifs.addAll(objectifRepository.findByStageIdOrderByOrdreAsc(stage.getId()));
        }
    }

    model.addAttribute("mesStages", mesStages);
    model.addAttribute("objectifs", objectifs);
    if (succes != null) model.addAttribute("succes", succes);

    return "encadreur/objectifs";
}

@PostMapping("/objectifs/creer")
public String creerObjectif(@RequestParam Integer stageId,
                             @RequestParam String libelle,
                             @RequestParam(required = false) String description,
                             @RequestParam Integer ordre) {
    stageRepository.findById(stageId).ifPresent(stage -> {
        Objectif objectif = new Objectif(stage, libelle, description, ordre);
        objectifRepository.save(objectif);
    });

    return "redirect:/encadreur/objectifs?succes=true";
}

@GetMapping("/livrables")
public String afficherLivrables(@AuthenticationPrincipal CustomUserDetails userDetails,
                                Model model) {
    Encadreur encadreur = getEncadreur(userDetails);
    List<Livrable> livrables = new ArrayList<>();

    if (encadreur != null) {
        List<Stage> mesStages = stageRepository.findByEncadreurId(encadreur.getId());
        for (Stage stage : mesStages) {
            List<Tache> taches = tacheRepository.findByStageId(stage.getId());
            for (Tache tache : taches) {
                livrables.addAll(livrableRepository.findByTacheId(tache.getId()));
            }
        }
    }

    model.addAttribute("livrables", livrables);
    return "encadreur/livrables";
}
}