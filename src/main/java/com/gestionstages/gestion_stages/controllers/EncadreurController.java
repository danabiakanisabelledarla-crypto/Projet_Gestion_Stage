package com.gestionstages.gestion_stages.controllers;
import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;

import com.gestionstages.gestion_stages.entities.*;
import com.gestionstages.gestion_stages.repositories.*;
import com.gestionstages.gestion_stages.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.processing.Generated;

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
    private final StagiaireRepository stagiaireRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EvenementPersonnelRepository evenementPersonnelRepository;
    private final JournalBordRepository journalBordRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public EncadreurController(EncadreurRepository encadreurRepository,
                           StageRepository stageRepository,
                           TacheRepository tacheRepository,
                           LivrableRepository livrableRepository,
                           ObjectifRepository objectifRepository,
                           CritereEvaluationRepository critereRepository,
                           EvaluationRepository evaluationRepository,
                           NoteEvaluationRepository noteRepository,
                           StagiaireRepository stagiaireRepository,
                           UtilisateurRepository utilisateurRepository,
                           EvenementPersonnelRepository evenementPersonnelRepository,
                           JournalBordRepository journalBordRepository,
                           ConversationRepository conversationRepository,
                           MessageRepository messageRepository) {
            this.encadreurRepository = encadreurRepository;
            this.stageRepository = stageRepository;
            this.tacheRepository = tacheRepository;
            this.livrableRepository = livrableRepository;
            this.critereRepository = critereRepository;
            this.evaluationRepository = evaluationRepository;
            this.noteRepository = noteRepository;
            this.objectifRepository = objectifRepository;
            this.stagiaireRepository = stagiaireRepository;
            this.utilisateurRepository = utilisateurRepository;
            this.evenementPersonnelRepository = evenementPersonnelRepository;
            this.journalBordRepository = journalBordRepository;
            this.conversationRepository = conversationRepository;
            this.messageRepository = messageRepository;
}

    private Encadreur getEncadreur(CustomUserDetails userDetails) {
        return encadreurRepository
                .findByUtilisateurId(userDetails.getUtilisateur().getId())
                .orElse(null);
    }

    private List<Stage> getStages(CustomUserDetails userDetails) {
        Encadreur encadreur = getEncadreur(userDetails);
        return encadreur == null ? new ArrayList<>() : stageRepository.findByEncadreurId(encadreur.getId());
    }

    private void ajouterIdentite(Model model, CustomUserDetails userDetails, String activePage) {
        Utilisateur utilisateur = userDetails.getUtilisateur();
        List<Stage> stages = getStages(userDetails);
        long notifications = livrablesDesStages(stages).stream()
                .filter(livrable -> livrable.getStatut() == Livrable.StatutLivrable.depose)
                .count();
        long messages = conversationRepository.findByParticipantIdOrderByDernierMessageDesc(utilisateur.getId()).stream()
                .mapToLong(conversation -> conversationRepository.countNonLuByConversation(conversation.getId(), utilisateur.getId()))
                .sum();
        model.addAttribute("activePage", activePage);
        model.addAttribute("nomComplet", utilisateur.getPrenom() + " " + utilisateur.getNom());
        model.addAttribute("initiales",
                utilisateur.getPrenom().substring(0, 1).toUpperCase()
                        + utilisateur.getNom().substring(0, 1).toUpperCase());
        model.addAttribute("notificationsCount", notifications);
        model.addAttribute("messagesCount", messages);
    }

    private List<Tache> tachesDesStages(List<Stage> stages) {
        return stages.stream()
                .flatMap(stage -> tacheRepository.findByStageId(stage.getId()).stream())
                .toList();
    }

    private List<Objectif> objectifsDesStages(List<Stage> stages) {
        return stages.stream()
                .flatMap(stage -> objectifRepository.findByStageIdOrderByOrdreAsc(stage.getId()).stream())
                .toList();
    }

    private List<Livrable> livrablesDesStages(List<Stage> stages) {
        List<Livrable> resultat = new ArrayList<>();
        for (Stage stage : stages) {
            resultat.addAll(livrableRepository.findByStageId(stage.getId()));
            for (Tache tache : tacheRepository.findByStageId(stage.getId())) {
                for (Livrable livrable : livrableRepository.findByTacheId(tache.getId())) {
                    if (resultat.stream().noneMatch(item -> item.getId().equals(livrable.getId()))) {
                        resultat.add(livrable);
                    }
                }
            }
        }
        return resultat;
    }

    private List<Evaluation> evaluationsDesStages(List<Stage> stages) {
        return stages.stream()
                .flatMap(stage -> evaluationRepository.findByStageId(stage.getId()).stream())
                .toList();
    }

    private boolean stageAppartient(CustomUserDetails userDetails, Stage stage) {
        Encadreur encadreur = getEncadreur(userDetails);
        return encadreur != null && stage != null
                && stage.getEncadreur().getId().equals(encadreur.getId());
    }

    private Stage stageDuLivrable(Livrable livrable) {
        if (livrable == null) return null;
        if (livrable.getStage() != null) return livrable.getStage();
        return livrable.getTache() != null ? livrable.getTache().getStage() : null;
    }

    @GetMapping("/mes-stagiaires")
    public String mesStagiaires(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        ajouterIdentite(model, userDetails, "mes-stagiaires");
        Encadreur encadreur = getEncadreur(userDetails);
        if (encadreur == null) {
            return "redirect:/login";
        }

        // Récupérer les stages de cet encadreur
        List<Stage> stages = stageRepository.findByEncadreurId(encadreur.getId());
        
        // Récupérer les stagiaires associés à ces stages
        List<Map<String, Object>> stagiairesData = new ArrayList<>();
        long stagesActifs = 0;
        long evaluationsCompletes = 0;

        for (Stage stage : stages) {
            if (stage.getStagiaire() != null) {
                Stagiaire stagiaire = stage.getStagiaire();
                Map<String, Object> data = new HashMap<>();
                data.put("id", stagiaire.getId());
                data.put("utilisateur", stagiaire.getUtilisateur());
                data.put("matricule", stagiaire.getMatricule());
                data.put("demandeStage", stagiaire.getDemandeStage());
                data.put("stage", stage);
                List<Tache> taches = tacheRepository.findByStageId(stage.getId());
                List<Objectif> objectifs = objectifRepository.findByStageIdOrderByOrdreAsc(stage.getId());
                List<Livrable> livrables = livrablesDesStages(List.of(stage));
                List<JournalBord> journaux = journalBordRepository.findByStageIdOrderByDateActiviteDesc(stage.getId());
                data.put("nombreObjectifs", objectifs.size());
                data.put("nombreTaches", taches.size());
                data.put("nombreLivrables", livrables.size());
                data.put("derniereActivite", journaux.isEmpty() ? null : journaux.get(0).getDateActivite());
                
                // Calculer la progression
                if (stage.getDateDebut() != null && stage.getDateFin() != null) {
                    long total = java.time.temporal.ChronoUnit.DAYS.between(stage.getDateDebut(), stage.getDateFin());
                    long ecoule = java.time.temporal.ChronoUnit.DAYS.between(stage.getDateDebut(), java.time.LocalDate.now());
                    int progression = total > 0 ? (int) Math.min(100, Math.max(0, ecoule * 100 / total)) : 0;
                    data.put("progression", progression);
                } else {
                    data.put("progression", 0);
                }

                stagiairesData.add(data);

                if (stage.getStatut() == Stage.StatutStage.en_cours) {
                    stagesActifs++;
                }

                // Compter les évaluations complètes
                long evalsCount = evaluationRepository.findByStageId(stage.getId()).stream()
                    .filter(e -> e.getTypeEvaluation() == Evaluation.TypeEvaluation.finale)
                    .count();
                if (evalsCount > 0) {
                    evaluationsCompletes++;
                }
            }
        }

        model.addAttribute("stagiaires", stagiairesData);
        model.addAttribute("totalStagiaires", stagiairesData.size());
        model.addAttribute("stagesActifs", stagesActifs);
        model.addAttribute("evaluationsCompletes", evaluationsCompletes);
        List<Tache> toutesLesTaches = tachesDesStages(stages);
        List<Objectif> tousLesObjectifs = objectifsDesStages(stages);
        List<Livrable> tousLesLivrables = livrablesDesStages(stages);
        List<Evaluation> toutesLesEvaluations = evaluationsDesStages(stages);
        int progressionMoyenne = stagiairesData.isEmpty() ? 0 : (int) Math.round(stagiairesData.stream()
                .mapToInt(data -> (Integer) data.get("progression")).average().orElse(0));
        model.addAttribute("objectifsActifs", tousLesObjectifs.stream()
                .filter(o -> o.getStatut() != Objectif.StatutObjectif.atteint).count());
        model.addAttribute("livrablesAttente", tousLesLivrables.stream()
                .filter(l -> l.getStatut() == Livrable.StatutLivrable.depose).count());
        model.addAttribute("evaluationsRealisees", toutesLesEvaluations.size());
        model.addAttribute("progressionMoyenne", progressionMoyenne);
        model.addAttribute("tachesTerminees", toutesLesTaches.stream()
                .filter(t -> t.getStatut() == Tache.StatutTache.terminee).count());
        model.addAttribute("tachesEnCours", toutesLesTaches.stream()
                .filter(t -> t.getStatut() == Tache.StatutTache.en_cours).count());
        model.addAttribute("tachesRetard", toutesLesTaches.stream()
                .filter(t -> t.getStatut() == Tache.StatutTache.en_retard).count());
        model.addAttribute("livrablesRecents", tousLesLivrables.stream()
                .sorted(Comparator.comparing(Livrable::getDateDepot).reversed()).limit(5).toList());
        model.addAttribute("stagiaireSelectionne", stagiairesData.isEmpty() ? null : stagiairesData.get(0));

        return "encadreur/mes-stagiaires";
    }

    @GetMapping("/dashboard")
    public String afficherDashboard(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     Model model) {
        ajouterIdentite(model, userDetails, "dashboard");
        List<Stage> stages = getStages(userDetails);
        List<Tache> taches = tachesDesStages(stages);
        List<Objectif> objectifs = objectifsDesStages(stages);
        List<Livrable> livrables = livrablesDesStages(stages);
        List<Evaluation> evaluations = evaluationsDesStages(stages);
        List<EvenementPersonnel> evenements = stages.stream()
                .flatMap(stage -> evenementPersonnelRepository.findByStageId(stage.getId()).stream())
                .sorted(Comparator.comparing(EvenementPersonnel::getDate))
                .toList();

        long tachesTerminees = taches.stream().filter(t -> t.getStatut() == Tache.StatutTache.terminee).count();
        long tachesEnCours = taches.stream().filter(t -> t.getStatut() == Tache.StatutTache.en_cours).count();
        long tachesRetard = taches.stream().filter(t -> t.getStatut() == Tache.StatutTache.en_retard
                || (t.getDateLimite().isBefore(LocalDate.now()) && t.getStatut() != Tache.StatutTache.terminee)).count();
        int progressionMoyenne = stages.isEmpty() ? 0 : (int) Math.round(stages.stream()
                .mapToDouble(stage -> {
                    List<Tache> stageTaches = tacheRepository.findByStageId(stage.getId());
                    long terminees = stageTaches.stream().filter(t -> t.getStatut() == Tache.StatutTache.terminee).count();
                    return stageTaches.isEmpty() ? 0 : terminees * 100.0 / stageTaches.size();
                }).average().orElse(0));

        model.addAttribute("stages", stages);
        model.addAttribute("taches", taches);
        model.addAttribute("objectifs", objectifs);
        model.addAttribute("livrables", livrables.stream()
                .sorted(Comparator.comparing(Livrable::getDateDepot).reversed()).toList());
        Map<Integer, Long> livrablesParStage = stages.stream().collect(Collectors.toMap(
                Stage::getId,
                stage -> (long) livrablesDesStages(List.of(stage)).size()
        ));
        model.addAttribute("livrablesParStage", livrablesParStage);
        model.addAttribute("evaluations", evaluations);
        model.addAttribute("evenements", evenements);
        model.addAttribute("nombreStagiaires", stages.size());
        model.addAttribute("objectifsActifs", objectifs.stream().filter(o -> o.getStatut() != Objectif.StatutObjectif.atteint).count());
        model.addAttribute("tachesEnCours", tachesEnCours);
        model.addAttribute("tachesTerminees", tachesTerminees);
        model.addAttribute("tachesRetard", tachesRetard);
        model.addAttribute("livrablesAttente", livrables.stream().filter(l -> l.getStatut() == Livrable.StatutLivrable.depose).count());
        model.addAttribute("evaluationsRealisees", evaluations.size());
        model.addAttribute("progressionMoyenne", progressionMoyenne);
        return "encadreur/dashboard";
    }

    @GetMapping("/taches")
    public String afficherTaches(@AuthenticationPrincipal CustomUserDetails userDetails,
                                  Model model) {
        ajouterIdentite(model, userDetails, "taches");
        List<Stage> mesStages = getStages(userDetails);
        List<Tache> toutesLesTaches = tachesDesStages(mesStages);

        model.addAttribute("mesStages", mesStages);
        model.addAttribute("toutesLesTaches", toutesLesTaches);
        model.addAttribute("tachesTotales", toutesLesTaches.size());
        model.addAttribute("tachesTerminees", toutesLesTaches.stream().filter(t -> t.getStatut() == Tache.StatutTache.terminee).count());
        model.addAttribute("tachesEnCours", toutesLesTaches.stream().filter(t -> t.getStatut() == Tache.StatutTache.en_cours).count());
        model.addAttribute("tachesRetard", toutesLesTaches.stream().filter(t -> t.getStatut() == Tache.StatutTache.en_retard
                || (t.getDateLimite().isBefore(LocalDate.now()) && t.getStatut() != Tache.StatutTache.terminee)).count());
        model.addAttribute("stagiairesConcernes", mesStages.size());
        model.addAttribute("tauxExecution", toutesLesTaches.isEmpty() ? 0
                : toutesLesTaches.stream().filter(t -> t.getStatut() == Tache.StatutTache.terminee).count() * 100 / toutesLesTaches.size());
        return "encadreur/taches";
    }

    @PostMapping("/taches/creer")
    public String creerTache(@AuthenticationPrincipal CustomUserDetails userDetails,
                              @RequestParam Integer stageId,
                              @RequestParam String titre,
                              @RequestParam(required = false) String description,
                              @RequestParam String dateLimite,
                              @RequestParam(defaultValue = "moyenne") String priorite,
                              Model model) {
        stageRepository.findById(stageId).filter(stage -> stageAppartient(userDetails, stage)).ifPresent(stage -> {
            Tache tache = new Tache(stage, titre, description,
                    LocalDate.parse(dateLimite));
            tache.setPriorite(priorite);
            tacheRepository.save(tache);
        });
        return "redirect:/encadreur/taches?succes=true";
    }

    @PostMapping("/taches/statut")
    public String modifierStatutTache(@AuthenticationPrincipal CustomUserDetails userDetails,
                                      @RequestParam Integer id,
                                      @RequestParam Tache.StatutTache statut) {
        tacheRepository.findById(id)
                .filter(tache -> stageAppartient(userDetails, tache.getStage()))
                .ifPresent(tache -> {
                    tache.setStatut(statut);
                    tacheRepository.save(tache);
                });
        return "redirect:/encadreur/taches";
    }
    @GetMapping("/evaluations")
