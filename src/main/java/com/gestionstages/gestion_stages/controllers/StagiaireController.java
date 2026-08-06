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
import org.springframework.format.annotation.DateTimeFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.gestionstages.gestion_stages.repositories.UtilisateurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

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
private final EvenementPersonnelRepository evenementPersonnelRepository;

private final UtilisateurRepository utilisateurRepository;
private final PasswordEncoder passwordEncoder;
private final ConversationRepository conversationRepository;
private final MessageRepository messageRepository;
private final ProjetRepository projetRepository;
private final NotificationRepository notificationRepository;
private final com.gestionstages.gestion_stages.services.MessagingService messagingService;

private static final String DOSSIER_UPLOAD = "uploads/";

public StagiaireController(StageRepository stageRepository,
                           TacheRepository tacheRepository,
                           LivrableRepository livrableRepository,
                           JournalBordRepository journalBordRepository,
                           StagiaireRepository stagiaireRepository,
                           DocumentRepository documentRepository,
                           ObjectifRepository objectifRepository,
                           EvaluationRepository evaluationRepository,
                           EvenementPersonnelRepository evenementPersonnelRepository,
                           UtilisateurRepository utilisateurRepository,
                           PasswordEncoder passwordEncoder,
                           ConversationRepository conversationRepository,
                           MessageRepository messageRepository,
                           ProjetRepository projetRepository,
                           NotificationRepository notificationRepository,
                           com.gestionstages.gestion_stages.services.MessagingService messagingService) {

    this.stageRepository = stageRepository;
    this.tacheRepository = tacheRepository;
    this.livrableRepository = livrableRepository;
    this.journalBordRepository = journalBordRepository;
    this.stagiaireRepository = stagiaireRepository;
    this.documentRepository = documentRepository;
    this.objectifRepository = objectifRepository;
    this.evaluationRepository = evaluationRepository;
    this.evenementPersonnelRepository = evenementPersonnelRepository;
    this.utilisateurRepository = utilisateurRepository;
    this.passwordEncoder = passwordEncoder;
    this.conversationRepository = conversationRepository;
    this.messageRepository = messageRepository;
    this.projetRepository = projetRepository;
    this.notificationRepository = notificationRepository;
    this.messagingService = messagingService;
}

private Optional<Stage> getStage(CustomUserDetails userDetails) {
    Integer utilisateurId = userDetails.getUtilisateur().getId();
    return stagiaireRepository.findByUtilisateurId(utilisateurId)
            .flatMap(stagiaire -> stageRepository.findByStagiaireId(stagiaire.getId()));
}

