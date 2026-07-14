package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.dto.EvenementPlanning;
import java.util.Comparator;

import com.gestionstages.gestion_stages.entities.*;
import com.gestionstages.gestion_stages.repositories.*;
import com.gestionstages.gestion_stages.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.gestionstages.gestion_stages.repositories.EvaluationRepository;

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
private final DocumentRepository documentRepository;
private final ObjectifRepository objectifRepository;
private final EvaluationRepository evaluationRepository;
//private final EvaluationRepository evaluationRepository;

private static final String DOSSIER_UPLOAD = "uploads/";

public StagiaireController(StageRepository stageRepository,
                           TacheRepository tacheRepository,
                           LivrableRepository livrableRepository,
                           JournalBordRepository journalBordRepository,
                           StagiaireRepository stagiaireRepository,
                           DocumentRepository documentRepository,
                           ObjectifRepository objectifRepository,
                           EvaluationRepository evaluationRepository) {   // ← Ajoute cette ligne

    this.stageRepository = stageRepository;
    this.tacheRepository = tacheRepository;
    this.livrableRepository = livrableRepository;
    this.journalBordRepository = journalBordRepository;
    this.stagiaireRepository = stagiaireRepository;
    this.documentRepository = documentRepository;
    this.objectifRepository = objectifRepository;
    this.evaluationRepository = evaluationRepository;   // ← Ajoute cette ligne
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
    String prenom = userDetails.getUtilisateur().getPrenom();
    String nom = userDetails.getUtilisateur().getNom();
    String initiales = prenom.substring(0,1).toUpperCase()
            + nom.substring(0,1).toUpperCase();

    model.addAttribute("prenom", prenom);
    model.addAttribute("nomComplet", prenom + " " + nom);
    model.addAttribute("initiales", initiales);
    model.addAttribute("activePage", "dashboard");


    if (stageOpt.isPresent()) {
        Stage stage = stageOpt.get();
        model.addAttribute("stage", stage);

        List<Tache> taches = tacheRepository.findByStageId(stage.getId());
        long tachesEnCours = taches.stream()
                .filter(t -> t.getStatut() == Tache.StatutTache.en_cours)
                .count();
        long tachesAFaire = taches.stream()
                .filter(t -> t.getStatut() == Tache.StatutTache.a_faire)
                .count();

        List<Livrable> livrables = new ArrayList<>();
        for (Tache t : taches) {
            livrables.addAll(livrableRepository.findByTacheId(t.getId()));
        }
        long livrablesPending = livrables.stream()
                .filter(l -> l.getStatut() == Livrable.StatutLivrable.depose)
                .count();

        List<Objectif> objectifs = objectifRepository
                .findByStageIdOrderByOrdreAsc(stage.getId());
        long objectifsEnCours = objectifs.stream()
                .filter(o -> o.getStatut() == Objectif.StatutObjectif.en_cours)
                .count();

        int totalTaches = taches.size();
        int tachesTerminees = (int) taches.stream()
                .filter(t -> t.getStatut() == Tache.StatutTache.terminee)
                .count();
        int progression = totalTaches > 0
                ? (tachesTerminees * 100 / totalTaches) : 0;

        List<JournalBord> journaux = journalBordRepository
                .findByStageIdOrderByDateActiviteDesc(stage.getId());

        model.addAttribute("nombreTaches", totalTaches);
        model.addAttribute("tachesEnCours", tachesEnCours);
        model.addAttribute("tachesRecentes", taches.stream().limit(3).toList());
        model.addAttribute("nombreLivrables", livrables.size());
        model.addAttribute("livrablesPending", livrablesPending);
        model.addAttribute("nombreObjectifs", objectifs.size());
        model.addAttribute("objectifsEnCours", objectifsEnCours);
        model.addAttribute("progression", progression);
        model.addAttribute("nombreJours", journaux.size());

    } else {
        model.addAttribute("stage", null);
        model.addAttribute("nombreTaches", 0);
        model.addAttribute("tachesEnCours", 0);
        model.addAttribute("tachesRecentes", new ArrayList<>());
        model.addAttribute("nombreLivrables", 0);
        model.addAttribute("livrablesPending", 0);
        model.addAttribute("nombreObjectifs", 0);
        model.addAttribute("objectifsEnCours", 0);
        model.addAttribute("progression", 0);
        model.addAttribute("nombreJours", 0);
    }

    return "stagiaire/dashboard";
}
// dans afficherJournal
    @GetMapping("/journal")
    public String afficherJournal(@AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model,
                                   @RequestParam(required = false) String succes) {
        Optional<Stage> stageOpt = getStage(userDetails);
        model.addAttribute("activePage", "journal");
        String prenom = userDetails.getUtilisateur().getPrenom();
        String nom = userDetails.getUtilisateur().getNom();
        model.addAttribute("prenom", prenom);
        model.addAttribute("nomComplet", prenom + " " + nom);
        model.addAttribute("initiales", prenom.substring(0,1).toUpperCase()
                + nom.substring(0,1).toUpperCase());

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
            List<JournalBord> journaux = journalBordRepository
                    .findByStageIdOrderByDateActiviteDesc(stage.getId());
            model.addAttribute("journaux", journaux);
            model.addAttribute("nombreEntrees", journaux.size());
        } else {
            model.addAttribute("journaux", new ArrayList<>());
            model.addAttribute("nombreEntrees", 0);
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
                                  @RequestParam(required = false) String difficultes,
                                  @RequestParam(required = false) String solutions) {
        getStage(userDetails).ifPresent(stage -> {
            JournalBord journal = new JournalBord(stage,
                    LocalDate.parse(dateActivite), travauxRealises);

            journal.setDifficultes(difficultes);
            journal.setObservations(solutions);
            journalBordRepository.save(journal);
            journal.setObservations(solutions);
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

    model.addAttribute("activePage", "taches");
    if (stageOpt.isPresent()) {
        taches = tacheRepository.findByStageId(stageOpt.get().getId());
    }
    String prenom = userDetails.getUtilisateur().getPrenom();
    String nom = userDetails.getUtilisateur().getNom();
    model.addAttribute("prenom", prenom);
    model.addAttribute("nomComplet", prenom + " " + nom);
    model.addAttribute("initiales", prenom.substring(0,1).toUpperCase()
        + nom.substring(0,1).toUpperCase());
    model.addAttribute("taches", taches);
    long tachesAFaire = taches.stream().filter(t -> t.getStatut() == Tache.StatutTache.a_faire).count();
    long tachesEnCours = taches.stream().filter(t -> t.getStatut() == Tache.StatutTache.en_cours).count();
    long tachesTerminees = taches.stream().filter(t -> t.getStatut() == Tache.StatutTache.terminee).count();
    model.addAttribute("tachesAFaire", tachesAFaire);
    model.addAttribute("tachesEnCours", tachesEnCours);
    model.addAttribute("tachesTerminees", tachesTerminees);
    return "stagiaire/taches";

}

@GetMapping("/livrables")
public String afficherLivrables(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 Model model) {
    Optional<Stage> stageOpt = getStage(userDetails);
    List<Livrable> livrables = new ArrayList<>();

    model.addAttribute("activePage", "livrables");

    String prenom = userDetails.getUtilisateur().getPrenom();
    String nom = userDetails.getUtilisateur().getNom();
    model.addAttribute("prenom", prenom);
    model.addAttribute("nomComplet", prenom + " " + nom);
    model.addAttribute("initiales", prenom.substring(0,1).toUpperCase()
            + nom.substring(0,1).toUpperCase());

    if (stageOpt.isPresent()) {
        List<Tache> taches = tacheRepository.findByStageId(stageOpt.get().getId());
        for (Tache t : taches) {
            livrables.addAll(livrableRepository.findByTacheId(t.getId()));
        }
    }
    model.addAttribute("livrables", livrables);
    model.addAttribute("nombreLivrables", livrables.size());
    return "stagiaire/livrables";
}

@GetMapping("/rapport")
public String afficherRapport(@AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model,
                               @RequestParam(required = false) String succes) {
    
    Optional<Stage> stageOpt = getStage(userDetails);
    model.addAttribute("stage", stageOpt.orElse(null));

    model.addAttribute("activePage", "rapport");
    String prenom = userDetails.getUtilisateur().getPrenom();
    String nom = userDetails.getUtilisateur().getNom();
    model.addAttribute("prenom", prenom);
    model.addAttribute("nomComplet", prenom + " " + nom);
    model.addAttribute("initiales", prenom.substring(0,1).toUpperCase()
            + nom.substring(0,1).toUpperCase());
    
    stageOpt.ifPresent(stage -> {
        List<Document> rapports = documentRepository.findByStageId(stage.getId()).stream()
                .filter(d -> "rapport_final".equals(d.getTypeDocument()))
                .toList();
        
        // Trier manuellement si la méthode existe
        model.addAttribute("rapports", rapports);
        
        if (!rapports.isEmpty()) {
            model.addAttribute("dernierRapport", rapports.get(0));
            model.addAttribute("nombreVersions", rapports.size());
        }
    });
    
    if (succes != null) model.addAttribute("succes", succes);
    return "stagiaire/rapport";
}

@PostMapping("/rapport/deposer")
public String deposerRapport(@AuthenticationPrincipal CustomUserDetails userDetails,
                              @RequestParam String titre,
                              @RequestParam MultipartFile fichier) {
    Optional<Stage> stageOpt = getStage(userDetails);
    if (stageOpt.isEmpty()) {
        return "redirect:/stagiaire/rapport";
    }
    try {
        Files.createDirectories(Paths.get(DOSSIER_UPLOAD));
        String nomFichier = "rapport_" + userDetails.getUtilisateur().getId()
                + "_" + fichier.getOriginalFilename();
        Path chemin = Paths.get(DOSSIER_UPLOAD + nomFichier);
        Files.write(chemin, fichier.getBytes());

        Document document = new Document(
                fichier.getOriginalFilename(), "rapport_final", chemin.toString());
        document.setStage(stageOpt.get());
        documentRepository.save(document);
    } catch (IOException e) {
        System.err.println("Erreur upload rapport : " + e.getMessage());
        return "redirect:/stagiaire/rapport?succes=Erreur lors du depot du rapport.";
    }
    return "redirect:/stagiaire/rapport?succes=Rapport depose avec succes.";
}

@GetMapping("/profil")
public String afficherProfilStagiaire(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    model.addAttribute("activePage", "profil");
    model.addAttribute("utilisateur", userDetails.getUtilisateur());
    stagiaireRepository.findByUtilisateurId(userDetails.getUtilisateur().getId())
            .ifPresent(s -> model.addAttribute("stagiaire", s));
    model.addAttribute("stage", getStage(userDetails).orElse(null));
    return "stagiaire/profil";
}



@GetMapping("/objectifs")
public String afficherObjectifsStagiaire(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    model.addAttribute("activePage", "objectifs");
    Optional<Stage> stageOpt = getStage(userDetails);
    model.addAttribute("stage", stageOpt.orElse(null));

    List<Objectif> objectifs = stageOpt.isPresent()
            ? objectifRepository.findByStageIdOrderByOrdreAsc(stageOpt.get().getId())
            : new ArrayList<>();

    model.addAttribute("objectifs", objectifs);
    model.addAttribute("nombreObjectifs", objectifs.size());
    model.addAttribute("objectifsAtteints", objectifs.stream()
            .filter(o -> o.getStatut() == Objectif.StatutObjectif.atteint).count());
    model.addAttribute("objectifsEnCours", objectifs.stream()
            .filter(o -> o.getStatut() == Objectif.StatutObjectif.en_cours).count());
    model.addAttribute("objectifsNonCommences", objectifs.stream()
            .filter(o -> o.getStatut() == Objectif.StatutObjectif.non_commence).count());

    return "stagiaire/objectifs";
}

@GetMapping("/planning")
public String afficherPlanningStagiaire(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    model.addAttribute("activePage", "planning");
    Optional<Stage> stageOpt = getStage(userDetails);
    model.addAttribute("stage", stageOpt.orElse(null));

    List<EvenementPlanning> evenements = new ArrayList<>();

    if (stageOpt.isPresent()) {
        Stage stage = stageOpt.get();

        List<Tache> taches = tacheRepository.findByStageId(stage.getId());
        for (Tache t : taches) {
            if (t.getDateLimite() != null) {
                evenements.add(new EvenementPlanning(
                        t.getDateLimite(), "Echeance : " + t.getTitre(), "tache"));
            }
        }

        List<Evaluation> evaluations = evaluationRepository.findByStageId(stage.getId());
        for (Evaluation e : evaluations) {
            evenements.add(new EvenementPlanning(
                    e.getDateEvaluation(), "Evaluation (" + e.getTypeEvaluation() + ")", "evaluation"));
        }
        if (stage.getDateFin() != null) {
            evenements.add(new EvenementPlanning(stage.getDateFin(), "Fin du stage", "fin_stage"));
        }
    }

    evenements.sort(Comparator.comparing(EvenementPlanning::getDate));
    model.addAttribute("evenements", evenements);

    LocalDate aujourdHui = LocalDate.now();
    model.addAttribute("evenementsAvenir", evenements.stream()
            .filter(e -> !e.getDate().isBefore(aujourdHui))
            .toList());

    return "stagiaire/planning";
}

@GetMapping("/messages")
public String afficherMessages(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    model.addAttribute("activePage", "messages");
    String prenom = userDetails.getUtilisateur().getPrenom();
    String nom = userDetails.getUtilisateur().getNom();
    model.addAttribute("prenom", prenom);
    model.addAttribute("nomComplet", prenom + " " + nom);
    model.addAttribute("initiales", prenom.substring(0,1).toUpperCase() + nom.substring(0,1).toUpperCase());
    getStage(userDetails).ifPresent(s -> model.addAttribute("stage", s));
    return "stagiaire/messages";
}

}