public String afficherEvaluations(@AuthenticationPrincipal CustomUserDetails userDetails,
                                  Model model,
                                  @RequestParam(required = false) String succes) {
    ajouterIdentite(model, userDetails, "evaluations");
    List<Stage> mesStages = getStages(userDetails);
    List<Evaluation> evaluations = evaluationsDesStages(mesStages).stream()
            .sorted(Comparator.comparing(Evaluation::getDateEvaluation).reversed())
            .toList();
    Map<Integer, BigDecimal> moyennes = new HashMap<>();
    Map<Integer, String> criteresPrincipaux = new HashMap<>();
    Map<String, BigDecimal> moyennesCriteres = new LinkedHashMap<>();
    Map<String, Integer> nombresNotesCriteres = new HashMap<>();
    evaluations.forEach(evaluation -> {
        List<NoteEvaluation> notes = noteRepository.findByEvaluationId(evaluation.getId());
        BigDecimal moyenne = notes.isEmpty() ? BigDecimal.ZERO : notes.stream()
                .map(NoteEvaluation::getNote)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(notes.size()), 1, java.math.RoundingMode.HALF_UP);
        moyennes.put(evaluation.getId(), moyenne);
        notes.stream().max(Comparator.comparing(NoteEvaluation::getNote))
                .ifPresent(note -> criteresPrincipaux.put(evaluation.getId(), note.getCritere().getLibelle()));
        notes.forEach(note -> {
            String libelle = note.getCritere().getLibelle();
            moyennesCriteres.merge(libelle, note.getNote(), BigDecimal::add);
            nombresNotesCriteres.merge(libelle, 1, Integer::sum);
        });
    });
    moyennesCriteres.replaceAll((libelle, total) -> total.divide(
            BigDecimal.valueOf(nombresNotesCriteres.get(libelle)), 1, java.math.RoundingMode.HALF_UP));
    long finales = evaluations.stream()
            .filter(evaluation -> evaluation.getTypeEvaluation() == Evaluation.TypeEvaluation.finale).count();
    long continues = evaluations.size() - finales;
    BigDecimal moyenneGenerale = moyennes.isEmpty() ? BigDecimal.ZERO : moyennes.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(moyennes.size()), 1, java.math.RoundingMode.HALF_UP);
    long excellentes = moyennes.values().stream()
            .filter(moyenne -> moyenne.compareTo(BigDecimal.valueOf(18)) >= 0).count();
    long enAttente = mesStages.stream()
            .filter(stage -> evaluations.stream().noneMatch(evaluation -> evaluation.getStage().getId().equals(stage.getId())
                    && evaluation.getTypeEvaluation() == Evaluation.TypeEvaluation.finale))
            .count();
    long note18a20 = moyennes.values().stream().filter(n -> n.compareTo(BigDecimal.valueOf(18)) >= 0).count();
    long note15a17 = moyennes.values().stream().filter(n -> n.compareTo(BigDecimal.valueOf(15)) >= 0
            && n.compareTo(BigDecimal.valueOf(18)) < 0).count();
    long note10a14 = moyennes.values().stream().filter(n -> n.compareTo(BigDecimal.TEN) >= 0
            && n.compareTo(BigDecimal.valueOf(15)) < 0).count();
    long noteMoins10 = moyennes.values().stream().filter(n -> n.compareTo(BigDecimal.TEN) < 0).count();
    List<Evaluation> meilleuresEvaluations = evaluations.stream()
            .sorted(Comparator.comparing((Evaluation evaluation) ->
                    moyennes.getOrDefault(evaluation.getId(), BigDecimal.ZERO)).reversed())
            .limit(4).toList();
    model.addAttribute("mesStages", mesStages);
    model.addAttribute("criteres", critereRepository.findAll());
    model.addAttribute("evaluations", evaluations);
    model.addAttribute("moyennesEvaluations", moyennes);
    model.addAttribute("criteresPrincipaux", criteresPrincipaux);
    model.addAttribute("moyennesCriteres", moyennesCriteres);
    model.addAttribute("evaluationsTotales", evaluations.size());
    model.addAttribute("evaluationsFinales", finales);
    model.addAttribute("evaluationsContinues", continues);
    model.addAttribute("stagiairesEvalues", evaluations.stream()
            .map(evaluation -> evaluation.getStage().getStagiaire().getId()).distinct().count());
    model.addAttribute("moyenneGenerale", moyenneGenerale);
    model.addAttribute("evaluationsEnAttente", enAttente);
    model.addAttribute("evaluationsExcellentes", excellentes);
    model.addAttribute("tauxEvaluation", mesStages.isEmpty() ? 0
            : Math.min(100, Math.round((mesStages.size() - enAttente) * 100.0 / mesStages.size())));
    model.addAttribute("note18a20", note18a20);
    model.addAttribute("note15a17", note15a17);
    model.addAttribute("note10a14", note10a14);
    model.addAttribute("noteMoins10", noteMoins10);
    model.addAttribute("meilleuresEvaluations", meilleuresEvaluations);
    model.addAttribute("evaluationsCeMois", evaluations.stream()
            .filter(evaluation -> evaluation.getDateEvaluation().getMonth() == LocalDate.now().getMonth()
                    && evaluation.getDateEvaluation().getYear() == LocalDate.now().getYear()).count());
    if (succes != null) model.addAttribute("succes", succes);

    return "encadreur/evaluations";
}