private void recalculerProgression(Stage stage) {
    List<Tache> taches = tacheRepository.findByStageId(stage.getId());
    List<Objectif> objectifs = objectifRepository.findByStageIdOrderByOrdreAsc(stage.getId());
    List<Livrable> livrables = livrableRepository.findByStageId(stage.getId());

    double progressionTaches = taches.isEmpty() ? 0 : taches.stream()
            .mapToInt(tache -> tache.getStatut() == Tache.StatutTache.terminee ? 100
                    : (tache.getStatut() == Tache.StatutTache.en_cours
                    || tache.getStatut() == Tache.StatutTache.en_revue ? 50 : 0))
            .average().orElse(0);
    double progressionObjectifs = objectifs.stream()
            .mapToInt(Objectif::getProgression)
            .average().orElse(0);
    double progressionLivrables = livrables.isEmpty() ? 0 : livrables.stream()
            .mapToInt(livrable -> livrable.getStatut() == Livrable.StatutLivrable.valide ? 100
                    : (livrable.getStatut() == Livrable.StatutLivrable.depose ? 50 : 0))
            .average().orElse(0);
    int progression = (int) Math.round(
            progressionTaches * 0.5 + progressionObjectifs * 0.3 + progressionLivrables * 0.2);

    Stagiaire stagiaire = stage.getStagiaire();
    if (stagiaire != null) {
        stagiaire.setProgression(Math.max(0, Math.min(100, progression)));
        stagiaireRepository.save(stagiaire);
    }
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
        int progressionTaches = totalTaches > 0
                ? (tachesTerminees * 100 / totalTaches) : 0;
        int progressionObjectifs = objectifs.isEmpty() ? 0
                : (int) Math.round(objectifs.stream()
                        .mapToInt(Objectif::getProgression)
                        .average()
                        .orElse(0));
        long livrablesValides = livrables.stream()
                .filter(livrable -> livrable.getStatut() == Livrable.StatutLivrable.valide)
                .count();
        int progressionLivrables = livrables.isEmpty() ? 0
                : (int) (livrablesValides * 100 / livrables.size());
        int progression = (int) Math.round(
                progressionTaches * 0.5
                        + progressionObjectifs * 0.3
                        + progressionLivrables * 0.2);

        List<JournalBord> journaux = journalBordRepository
                .findByStageIdOrderByDateActiviteDesc(stage.getId());
        List<Evaluation> evaluations = evaluationRepository.findByStageId(stage.getId()).stream()
                .sorted(Comparator.comparing(Evaluation::getDateEvaluation).reversed())
                .toList();
        List<EvenementPlanning> evenementsDashboard = new ArrayList<>();
        taches.stream().filter(tache -> tache.getDateLimite() != null).forEach(tache ->
                evenementsDashboard.add(new EvenementPlanning(
                        tache.getDateLimite(), tache.getTitre(), "tache")));
        objectifs.stream().filter(objectif -> objectif.getDateLimite() != null).forEach(objectif ->
                evenementsDashboard.add(new EvenementPlanning(
                        objectif.getDateLimite(), objectif.getLibelle(), "objectif")));
        evaluations.forEach(evaluation -> evenementsDashboard.add(new EvenementPlanning(
                evaluation.getDateEvaluation(),
                "Evaluation " + evaluation.getTypeEvaluation().name().replace("_", ""),
                "evaluation")));
        evenementPersonnelRepository.findByStageId(stage.getId()).forEach(evenement ->
                evenementsDashboard.add(new EvenementPlanning(
                        evenement.getDate(),
                        evenement.getMotif(),
                        evenement.getTypeCouleur(),
                        evenement.getHeure(),
                        evenement.getDescription())));
        evenementsDashboard.sort(Comparator.comparing(EvenementPlanning::getDate));

        model.addAttribute("nombreTaches", totalTaches);
        model.addAttribute("taches", taches);
        model.addAttribute("tachesEnCours", tachesEnCours);
        model.addAttribute("tachesTerminees", tachesTerminees);
        model.addAttribute("tachesRecentes", taches.stream().limit(3).toList());
        model.addAttribute("nombreLivrables", livrables.size());
        model.addAttribute("livrables", livrables);
        model.addAttribute("livrablesPending", livrablesPending);
        model.addAttribute("nombreObjectifs", objectifs.size());
        model.addAttribute("objectifs", objectifs);
        model.addAttribute("objectifsEnCours", objectifsEnCours);
        model.addAttribute("progression", progression);
        model.addAttribute("progressionTaches", progressionTaches);
        model.addAttribute("progressionObjectifs", progressionObjectifs);
        model.addAttribute("progressionLivrables", progressionLivrables);
        model.addAttribute("nombreJours", journaux.size());
        model.addAttribute("journaux", journaux);
        model.addAttribute("journauxRecents", journaux.stream().limit(3).toList());
        model.addAttribute("evaluationsRecentes", evaluations.stream().limit(3).toList());
        model.addAttribute("nombreEvaluations", evaluations.size());
        model.addAttribute("evenementsDashboard", evenementsDashboard);
        long joursRestants = stage.getDateFin() == null
                ? 0
                : Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), stage.getDateFin()));
        int progressionTemps = 0;
        if (stage.getDateDebut() != null && stage.getDateFin() != null) {
            long dureeTotale = Math.max(1, ChronoUnit.DAYS.between(stage.getDateDebut(), stage.getDateFin()));
            long dureeEcoulee = ChronoUnit.DAYS.between(stage.getDateDebut(), LocalDate.now());
            progressionTemps = (int) Math.max(0, Math.min(100, dureeEcoulee * 100 / dureeTotale));
        }
        model.addAttribute("joursRestants", joursRestants);
        model.addAttribute("progressionTemps", progressionTemps);

    } else {
        model.addAttribute("stage", null);
        model.addAttribute("nombreTaches", 0);
        model.addAttribute("taches", new ArrayList<>());
        model.addAttribute("tachesEnCours", 0);
        model.addAttribute("tachesTerminees", 0);
        model.addAttribute("tachesRecentes", new ArrayList<>());
        model.addAttribute("nombreLivrables", 0);
        model.addAttribute("livrables", new ArrayList<>());
        model.addAttribute("livrablesPending", 0);
        model.addAttribute("nombreObjectifs", 0);
        model.addAttribute("objectifs", new ArrayList<>());
        model.addAttribute("objectifsEnCours", 0);
        model.addAttribute("progression", 0);
        model.addAttribute("progressionTaches", 0);
        model.addAttribute("progressionObjectifs", 0);
        model.addAttribute("progressionLivrables", 0);
        model.addAttribute("nombreJours", 0);
        model.addAttribute("journaux", new ArrayList<>());
        model.addAttribute("journauxRecents", new ArrayList<>());
        model.addAttribute("evaluationsRecentes", new ArrayList<>());
        model.addAttribute("nombreEvaluations", 0);
        model.addAttribute("evenementsDashboard", new ArrayList<>());
        model.addAttribute("joursRestants", 0);
        model.addAttribute("progressionTemps", 0);
    }

    return "stagiaire/dashboard";
}
// dans afficherJournal
    @GetMapping("/journal")
    public String afficherJournal(@AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model,
                                   @RequestParam(defaultValue = "semaine") String periode,
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
            LocalDate debutPeriode = "mois".equals(periode)
                    ? LocalDate.now().withDayOfMonth(1)
                    : LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1L);
            List<JournalBord> journauxFiltres = journaux.stream()
                    .filter(journal -> !journal.getDateActivite().isBefore(debutPeriode))
                    .toList();
            List<Objectif> objectifs = objectifRepository.findByStageIdOrderByOrdreAsc(stage.getId());
            long objectifsAtteints = objectifs.stream()
                    .filter(objectif -> objectif.getStatut() == Objectif.StatutObjectif.atteint)
                    .count();
            long tachesTerminees = taches.stream()
                    .filter(tache -> tache.getStatut() == Tache.StatutTache.terminee)
                    .count();
            int progressionStage = taches.isEmpty() ? 0
                    : (int) (tachesTerminees * 100 / taches.size());
            model.addAttribute("journaux", journauxFiltres);
            model.addAttribute("journauxRecents", journaux.stream().limit(5).toList());
            model.addAttribute("nombreEntrees", journaux.size());
            model.addAttribute("periodeJournal", "mois".equals(periode) ? "mois" : "semaine");
            model.addAttribute("commentairesEncadreur", livrables.stream()
                    .filter(livrable -> livrable.getCommentaireEncadreur() != null
                            && !livrable.getCommentaireEncadreur().isBlank())
                    .limit(4)
                    .toList());
            model.addAttribute("objectifsTotal", objectifs.size());
            model.addAttribute("objectifsJournal", objectifs);
            model.addAttribute("objectifsAtteints", objectifsAtteints);
            model.addAttribute("tachesTerminees", tachesTerminees);
            model.addAttribute("progressionStage", progressionStage);
            model.addAttribute("heuresDeclarees", journaux.stream()
                    .mapToInt(JournalBord::getDureeHeures)
                    .sum());
        } else {
            model.addAttribute("journaux", new ArrayList<>());
            model.addAttribute("journauxRecents", new ArrayList<>());
            model.addAttribute("nombreEntrees", 0);
            model.addAttribute("stage", null);
            model.addAttribute("taches", new ArrayList<>());
            model.addAttribute("livrables", new ArrayList<>());
            model.addAttribute("commentairesEncadreur", new ArrayList<>());
            model.addAttribute("objectifsTotal", 0);
            model.addAttribute("objectifsJournal", new ArrayList<>());
            model.addAttribute("objectifsAtteints", 0);
            model.addAttribute("tachesTerminees", 0);
            model.addAttribute("progressionStage", 0);
            model.addAttribute("heuresDeclarees", 0);
            model.addAttribute("periodeJournal", "semaine");
        }

        if (succes != null) model.addAttribute("succes", succes);
        return "stagiaire/journal";
    }

    @PostMapping("/journal/ajouter")
    public String ajouterJournal(@AuthenticationPrincipal CustomUserDetails userDetails,
                                  @RequestParam String dateActivite,
                                  @RequestParam String travauxRealises,
                                  @RequestParam(defaultValue = "7") Integer duree,
                                  @RequestParam(required = false) String difficultes,
                                  @RequestParam(required = false) String solutions) {
        getStage(userDetails).ifPresent(stage -> {
            JournalBord journal = new JournalBord(stage,
                    LocalDate.parse(dateActivite), travauxRealises);

            journal.setDifficultes(difficultes);
            journal.setObservations(solutions);
            journal.setDureeHeures(duree);
            journalBordRepository.save(journal);
            recalculerProgression(stage);
        });
        return "redirect:/stagiaire/journal?succes=Journal enregistre avec succes.";
    }

    @PostMapping("/livrables/deposer")
    public String deposerLivrable(@AuthenticationPrincipal CustomUserDetails userDetails,
                                  @RequestParam String titre,
                                  @RequestParam(required = false, defaultValue = "Autre") String categorie,
                                  @RequestParam(required = false) String description,
                                  @RequestParam MultipartFile fichier) {
        try {
            Files.createDirectories(Paths.get(DOSSIER_UPLOAD));
            String nomFichier = "livrable_" + userDetails.getUtilisateur().getId() 
                    + "_" + System.currentTimeMillis() + "_" + fichier.getOriginalFilename();
            Path chemin = Paths.get(DOSSIER_UPLOAD + nomFichier);
            Files.write(chemin, fichier.getBytes());

            Optional<Stage> stageOpt = getStage(userDetails);
            if (stageOpt.isPresent()) {
                Stage stage = stageOpt.get();
                Livrable livrable = new Livrable();
                livrable.setTitre(titre);
                livrable.setCategorie(categorie);
                livrable.setDescription(description);
                livrable.setTailleOctets(fichier.getSize());
                livrable.setFichier(chemin.toString());
                livrable.setStage(stage);
                livrable.setStatut(Livrable.StatutLivrable.depose);
                livrable.setDateDepot(java.time.LocalDateTime.now());
                livrableRepository.save(livrable);
                recalculerProgression(stage);
            }
        } catch (IOException e) {
            System.err.println("Erreur upload livrable : " + e.getMessage());
            return "redirect:/stagiaire/livrables?erreur=Erreur lors du depot du document.";
        }
        return "redirect:/stagiaire/livrables?succes=Document depose avec succes.";
    }

@GetMapping("/taches")
public String afficherTaches(@AuthenticationPrincipal CustomUserDetails userDetails,
                              Model model) {
    Optional<Stage> stageOpt = getStage(userDetails);
    List<Tache> taches = new ArrayList<>();

    model.addAttribute("activePage", "taches");
    if (stageOpt.isPresent()) {
        Stage stage = stageOpt.get();
        model.addAttribute("stage", stage);
        taches = tacheRepository.findByStageId(stage.getId());
    } else {
        model.addAttribute("stage", null);
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
    long tachesEnRevue = taches.stream().filter(t -> t.getStatut() == Tache.StatutTache.en_revue).count();
    long tachesTerminees = taches.stream().filter(t -> t.getStatut() == Tache.StatutTache.terminee).count();
    long tachesEnRetard = taches.stream().filter(t -> t.getStatut() == Tache.StatutTache.en_retard).count();
    model.addAttribute("tachesAFaire", tachesAFaire);
    model.addAttribute("tachesEnCours", tachesEnCours);
    model.addAttribute("tachesEnRevue", tachesEnRevue);
    model.addAttribute("tachesTerminees", tachesTerminees);
    model.addAttribute("tachesEnRetard", tachesEnRetard);
    return "stagiaire/taches";

}

@PostMapping("/taches/statut")
public String changerStatutTache(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 @RequestParam Integer id,
                                 @RequestParam Tache.StatutTache statut) {
    Optional<Stage> stageOpt = getStage(userDetails);
    Optional<Tache> tacheOpt = tacheRepository.findById(id);

    if (stageOpt.isPresent()
            && tacheOpt.isPresent()
            && tacheOpt.get().getStage().getId().equals(stageOpt.get().getId())) {
        Tache tache = tacheOpt.get();
        tache.setStatut(statut);
        tacheRepository.save(tache);
        recalculerProgression(stageOpt.get());
    }

    return "redirect:/stagiaire/taches";
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
        Stage stage = stageOpt.get();
        model.addAttribute("stage", stage);
        livrables.addAll(livrableRepository.findByStageId(stage.getId()));
        List<Tache> taches = tacheRepository.findByStageId(stage.getId());
        LocalDate aujourdHui = LocalDate.now();
        model.addAttribute("echeancesLivrables", taches.stream()
                .filter(tache -> tache.getDateLimite() != null
                        && !tache.getDateLimite().isBefore(aujourdHui)
                        && tache.getStatut() != Tache.StatutTache.terminee)
                .sorted(Comparator.comparing(Tache::getDateLimite))
                .limit(3)
                .toList());
        for (Tache t : taches) {
            for (Livrable livrable : livrableRepository.findByTacheId(t.getId())) {
                if (livrables.stream().noneMatch(existing -> existing.getId().equals(livrable.getId()))) {
                    livrables.add(livrable);
                }
            }
        }
    } else {
        model.addAttribute("stage", null);
        model.addAttribute("echeancesLivrables", List.of());
    }
    long valides = livrables.stream().filter(l -> l.getStatut() == Livrable.StatutLivrable.valide).count();
    long attente = livrables.stream().filter(l -> l.getStatut() == Livrable.StatutLivrable.depose).count();
    long refuses = livrables.stream().filter(l -> l.getStatut() == Livrable.StatutLivrable.rejete).count();
    long corrections = livrables.stream().filter(l -> l.getStatut() == Livrable.StatutLivrable.correction_demandee).count();
    int nombreAttendus = stageOpt.map(stage -> Math.max(
            tacheRepository.findByStageId(stage.getId()).size(), livrables.size())).orElse(0);
    model.addAttribute("livrables", livrables);
    model.addAttribute("nombreLivrables", livrables.size());
    model.addAttribute("livrablesValides", valides);
    model.addAttribute("livrablesAttente", attente);
    model.addAttribute("livrablesRefuses", refuses);
    model.addAttribute("livrablesCorrections", corrections);
    model.addAttribute("nombreLivrablesDemandes", nombreAttendus);
    model.addAttribute("livrablesACorriger", livrables.stream()
            .filter(livrable -> livrable.getStatut() == Livrable.StatutLivrable.rejete
                    || livrable.getStatut() == Livrable.StatutLivrable.correction_demandee)
            .toList());
    model.addAttribute("commentairesLivrables", livrables.stream()
            .filter(livrable -> livrable.getCommentaireEncadreur() != null
                    && !livrable.getCommentaireEncadreur().isBlank())
            .toList());
    model.addAttribute("progressionDocumentaire", nombreAttendus == 0
            ? 0 : Math.min(100, livrables.size() * 100 / nombreAttendus));
    return "stagiaire/livrables";
}

@PostMapping("/livrables/{id}/envoyer")
public String envoyerLivrable(@AuthenticationPrincipal CustomUserDetails userDetails,
                              @PathVariable Integer id) {
    Optional<Stage> stageOpt = getStage(userDetails);
    livrableRepository.findById(id)
            .filter(livrable -> stageOpt.isPresent()
                    && ((livrable.getStage() != null
                            && livrable.getStage().getId().equals(stageOpt.get().getId()))
                        || (livrable.getTache() != null
                            && livrable.getTache().getStage().getId().equals(stageOpt.get().getId()))))
            .ifPresent(livrable -> {
                livrable.setStatut(Livrable.StatutLivrable.depose);
                livrableRepository.save(livrable);
                Stage stage = stageOpt.get();
                recalculerProgression(stage);
                if (stage.getEncadreur() != null && stage.getEncadreur().getUtilisateur() != null) {
                    Notification notification = new Notification(
                            "Nouveau livrable",
                            userDetails.getUtilisateur().getPrenom() + " "
                                    + userDetails.getUtilisateur().getNom()
                                    + " a envoyé le document « " + livrable.getTitre() + " ».",
                            "ENCADREUR",
                            "normale",
                            userDetails.getUtilisateur().getPrenom() + " "
                                    + userDetails.getUtilisateur().getNom());
                    notification.setDestinataireEmail(
                            stage.getEncadreur().getUtilisateur().getEmail());
                    notification.setStatut("non_lue");
                    notificationRepository.save(notification);
                }
            });
    return "redirect:/stagiaire/livrables?succes=Livrable envoye a votre encadreur.";
}