@PostMapping("/evaluations/creer")
public String creerEvaluation(@AuthenticationPrincipal CustomUserDetails userDetails,
                              @RequestParam Integer stageId,
                               @RequestParam String typeEvaluation,
                               @RequestParam String dateEvaluation,
                               @RequestParam(required = false) String appreciation,
                               @RequestParam Map<String, String> allParams) {
    stageRepository.findById(stageId).filter(stage -> stageAppartient(userDetails, stage)).ifPresent(stage -> {
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
    ajouterIdentite(model, userDetails, "objectifs");
    List<Stage> mesStages = getStages(userDetails);
    List<Objectif> objectifs = objectifsDesStages(mesStages);

    model.addAttribute("mesStages", mesStages);
    model.addAttribute("objectifs", objectifs);
    model.addAttribute("objectifsTotaux", objectifs.size());
    model.addAttribute("objectifsAtteints", objectifs.stream().filter(o -> o.getStatut() == Objectif.StatutObjectif.atteint).count());
    model.addAttribute("objectifsEnCours", objectifs.stream().filter(o -> o.getStatut() == Objectif.StatutObjectif.en_cours).count());
    model.addAttribute("objectifsRetard", objectifs.stream().filter(o -> o.getDateLimite() != null
            && o.getDateLimite().isBefore(LocalDate.now()) && o.getStatut() != Objectif.StatutObjectif.atteint).count());
    model.addAttribute("stagiairesConcernes", objectifs.stream().map(o -> o.getStage().getId()).distinct().count());
    model.addAttribute("tauxRealisation", objectifs.isEmpty() ? 0
            : objectifs.stream().filter(o -> o.getStatut() == Objectif.StatutObjectif.atteint).count() * 100 / objectifs.size());
    if (succes != null) model.addAttribute("succes", succes);

    return "encadreur/objectifs";
}

@PostMapping("/objectifs/creer")
 public String creerObjectif(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @RequestParam Integer stageId,
                             @RequestParam String libelle,
                             @RequestParam(required = false) String description,
                             @RequestParam(defaultValue = "1") Integer ordre,
                             @RequestParam(defaultValue = "moyenne") Objectif.Priorite priorite,
                             @RequestParam(required = false) String dateLimite,
                             @RequestParam(defaultValue = "0") Integer progression) {
    stageRepository.findById(stageId).filter(stage -> stageAppartient(userDetails, stage)).ifPresent(stage -> {
        Objectif objectif = new Objectif(stage, libelle, description, ordre);
        objectif.setPriorite(priorite);
        objectif.setProgression(Math.max(0, Math.min(100, progression)));
        if (dateLimite != null && !dateLimite.isBlank()) {
            objectif.setDateLimite(LocalDate.parse(dateLimite));
        }
        objectifRepository.save(objectif);
    });

    return "redirect:/encadreur/objectifs?succes=true";
}

@PostMapping("/objectifs/statut")
public String modifierStatutObjectif(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @RequestParam Integer id,
                                     @RequestParam Objectif.StatutObjectif statut) {
    objectifRepository.findById(id)
            .filter(objectif -> stageAppartient(userDetails, objectif.getStage()))
            .ifPresent(objectif -> {
                objectif.setStatut(statut);
                if (statut == Objectif.StatutObjectif.atteint) objectif.setProgression(100);
                objectifRepository.save(objectif);
            });
    return "redirect:/encadreur/objectifs";
}

@GetMapping("/livrables")
public String afficherLivrables(@AuthenticationPrincipal CustomUserDetails userDetails,
                                Model model) {
    ajouterIdentite(model, userDetails, "livrables");
    List<Stage> mesStages = getStages(userDetails);
    List<Livrable> livrables = livrablesDesStages(mesStages).stream()
            .sorted(Comparator.comparing(Livrable::getDateDepot).reversed())
            .toList();
    Map<Integer, Stage> stagesLivrables = new HashMap<>();
    livrables.forEach(livrable -> stagesLivrables.put(livrable.getId(), stageDuLivrable(livrable)));
    long enAttente = livrables.stream().filter(l -> l.getStatut() == Livrable.StatutLivrable.depose).count();
    long valides = livrables.stream().filter(l -> l.getStatut() == Livrable.StatutLivrable.valide).count();
    long corrections = livrables.stream().filter(l -> l.getStatut() == Livrable.StatutLivrable.correction_demandee
            || l.getStatut() == Livrable.StatutLivrable.rejete).count();
    Map<String, Long> livrablesParCategorie = livrables.stream().collect(Collectors.groupingBy(
            livrable -> livrable.getCategorie() == null || livrable.getCategorie().isBlank()
                    ? "Autre" : livrable.getCategorie(),
            LinkedHashMap::new, Collectors.counting()));
    Map<Integer, Long> livrablesParStage = mesStages.stream().collect(Collectors.toMap(
            Stage::getId, stage -> livrables.stream()
                    .filter(livrable -> {
                        Stage stageLivrable = stagesLivrables.get(livrable.getId());
                        return stageLivrable != null && stageLivrable.getId().equals(stage.getId());
                    }).count(),
            (a, b) -> a, LinkedHashMap::new));
    model.addAttribute("livrables", livrables);
    model.addAttribute("mesStages", mesStages);
    model.addAttribute("stagesLivrables", stagesLivrables);
    model.addAttribute("livrablesTotaux", livrables.size());
    model.addAttribute("livrablesEnAttente", enAttente);
    model.addAttribute("livrablesValides", valides);
    model.addAttribute("livrablesACorriger", corrections);
    model.addAttribute("tauxValidation", livrables.isEmpty() ? 0 : Math.round(valides * 100.0 / livrables.size()));
    model.addAttribute("livrablesParCategorie", livrablesParCategorie);
    model.addAttribute("livrablesParStage", livrablesParStage);
    return "encadreur/livrables";
}


@GetMapping("/livrables/valider/{id}")
public String validerLivrable(@AuthenticationPrincipal CustomUserDetails userDetails,
                              @PathVariable Integer id) {
    livrableRepository.findById(id)
            .filter(livrable -> stageAppartient(userDetails, stageDuLivrable(livrable)))
            .ifPresent(livrable -> {
        livrable.setStatut(Livrable.StatutLivrable.valide);
        livrableRepository.save(livrable);
    });
    return "redirect:/encadreur/livrables?succes=valide";
}

@GetMapping("/livrables/rejeter/{id}")
public String rejeterLivrable(@AuthenticationPrincipal CustomUserDetails userDetails,
                              @PathVariable Integer id) {
    livrableRepository.findById(id)
            .filter(livrable -> stageAppartient(userDetails, stageDuLivrable(livrable)))
            .ifPresent(livrable -> {
        livrable.setStatut(Livrable.StatutLivrable.correction_demandee);
        livrableRepository.save(livrable);
    });
    return "redirect:/encadreur/livrables?succes=correction";
}

@PostMapping("/livrables/commenter")
public String commenterLivrable(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @RequestParam Integer id,
                                @RequestParam String commentaire) {
    livrableRepository.findById(id)
            .filter(livrable -> stageAppartient(userDetails, stageDuLivrable(livrable)))
            .ifPresent(livrable -> {
                livrable.setCommentaireEncadreur(commentaire.trim());
                livrableRepository.save(livrable);
            });
    return "redirect:/encadreur/livrables?succes=commentaire";
}

@GetMapping("/profil")
public String afficherProfil(@AuthenticationPrincipal CustomUserDetails userDetails,
                             Model model) {
    ajouterIdentite(model, userDetails, "profil");
    Encadreur encadreur = getEncadreur(userDetails);
    if (encadreur == null) return "redirect:/login";
    List<Stage> stages = getStages(userDetails);
    List<Evaluation> evaluations = evaluationsDesStages(stages);
    List<Livrable> livrables = livrablesDesStages(stages);
    List<EvenementPersonnel> evenements = stages.stream()
            .flatMap(stage -> evenementPersonnelRepository.findByStageId(stage.getId()).stream())
            .sorted(Comparator.comparing(EvenementPersonnel::getDate).reversed())
            .toList();
    long valides = livrables.stream().filter(l -> l.getStatut() == Livrable.StatutLivrable.valide).count();
    long reunions = evenements.stream().filter(e -> "reunion".equalsIgnoreCase(e.getTypeCouleur())).count();
    model.addAttribute("encadreur", encadreur);
    model.addAttribute("utilisateur", encadreur.getUtilisateur());
    model.addAttribute("stages", stages);
    model.addAttribute("evaluations", evaluations);
    model.addAttribute("livrables", livrables);
    model.addAttribute("evenements", evenements);
    model.addAttribute("stagiairesEncadres", stages.size());
    model.addAttribute("evaluationsRealisees", evaluations.size());
    model.addAttribute("livrablesValides", valides);
    model.addAttribute("reunionsEffectuees", reunions);
    model.addAttribute("tauxSuivi", stages.isEmpty() ? 0 : Math.min(100,
            Math.round((evaluations.size() + valides) * 100.0 / (stages.size() * 2))));
    return "encadreur/profil";
}

@PostMapping("/profil/modifier")
public String modifierProfil(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @RequestParam String nom,
                             @RequestParam String prenom,
                             @RequestParam String email,
                             @RequestParam(required = false) String telephone,
                             @RequestParam(required = false) String adresse,
                             @RequestParam(required = false) String fonction,
                             @RequestParam(required = false) String specialite) {
    Encadreur encadreur = getEncadreur(userDetails);
    if (encadreur != null) {
        Utilisateur utilisateur = encadreur.getUtilisateur();
        utilisateur.setNom(nom.trim());
        utilisateur.setPrenom(prenom.trim());
        utilisateur.setEmail(email.trim());
        utilisateur.setTelephone(telephone == null ? null : telephone.trim());
        utilisateur.setAdresse(adresse == null ? null : adresse.trim());
        utilisateurRepository.save(utilisateur);
        encadreur.setFonction(fonction == null ? null : fonction.trim());
        encadreur.setSpecialite(specialite == null ? null : specialite.trim());
        encadreurRepository.save(encadreur);
    }
    return "redirect:/encadreur/profil?succes=true";
}

@GetMapping("/planning")
public String planning(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    ajouterIdentite(model, userDetails, "planning");
    List<Stage> stages = getStages(userDetails);
    List<Tache> taches = tachesDesStages(stages);
    List<Objectif> objectifs = objectifsDesStages(stages);
    List<Livrable> livrables = livrablesDesStages(stages);
    List<Evaluation> evaluations = evaluationsDesStages(stages);
    List<EvenementPersonnel> evenements = stages.stream()
            .flatMap(stage -> evenementPersonnelRepository.findByStageId(stage.getId()).stream())
            .sorted(Comparator.comparing(EvenementPersonnel::getDate)
                    .thenComparing(event -> event.getHeure() == null ? LocalTime.MIN : event.getHeure()))
            .toList();
    LocalDate aujourdHui = LocalDate.now();
    LocalDate finSemaine = aujourdHui.plusDays(7);

    model.addAttribute("stages", stages);
    model.addAttribute("taches", taches);
    model.addAttribute("objectifs", objectifs);
    model.addAttribute("livrables", livrables);
    model.addAttribute("evaluations", evaluations);
    model.addAttribute("evenements", evenements);
    model.addAttribute("evenementsPlanifies", evenements.size() + evaluations.size());
    model.addAttribute("reunionsProgrammees", evenements.stream().filter(e -> "reunion".equals(e.getTypeCouleur())).count());
    model.addAttribute("echeancesSemaine", taches.stream().filter(t -> !t.getDateLimite().isBefore(aujourdHui)
            && !t.getDateLimite().isAfter(finSemaine)).count());
    model.addAttribute("livrablesAttendus", livrables.stream().filter(l -> l.getStatut() == Livrable.StatutLivrable.depose).count());
    model.addAttribute("objectifsProches", objectifs.stream().filter(o -> o.getDateLimite() != null
            && !o.getDateLimite().isBefore(aujourdHui) && !o.getDateLimite().isAfter(finSemaine)).count());
    model.addAttribute("activitesMois", evenements.stream().filter(e -> e.getDate().getMonth() == aujourdHui.getMonth()
            && e.getDate().getYear() == aujourdHui.getYear()).count());
    return "encadreur/planning";
}

@PostMapping("/planning/evenements/ajouter")
public String ajouterEvenement(@AuthenticationPrincipal CustomUserDetails userDetails,
                               @RequestParam Integer stageId,
                               @RequestParam String titre,
                               @RequestParam String type,
                               @RequestParam String date,
                               @RequestParam(required = false) String heure,
                               @RequestParam(required = false) String lieu,
                               @RequestParam(required = false) String description,
                               @RequestParam(defaultValue = "1_jour") String rappel) {
    stageRepository.findById(stageId)
            .filter(stage -> stageAppartient(userDetails, stage))
            .ifPresent(stage -> {
                EvenementPersonnel evenement = new EvenementPersonnel(
                        stage, titre.trim(), LocalDate.parse(date), type);
                if (heure != null && !heure.isBlank()) evenement.setHeure(LocalTime.parse(heure));
                evenement.setLieu(lieu);
                evenement.setDescription(description);
                evenement.setRappel(rappel);
                evenementPersonnelRepository.save(evenement);
            });
    return "redirect:/encadreur/planning?succes=evenement";
}

@GetMapping("/messagerie")
public String messagerie(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    ajouterIdentite(model, userDetails, "messagerie");
    Utilisateur utilisateur = userDetails.getUtilisateur();
    List<Conversation> conversations = conversationRepository
            .findByParticipantIdOrderByDernierMessageDesc(utilisateur.getId());
    Map<Integer, List<Message>> messagesParConversation = new HashMap<>();
    Map<Integer, Long> nonLusParConversation = new HashMap<>();
    conversations.forEach(conversation -> {
        messagesParConversation.put(conversation.getId(),
                messageRepository.findByConversationIdOrderByDateEnvoiAsc(conversation.getId()));
        nonLusParConversation.put(conversation.getId(),
                conversationRepository.countNonLuByConversation(conversation.getId(), utilisateur.getId()));
    });
    model.addAttribute("utilisateurConnecte", utilisateur);
    model.addAttribute("conversations", conversations);
    model.addAttribute("messagesParConversation", messagesParConversation);
    model.addAttribute("nonLusParConversation", nonLusParConversation);
    return "encadreur/messagerie";
}

}