@GetMapping("/rapport")
public String afficherRapport(@AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model,
                               @RequestParam(required = false) String succes) {
    
    Optional<Stage> stageOpt = getStage(userDetails);
    model.addAttribute("stage", stageOpt.orElse(null));
    model.addAttribute("rapports", List.of());
    model.addAttribute("dernierRapport", null);
    model.addAttribute("nombreVersions", 0);
    model.addAttribute("joursRapportRestants", 0);
    model.addAttribute("projet", null);
    model.addAttribute("tailleRapports", 0L);
    model.addAttribute("progressionRapport", 0);

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
                .sorted(Comparator.comparing(Document::getDateDepot).reversed())
                .toList();
        
        // Trier manuellement si la méthode existe
        model.addAttribute("rapports", rapports);
        model.addAttribute("tailleRapports", rapports.stream()
                .map(Document::getTailleOctets)
                .filter(java.util.Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum());
        model.addAttribute("progressionRapport",
                Math.min(100, (rapports.isEmpty() ? 0 : 60)
                        + (stage.getProjet() != null ? 40 : 0)));
        
        if (!rapports.isEmpty()) {
            model.addAttribute("dernierRapport", rapports.get(0));
            model.addAttribute("nombreVersions", rapports.size());
        }
        model.addAttribute("joursRapportRestants",
                Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), stage.getDateFin())));
        model.addAttribute("projet", stage.getProjet());
    });
    
    if (succes != null) model.addAttribute("succes", succes);
    return "stagiaire/rapport";
}

@PostMapping("/rapport/deposer")
public String deposerRapport(@AuthenticationPrincipal CustomUserDetails userDetails,
                               @RequestParam String titre,
                               @RequestParam(required = false, defaultValue = "1.0") String version,
                               @RequestParam(required = false) String descriptionModifications,
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
        document.setVersion(version);
        document.setDescriptionModifications(descriptionModifications);
        document.setTailleOctets(fichier.getSize());
        document.setStatut("en_attente");
        documentRepository.save(document);
        recalculerProgression(stageOpt.get());
    } catch (IOException e) {
        System.err.println("Erreur upload rapport : " + e.getMessage());
        return "redirect:/stagiaire/rapport?succes=Erreur lors du depot du rapport.";
    }
    return "redirect:/stagiaire/rapport?succes=Rapport depose avec succes.";
}

@PostMapping("/rapport/projet")
public String enregistrerProjet(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @RequestParam String titre,
                                @RequestParam(required = false) String typeProjet,
                                @RequestParam(required = false) String technologies,
                                @RequestParam(required = false) String lienGithub,
                                @RequestParam(required = false) String lienDemo,
                                @RequestParam(required = false) String description) {
    getStage(userDetails).ifPresent(stage -> {
        Projet projet = stage.getProjet();
        if (projet == null) {
            projet = new Projet();
        }
        projet.setTitre(titre);
        projet.setTypeProjet(typeProjet);
        projet.setTechnologies(technologies);
        projet.setLienGithub(lienGithub);
        projet.setLienDemo(lienDemo);
        projet.setDescription(description);
        projet.setDateDebut(stage.getDateDebut());
        projet.setDateFin(stage.getDateFin());
        projet = projetRepository.save(projet);
        stage.setProjet(projet);
        stageRepository.save(stage);
        recalculerProgression(stage);
    });
    return "redirect:/stagiaire/rapport?succes=Informations du projet enregistrees.";
}

@GetMapping("/profil")
public String afficherProfilStagiaire(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    Utilisateur utilisateur = userDetails.getUtilisateur();
    Stagiaire stagiaire = stagiaireRepository.findByUtilisateurId(utilisateur.getId()).orElse(null);
    Stage stage = getStage(userDetails).orElse(null);

    model.addAttribute("activePage", "profil");
    model.addAttribute("utilisateur", utilisateur);
    model.addAttribute("stagiaire", stagiaire);
    model.addAttribute("stage", stage);

    List<Objectif> objectifs = stage == null
            ? List.of()
            : objectifRepository.findByStageIdOrderByOrdreAsc(stage.getId());
    List<Tache> taches = stage == null ? List.of() : tacheRepository.findByStageId(stage.getId());
    List<Livrable> livrables = stage == null ? List.of() : livrableRepository.findByStageId(stage.getId());
    List<Evaluation> evaluations = stage == null ? List.of() : evaluationRepository.findByStageId(stage.getId());
    List<JournalBord> journaux = stage == null
            ? List.of()
            : journalBordRepository.findByStageIdOrderByDateActiviteDesc(stage.getId());

    long objectifsAtteints = objectifs.stream()
            .filter(objectif -> objectif.getStatut() == Objectif.StatutObjectif.atteint)
            .count();
    long tachesTerminees = taches.stream()
            .filter(tache -> tache.getStatut() == Tache.StatutTache.terminee)
            .count();
    long livrablesValides = livrables.stream()
            .filter(livrable -> livrable.getStatut() == Livrable.StatutLivrable.valide)
            .count();
    int progression = stagiaire != null && stagiaire.getProgression() != null
            ? stagiaire.getProgression()
            : (taches.isEmpty() ? 0 : (int) Math.round(tachesTerminees * 100.0 / taches.size()));

    List<Document> documents = new ArrayList<>();
    if (stagiaire != null && stagiaire.getDemandeStage() != null) {
        documents.addAll(documentRepository.findByDemandeStageId(stagiaire.getDemandeStage().getId()));
    }
    if (stage != null) {
        documentRepository.findByStageId(stage.getId()).stream()
                .filter(document -> documents.stream().noneMatch(existing -> existing.getId().equals(document.getId())))
                .forEach(documents::add);
    }
    documents.sort(Comparator.comparing(Document::getDateDepot).reversed());

    boolean rapportFinalDepose = documents.stream()
            .anyMatch(document -> document.getTypeDocument() != null
                    && document.getTypeDocument().toLowerCase().contains("final"));
    int objectifProgression = objectifs.isEmpty()
            ? 0
            : (int) Math.round(objectifsAtteints * 100.0 / objectifs.size());
    int livrableProgression = livrables.isEmpty()
            ? 0
            : (int) Math.round(livrablesValides * 100.0 / livrables.size());
    int journalProgression = Math.min(100, journaux.size() * 10);

    List<String> competences = new ArrayList<>();
    if (stage != null && stage.getProjet() != null
            && stage.getProjet().getTechnologies() != null
            && !stage.getProjet().getTechnologies().isBlank()) {
        for (String technologie : stage.getProjet().getTechnologies().split("[,;]")) {
            if (!technologie.isBlank()) competences.add(technologie.trim());
        }
    }
    for (String competence : List.of("Communication", "Travail d'équipe", "Résolution de problèmes")) {
        if (competences.stream().noneMatch(item -> item.equalsIgnoreCase(competence))) {
            competences.add(competence);
        }
    }

    List<Map<String, Object>> activites = new ArrayList<>();
    documents.stream().limit(2).forEach(document -> activites.add(activiteProfil(
            document.getTypeDocument() + " envoyé", document.getNomFichier(),
            document.getDateDepot(), "file-up", "blue")));
    objectifs.stream()
            .filter(objectif -> objectif.getStatut() == Objectif.StatutObjectif.atteint)
            .sorted(Comparator.comparing(Objectif::getDateCreation,
                    Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(2)
            .forEach(objectif -> activites.add(activiteProfil(
                    "Objectif terminé", objectif.getLibelle(),
                    objectif.getDateCreation(), "target", "green")));
    taches.stream()
            .sorted(Comparator.comparing(Tache::getDateCreation,
                    Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(2)
            .forEach(tache -> activites.add(activiteProfil(
                    "Tâche attribuée", tache.getTitre(),
                    tache.getDateCreation(), "list-checks", "violet")));
    livrables.stream()
            .sorted(Comparator.comparing(Livrable::getDateDepot,
                    Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(2)
            .forEach(livrable -> activites.add(activiteProfil(
                    livrable.getStatut() == Livrable.StatutLivrable.valide
                            ? "Livrable validé" : "Livrable déposé",
                    livrable.getTitre(), livrable.getDateDepot(), "package-check", "amber")));
    activites.sort((a, b) -> {
        LocalDateTime dateA = (LocalDateTime) a.get("date");
        LocalDateTime dateB = (LocalDateTime) b.get("date");
        return Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder()).compare(dateA, dateB);
    });

    model.addAttribute("objectifs", objectifs);
    model.addAttribute("objectifsAtteints", objectifsAtteints);
    model.addAttribute("tachesTerminees", tachesTerminees);
    model.addAttribute("livrablesValides", livrablesValides);
    model.addAttribute("evaluationsCount", evaluations.size());
    model.addAttribute("progressionProfil", Math.max(0, Math.min(100, progression)));
    model.addAttribute("objectifProgression", objectifProgression);
    model.addAttribute("livrableProgression", livrableProgression);
    model.addAttribute("journalProgression", journalProgression);
    model.addAttribute("rapportProgression", rapportFinalDepose ? 100 : 0);
    model.addAttribute("presenceProgression", stage != null && stage.getStatut() == Stage.StatutStage.en_cours ? 100 : 0);
    model.addAttribute("documentsProfil", documents);
    model.addAttribute("competencesProfil", competences);
    model.addAttribute("activitesProfil", activites.stream().limit(5).toList());
    return "stagiaire/profil";
}

private Map<String, Object> activiteProfil(String titre,
                                           String description,
                                           LocalDateTime date,
                                           String icone,
                                           String couleur) {
    Map<String, Object> activite = new LinkedHashMap<>();
    activite.put("titre", titre);
    activite.put("description", description);
    activite.put("date", date);
    activite.put("icone", icone);
    activite.put("couleur", couleur);
    return activite;
}



@GetMapping("/objectifs")
public String afficherObjectifsStagiaire(@AuthenticationPrincipal CustomUserDetails userDetails,
                                         @RequestParam(defaultValue = "7") int periode,
                                         Model model) {
    model.addAttribute("activePage", "objectifs");
    Optional<Stage> stageOpt = getStage(userDetails);
    Stage stage = stageOpt.orElse(null);
    model.addAttribute("stage", stage);
    model.addAttribute("activePage", "objectifs");

    String prenom = userDetails.getUtilisateur().getPrenom();
    String nom = userDetails.getUtilisateur().getNom();
    model.addAttribute("prenom", prenom);
    model.addAttribute("nomComplet", prenom + " " + nom);
    model.addAttribute("initiales", prenom.substring(0,1).toUpperCase()
            + nom.substring(0,1).toUpperCase());

    List<Objectif> objectifs = stageOpt.isPresent()
            ? objectifRepository.findByStageIdOrderByOrdreAsc(stageOpt.get().getId())
            : new ArrayList<>();

    model.addAttribute("objectifs", objectifs);
    long total = objectifs.size();
    long atteints = objectifs.stream().filter(o -> o.getStatut() == Objectif.StatutObjectif.atteint).count();
    long enCours = objectifs.stream().filter(o -> o.getStatut() == Objectif.StatutObjectif.en_cours).count();
    long enAttente = objectifs.stream().filter(o -> o.getStatut() == Objectif.StatutObjectif.non_commence).count();

    model.addAttribute("nombreObjectifs", total);
    model.addAttribute("objectifsAtteints", atteints);
    model.addAttribute("objectifsEnCours", enCours);
    model.addAttribute("objectifsNonCommences", enAttente);
    model.addAttribute("objectifsPrioritaires", objectifs.stream()
            .filter(o -> o.getPriorite() == Objectif.Priorite.haute)
            .count());

    double progressionMoyenne = objectifs.stream().mapToInt(Objectif::getProgression).average().orElse(0);
    model.addAttribute("progressionMoyenne", (int) Math.round(progressionMoyenne));
    int periodeValide = periode == 30 ? 30 : 7;
    LocalDateTime limitePeriode = LocalDateTime.now().minusDays(periodeValide);
    List<Objectif> objectifsPeriode = objectifs.stream()
            .filter(objectif -> objectif.getDateCreation() == null
                    || !objectif.getDateCreation().isBefore(limitePeriode))
            .toList();
    int progressionPeriode = (int) Math.round(objectifsPeriode.stream()
            .mapToInt(Objectif::getProgression)
            .average()
            .orElse(0));
    model.addAttribute("periodeObjectifs", periodeValide);
    model.addAttribute("progressionPeriode", progressionPeriode);
    model.addAttribute("activitesObjectifs", objectifs.stream()
            .sorted(Comparator.comparing(
                    Objectif::getDateCreation,
                    Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(5)
            .toList());

    LocalDate aujourdHui = LocalDate.now();
    List<Objectif> echeancesProches = objectifs.stream()
            .filter(o -> o.getDateLimite() != null
                    && !o.getDateLimite().isBefore(aujourdHui)
                    && o.getDateLimite().isBefore(aujourdHui.plusDays(7))
                    && o.getStatut() != Objectif.StatutObjectif.atteint)
            .toList();
    model.addAttribute("echeancesProches", echeancesProches);

    if (stage != null && stage.getEncadreur() != null) {
        model.addAttribute("encadreurNom", stage.getEncadreur().getUtilisateur().getPrenom()
                + " " + stage.getEncadreur().getUtilisateur().getNom());
        model.addAttribute("serviceNom", stage.getService().getNom());
    }

    return "stagiaire/objectifs";
}

@PostMapping("/objectifs/creer")
public String creerObjectif(@AuthenticationPrincipal CustomUserDetails userDetails,
                            @RequestParam String libelle,
                            @RequestParam(required = false) String description,
                            @RequestParam(defaultValue = "moyenne") String priorite,
                            @RequestParam(required = false) String dateLimite) {
    Optional<Stage> stageOpt = getStage(userDetails);
    if (stageOpt.isPresent()) {
        Objectif obj = new Objectif();
        obj.setStage(stageOpt.get());
        obj.setLibelle(libelle);
        obj.setDescription(description);
        obj.setPriorite(Objectif.Priorite.valueOf(priorite));
        if (dateLimite != null && !dateLimite.isEmpty())
            obj.setDateLimite(LocalDate.parse(dateLimite));
        int prochainOrdre = objectifRepository.findByStageIdOrderByOrdreAsc(stageOpt.get().getId())
                .stream()
                .mapToInt(Objectif::getOrdre)
                .max()
                .orElse(0) + 1;
        obj.setOrdre(prochainOrdre);
        obj.setProgression(0);
        obj.setStatut(Objectif.StatutObjectif.non_commence);
        obj.setOrigine(Objectif.OrigineObjectif.stagiaire);
        obj.setDateCreation(LocalDateTime.now());
        objectifRepository.save(obj);
        recalculerProgression(stageOpt.get());
    }
    return "redirect:/stagiaire/objectifs";
}

@PostMapping("/objectifs/statut")
public String changerStatutObjectif(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @RequestParam Integer id,
                                     @RequestParam String statut) {
    Optional<Stage> stageOpt = getStage(userDetails);
    Objectif obj = objectifRepository.findById(id).orElse(null);
    if (stageOpt.isPresent()
            && obj != null
            && obj.getStage().getId().equals(stageOpt.get().getId())) {
        obj.setStatut(Objectif.StatutObjectif.valueOf(statut));
        if (statut.equals("atteint")) {
            obj.setProgression(100);
        } else if (statut.equals("non_commence")) {
            obj.setProgression(0);
        } else if (obj.getProgression() == 0) {
            obj.setProgression(10);
        }
        objectifRepository.save(obj);
        recalculerProgression(stageOpt.get());
    }
   return "redirect:/stagiaire/objectifs";
}

@PostMapping("/objectifs/priorite")
public String basculerPrioriteObjectif(@AuthenticationPrincipal CustomUserDetails userDetails,
                                       @RequestParam Integer id) {
   Optional<Stage> stageOpt = getStage(userDetails);
   objectifRepository.findById(id)
           .filter(objectif -> stageOpt.isPresent()
                   && objectif.getStage().getId().equals(stageOpt.get().getId()))
           .ifPresent(objectif -> {
               objectif.setPriorite(objectif.getPriorite() == Objectif.Priorite.haute
                       ? Objectif.Priorite.moyenne
                       : Objectif.Priorite.haute);
               objectifRepository.save(objectif);
               recalculerProgression(stageOpt.get());
           });
   return "redirect:/stagiaire/objectifs";
}

@PostMapping("/objectifs/supprimer")
public String supprimerObjectif(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @RequestParam Integer id) {
    Optional<Stage> stageOpt = getStage(userDetails);
    objectifRepository.findById(id)
            .filter(objectif -> stageOpt.isPresent()
                    && objectif.getStage().getId().equals(stageOpt.get().getId()))
            .ifPresent(objectif -> {
                objectifRepository.delete(objectif);
                recalculerProgression(stageOpt.get());
            });
    return "redirect:/stagiaire/objectifs";
}

@GetMapping("/planning")
public String afficherPlanningStagiaire(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    model.addAttribute("activePage", "planning");
    String prenom = userDetails.getUtilisateur().getPrenom();
    String nom = userDetails.getUtilisateur().getNom();
    model.addAttribute("prenom", prenom);
    model.addAttribute("nomComplet", prenom + " " + nom);
    model.addAttribute("initiales", prenom.substring(0, 1).toUpperCase() + nom.substring(0, 1).toUpperCase());
    Optional<Stage> stageOpt = getStage(userDetails);
    model.addAttribute("stage", stageOpt.orElse(null));

    List<EvenementPlanning> evenements = new ArrayList<>();

        if (stageOpt.isPresent()) {
            Stage stage = stageOpt.get();

            List<Tache> taches = tacheRepository.findByStageId(stage.getId());
            model.addAttribute("nombreTachesPlanning", taches.size());
            for (Tache t : taches) {
                if (t.getDateLimite() != null) {
                    evenements.add(new EvenementPlanning(
                            t.getDateLimite(), "Echeance : " + t.getTitre(), "tache"));
                }
            }

            List<Objectif> objectifs = objectifRepository.findByStageIdOrderByOrdreAsc(stage.getId());
            for (Objectif objectif : objectifs) {
                if (objectif.getDateLimite() != null) {
                    evenements.add(new EvenementPlanning(
                            objectif.getDateLimite(), "Objectif : " + objectif.getLibelle(), "important"));
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

            List<EvenementPersonnel> evenementsPerso = evenementPersonnelRepository.findByStageId(stage.getId());
            for (EvenementPersonnel ep : evenementsPerso) {
                evenements.add(new EvenementPlanning(
                        ep.getDate(), ep.getMotif(), ep.getTypeCouleur(), ep.getHeure(), ep.getDescription()));
            }

            model.addAttribute("joursStageRestants",
                    Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), stage.getDateFin())));
            model.addAttribute("nombreEvenementsPersonnels", evenementsPerso.size());
            model.addAttribute("nombreLivrablesPlanning", livrableRepository.findByStageId(stage.getId()).size());
            model.addAttribute("activitesPlanning", evenementsPerso.stream()
                    .sorted(Comparator.comparing(
                            EvenementPersonnel::getDate,
                            Comparator.reverseOrder())
                            .thenComparing(
                                    EvenementPersonnel::getHeure,
                                    Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(5)
                    .toList());
        }
        if (stageOpt.isEmpty()) {
            model.addAttribute("activitesPlanning", List.of());
        }

    evenements.sort(Comparator.comparing(EvenementPlanning::getDate));
    model.addAttribute("evenements", evenements);

    LocalDate aujourdHui = LocalDate.now();
    model.addAttribute("evenementsAvenir", evenements.stream()
            .filter(e -> !e.getDate().isBefore(aujourdHui))
            .toList());
    model.addAttribute("evenementsCeMois", evenements.stream()
            .filter(e -> e.getDate().getYear() == aujourdHui.getYear()
                    && e.getDate().getMonth() == aujourdHui.getMonth())
            .count());

    return "stagiaire/planning";
}

@PostMapping("/planning/ajouter")
public String ajouterEvenementPlanning(@AuthenticationPrincipal CustomUserDetails userDetails,
                                         @RequestParam String motif,
                                         @RequestParam String date,
                                         @RequestParam String typeCouleur,
                                         @RequestParam(required = false) String heure,
                                         @RequestParam(required = false) String description,
                                         @RequestParam(required = false, defaultValue = "moyenne") String priorite,
                                         @RequestParam(required = false, defaultValue = "1_heure") String rappel) {
    getStage(userDetails).ifPresent(stage -> {
        EvenementPersonnel evt = new EvenementPersonnel(stage, motif, LocalDate.parse(date), typeCouleur);
        if (heure != null && !heure.isBlank()) {
            evt.setHeure(LocalTime.parse(heure));
        }
        evt.setDescription(description);
        evt.setPriorite(priorite);
        evt.setRappel(rappel);
        evenementPersonnelRepository.save(evt);
    });
    return "redirect:/stagiaire/planning?succes=Evenement ajoute avec succes.";
}

@GetMapping("/messages")
public String afficherMessages(@AuthenticationPrincipal CustomUserDetails userDetails,
                               @RequestParam(required = false) Integer convId, Model model) {
    model.addAttribute("activePage", "messages");
    model.addAttribute("prenom", userDetails.getUtilisateur().getPrenom());
    model.addAttribute("nomComplet", userDetails.getUtilisateur().getPrenom() + " " + userDetails.getUtilisateur().getNom());
    model.addAttribute("initiales", userDetails.getUtilisateur().getPrenom().substring(0,1).toUpperCase()
            + userDetails.getUtilisateur().getNom().substring(0,1).toUpperCase());
    messagingService.preparerModele(
            model, userDetails.getUtilisateur(), convId, "/stagiaire/messages", "Stagiaire");
    return "stagiaire/messages";
}

@PostMapping("/messages/nouveau")
public String nouvelleConversation(@AuthenticationPrincipal CustomUserDetails userDetails,
                                   @RequestParam Integer destinataireId,
                                   @RequestParam String message) {
    Utilisateur currentUser = userDetails.getUtilisateur();
    Utilisateur destinataire = utilisateurRepository.findById(destinataireId).orElse(null);
    if (destinataire == null) return "redirect:/stagiaire/messages";
    List<Conversation> existing = conversationRepository.findByParticipantIdOrderByDernierMessageDesc(currentUser.getId());
    for (Conversation c : existing) {
        boolean hasDest = c.getParticipants().stream().anyMatch(p -> p.getId().equals(destinataireId));
        if (hasDest) {
            Message msg = new Message();
            msg.setConversation(c);
            msg.setExpediteur(currentUser);
            msg.setContenu(message);
            msg.setDateEnvoi(java.time.LocalDateTime.now());
            msg.setLu(false);
            c.setDernierMessage(java.time.LocalDateTime.now());
            conversationRepository.save(c);
            messageRepository.save(msg);
            return "redirect:/stagiaire/messages?convId=" + c.getId();
        }
    }
    Conversation conv = new Conversation();
    conv.setSujet("Discussion avec " + destinataire.getPrenom() + " " + destinataire.getNom());
    conv.setDateCreation(java.time.LocalDateTime.now());
    conv.setDernierMessage(java.time.LocalDateTime.now());
    conv.getParticipants().add(currentUser);
    conv.getParticipants().add(destinataire);
    conv = conversationRepository.save(conv);
    Message msg = new Message();
    msg.setConversation(conv);
    msg.setExpediteur(currentUser);
    msg.setContenu(message);
    msg.setDateEnvoi(java.time.LocalDateTime.now());
    msg.setLu(false);
    messageRepository.save(msg);
    return "redirect:/stagiaire/messages?convId=" + conv.getId();
}

@PostMapping("/messages/envoyer")
public String envoyerMessage(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @RequestParam Integer convId,
                             @RequestParam String contenu) {
    Conversation conv = conversationRepository.findById(convId)
            .filter(conversation -> conversation.getParticipants().stream()
                    .anyMatch(participant -> participant.getId()
                            .equals(userDetails.getUtilisateur().getId())))
            .orElse(null);
    if (conv == null) return "redirect:/stagiaire/messages";
    Message msg = new Message();
    msg.setConversation(conv);
    msg.setExpediteur(userDetails.getUtilisateur());
    msg.setContenu(contenu);
    msg.setDateEnvoi(java.time.LocalDateTime.now());
    msg.setLu(false);
    conv.setDernierMessage(java.time.LocalDateTime.now());
    conversationRepository.save(conv);
    messageRepository.save(msg);
    return "redirect:/stagiaire/messages?convId=" + convId;
}

@PostMapping("/notifications/lire")
public String marquerNotificationsLues(@AuthenticationPrincipal CustomUserDetails userDetails) {
    List<Notification> notifications = notificationRepository
            .findByDestinataireEmailAndStatutNot(
                    userDetails.getUtilisateur().getEmail(), "lue");
    notifications.forEach(notification -> notification.setStatut("lue"));
    notificationRepository.saveAll(notifications);
    return "redirect:/stagiaire/dashboard";
}

@GetMapping("/preferences")
@ResponseBody
public Map<String, Object> preferences(@AuthenticationPrincipal CustomUserDetails userDetails) {
    return stagiaireRepository.findByUtilisateurId(userDetails.getUtilisateur().getId())
            .map(stagiaire -> Map.<String, Object>of(
                    "email", stagiaire.getNotificationsEmail(),
                    "system", stagiaire.getNotificationsSysteme(),
                    "tasks", stagiaire.getRappelTaches(),
                    "dark", stagiaire.getModeSombre(),
                    "language", stagiaire.getLangue()))
            .orElseGet(() -> Map.of(
                    "email", true,
                    "system", true,
                    "tasks", true,
                    "dark", false,
                    "language", "fr"));
}

@PostMapping("/preferences")
@ResponseBody
public Map<String, Object> enregistrerPreferences(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody Map<String, Object> preferences) {
    Stagiaire stagiaire = stagiaireRepository.findByUtilisateurId(userDetails.getUtilisateur().getId())
            .orElseThrow();
    if (preferences.containsKey("email")) {
        stagiaire.setNotificationsEmail(Boolean.TRUE.equals(preferences.get("email")));
    }
    if (preferences.containsKey("system")) {
        stagiaire.setNotificationsSysteme(Boolean.TRUE.equals(preferences.get("system")));
    }
    if (preferences.containsKey("tasks")) {
        stagiaire.setRappelTaches(Boolean.TRUE.equals(preferences.get("tasks")));
    }
    if (preferences.containsKey("dark")) {
        stagiaire.setModeSombre(Boolean.TRUE.equals(preferences.get("dark")));
    }
    if (preferences.containsKey("language")) {
        stagiaire.setLangue(String.valueOf(preferences.get("language")));
    }
    stagiaireRepository.save(stagiaire);
    return preferences(userDetails);
}
@PostMapping("/profil/modifier")
public String modifierProfil(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @RequestParam String prenom,
                             @RequestParam String nom,
                             @RequestParam String email,
                             @RequestParam(required = false) String telephone,
                             @RequestParam(required = false) String adresse,
                             @RequestParam(required = false) String ville,
                             @RequestParam(required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateNaissance,
                             @RequestParam(required = false) String ecole,
                             @RequestParam(required = false) String formation,
                             @RequestParam(required = false) String niveau,
                             @RequestParam(required = false) String specialite) {
    Utilisateur u = userDetails.getUtilisateur();
    u.setPrenom(prenom);
    u.setNom(nom);
    u.setEmail(email);
    u.setTelephone(telephone);
    u.setAdresse(adresse);
    u.setVille(ville);
    u.setDateNaissance(dateNaissance);
    utilisateurRepository.save(u);
    stagiaireRepository.findByUtilisateurId(u.getId()).ifPresent(stagiaire -> {
        stagiaire.setSpecialite(specialite);
        if (stagiaire.getDemandeStage() != null) {
            if (ecole != null && !ecole.isBlank()) stagiaire.getDemandeStage().setEcole(ecole.trim());
            if (formation != null && !formation.isBlank()) stagiaire.getDemandeStage().setFiliere(formation.trim());
            if (niveau != null && !niveau.isBlank()) stagiaire.getDemandeStage().setNiveau(niveau.trim());
        }
        stagiaireRepository.save(stagiaire);
    });
    return "redirect:/stagiaire/profil?succes=Profil modifie avec succes.";
}

@PostMapping("/profil/photo")
public String modifierPhoto(@AuthenticationPrincipal CustomUserDetails userDetails,
                            @RequestParam("photo") MultipartFile photo) {
    if (photo == null || photo.isEmpty()) {
        return "redirect:/stagiaire/profil?erreur=Veuillez sélectionner une image.";
    }
    String type = photo.getContentType();
    String extension = switch (type == null ? "" : type.toLowerCase(Locale.ROOT)) {
        case "image/jpeg" -> ".jpg";
        case "image/png" -> ".png";
        case "image/webp" -> ".webp";
        default -> null;
    };
    if (extension == null) {
        return "redirect:/stagiaire/profil?erreur=Formats acceptés : JPG, PNG ou WebP.";
    }
    if (photo.getSize() > 5 * 1024 * 1024) {
        return "redirect:/stagiaire/profil?erreur=La photo ne doit pas dépasser 5 Mo.";
    }
    try {
        Path dossier = Paths.get(DOSSIER_UPLOAD, "profils").toAbsolutePath().normalize();
        Files.createDirectories(dossier);
        String nomFichier = "stagiaire_" + userDetails.getUtilisateur().getId()
                + "_" + UUID.randomUUID() + extension;
        Path destination = dossier.resolve(nomFichier).normalize();
        if (!destination.startsWith(dossier)) {
            return "redirect:/stagiaire/profil?erreur=Nom de fichier invalide.";
        }
        Files.copy(photo.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        Utilisateur utilisateur = userDetails.getUtilisateur();
        utilisateur.setPhoto("profils/" + nomFichier);
        utilisateurRepository.save(utilisateur);
        return "redirect:/stagiaire/profil?succes=Photo de profil mise à jour.";
    } catch (IOException exception) {
        return "redirect:/stagiaire/profil?erreur=Impossible d'enregistrer la photo.";
    }
}

@PostMapping("/profil/mot-de-passe")
public String modifierMotDePasse(@AuthenticationPrincipal CustomUserDetails userDetails,
                                  @RequestParam String ancienMotDePasse,
                                  @RequestParam String nouveauMotDePasse,
                                  @RequestParam String confirmerMotDePasse) {
    Utilisateur u = userDetails.getUtilisateur();
    if (!passwordEncoder.matches(ancienMotDePasse, u.getMotDePasse())) {
        return "redirect:/stagiaire/profil?erreur=Ancien mot de passe incorrect.";
    }
    if (!nouveauMotDePasse.equals(confirmerMotDePasse)) {
        return "redirect:/stagiaire/profil?erreur=Les nouveaux mots de passe ne correspondent pas.";
    }
    u.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
    utilisateurRepository.save(u);
    return "redirect:/stagiaire/profil?succes=Mot de passe modifie avec succes.";
}

    @PostMapping("/taches/{id}/statut")
    public String changerStatutTacheDepuisDetail(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                 @PathVariable Integer id,
                                                 @RequestParam Tache.StatutTache statut) {
        Optional<Stage> stageOpt = getStage(userDetails);
        tacheRepository.findById(id)
                .filter(tache -> stageOpt.isPresent()
                        && tache.getStage().getId().equals(stageOpt.get().getId()))
                .ifPresent(tache -> {
                    tache.setStatut(statut);
                    tacheRepository.save(tache);
                    recalculerProgression(stageOpt.get());
                });
        return "redirect:/stagiaire/taches";
    }

@PostMapping("/taches/ajouter")
public String ajouterTache(@AuthenticationPrincipal CustomUserDetails userDetails,
                           @RequestParam String titre,
                           @RequestParam(required = false) String description,
                           @RequestParam(required = false) String dateLimite,
                           @RequestParam(defaultValue = "a_faire") Tache.StatutTache statut) {
   getStage(userDetails).ifPresent(stage -> {
       Tache tache = new Tache();
       tache.setTitre(titre.trim());
       tache.setDescription(description);
       tache.setDateLimite(dateLimite == null || dateLimite.isBlank()
               ? LocalDate.now().plusDays(7)
               : LocalDate.parse(dateLimite));
       tache.setStatut(statut);
       tache.setStage(stage);
       if (!tache.getTitre().isBlank()) {
           tacheRepository.save(tache);
           recalculerProgression(stage);
       }
   });
   return "redirect:/stagiaire/taches?succes=Tache cree avec succes.";
}

@PostMapping("/taches/modifier")
public String modifierTache(@AuthenticationPrincipal CustomUserDetails userDetails,
                            @RequestParam Integer id,
                            @RequestParam String titre,
                            @RequestParam(required = false) String description,
                            @RequestParam(required = false) String dateLimite) {
    Optional<Stage> stageOpt = getStage(userDetails);
    tacheRepository.findById(id)
            .filter(tache -> stageOpt.isPresent()
                    && tache.getStage().getId().equals(stageOpt.get().getId()))
            .ifPresent(tache -> {
                if (titre != null && !titre.isBlank()) {
                    tache.setTitre(titre.trim());
                    tache.setDescription(description);
                    if (dateLimite != null && !dateLimite.isBlank()) {
                        tache.setDateLimite(LocalDate.parse(dateLimite));
                    }
                    tacheRepository.save(tache);
                    recalculerProgression(stageOpt.get());
                }
            });
    return "redirect:/stagiaire/taches?succes=Tache modifiee avec succes.";
}

@PostMapping("/taches/supprimer")
public String supprimerTache(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @RequestParam Integer id) {
    Optional<Stage> stageOpt = getStage(userDetails);
    tacheRepository.findById(id)
            .filter(tache -> stageOpt.isPresent()
                    && tache.getStage().getId().equals(stageOpt.get().getId()))
            .ifPresent(tache -> {
                tacheRepository.delete(tache);
                recalculerProgression(stageOpt.get());
            });
    return "redirect:/stagiaire/taches?succes=Tache supprimee avec succes.";
}
}
