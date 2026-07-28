package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.EmailService;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.gestionstages.gestion_stages.entities.*;
import com.gestionstages.gestion_stages.repositories.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.gestionstages.gestion_stages.security.CustomUserDetails;
@Controller
@RequestMapping("/responsable")
public class ResponsableController {
    private final JavaMailSender mailSender;

    private final DemandeStageRepository demandeStageRepository;
    private final StagiaireRepository stagiaireRepository;
    private final StageRepository stageRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final EncadreurRepository encadreurRepository;
    private final ServiceEntrepriseRepository serviceRepository;
    private final ProjetRepository projetRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    private final ArchiveRepository archiveRepository;
    private final DocumentRepository documentRepository;
    private final EvaluationRepository evaluationRepository;
    private final TacheRepository tacheRepository;
    private final ObjectifRepository objectifRepository;
    private final LivrableRepository livrableRepository;
    private final EvenementPersonnelRepository evenementPersonnelRepository;
    
    private final EmailService emailService;

    private static String toJson(Object o) {
    if (o == null) return "null";
    if (o instanceof List<?> l) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < l.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(quote(l.get(i)));
        }
        return sb.append("]").toString();
    }
    return quote(o);
}

private static String quote(Object v) {
    if (v instanceof Number) return v.toString();
    if (v instanceof Boolean) return v.toString();
    return "\"" + v.toString().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
}

    public ResponsableController(DemandeStageRepository demandeStageRepository,
                              StagiaireRepository stagiaireRepository,
                              StageRepository stageRepository,
                              UtilisateurRepository utilisateurRepository,
                              RoleRepository roleRepository,
                              EncadreurRepository encadreurRepository,
                              ServiceEntrepriseRepository serviceRepository,
                              ProjetRepository projetRepository,
                              PasswordEncoder passwordEncoder,
                              ArchiveRepository archiveRepository,
                              DocumentRepository documentRepository,
                              EvaluationRepository evaluationRepository,
                              TacheRepository tacheRepository,
                              ObjectifRepository objectifRepository,
                              LivrableRepository livrableRepository,
                              EvenementPersonnelRepository evenementPersonnelRepository,
                              EmailService emailService,
                              NotificationRepository notificationRepository,
                              JavaMailSender mailSender) {
    this.demandeStageRepository = demandeStageRepository;
    this.stagiaireRepository = stagiaireRepository;
    this.stageRepository = stageRepository;
    this.utilisateurRepository = utilisateurRepository;
    this.roleRepository = roleRepository;
    this.notificationRepository = notificationRepository;
    this.encadreurRepository = encadreurRepository;
    this.serviceRepository = serviceRepository;
    this.projetRepository = projetRepository;
    this.passwordEncoder = passwordEncoder;
    this.archiveRepository = archiveRepository;
    this.documentRepository = documentRepository;
    this.evaluationRepository = evaluationRepository;
    this.tacheRepository = tacheRepository;
    this.objectifRepository = objectifRepository;
    this.livrableRepository = livrableRepository;
    this.evenementPersonnelRepository = evenementPersonnelRepository;
    this.mailSender = mailSender;
    this.emailService = emailService;
}

        @GetMapping("/dashboard")
    public String afficherDashboard(Model model) {
        model.addAttribute("activePage", "dashboard");
        List<DemandeStage> demandes = demandeStageRepository.findAll();

        long demandesEnAttente = demandes.stream()
                .filter(d -> d.getStatut() == DemandeStage.StatutDemande.en_attente).count();
        long acceptees = demandes.stream()
                .filter(d -> d.getStatut() == DemandeStage.StatutDemande.acceptee).count();
        long refusees = demandes.stream()
                .filter(d -> d.getStatut() == DemandeStage.StatutDemande.refusee).count();
        long totalDemandes = demandes.size();

        List<Stagiaire> stagiaires = stagiaireRepository.findAll();
        List<Stage> stages = stageRepository.findAll();
        long totalStagiaires = stagiaires.size();
        long totalStages = stages.size();
        long totalStagiairesActifs = stagiaireRepository.countByStatut(Stagiaire.StatutStagiaire.actif);
        long stagesEnCours = stageRepository.countByStatut(Stage.StatutStage.en_cours);
        long stagesTermines = stageRepository.countByStatut(Stage.StatutStage.termine);
        long stagesSuspendus = stageRepository.countByStatut(Stage.StatutStage.suspendu);

        LocalDate aujourdHui = LocalDate.now();
        LocalDate limite = aujourdHui.plusDays(30);
        List<Stage> echeancesStages = stageRepository.findByDateFinBetween(aujourdHui, limite);
        echeancesStages.sort(Comparator.comparing(Stage::getDateFin));
        long echeancesCount = echeancesStages.size();
        List<Map<String, Object>> echeances = echeancesStages.stream().limit(5).map(s -> {
            Map<String, Object> m = new HashMap<>();
            String nom = (s.getStagiaire() != null && s.getStagiaire().getUtilisateur() != null)
                    ? s.getStagiaire().getUtilisateur().getPrenom() + " " + s.getStagiaire().getUtilisateur().getNom()
                    : "Stagiaire";
            m.put("nom", nom);
            m.put("service", s.getService() != null ? s.getService().getNom() : "—");
            m.put("dateFin", s.getDateFin());
            m.put("joursRestants", Math.max(0, aujourdHui.until(s.getDateFin()).getDays()));
            return m;
        }).toList();

        long tauxReussite = totalStages > 0 ? (stagesTermines * 100 / totalStages) : 0;
        int progressionMoyenne = 65;

        Map<String, Long> repartition = stagiaires.stream()
                .collect(Collectors.groupingBy(
                        s -> (s.getDemandeStage() != null && s.getDemandeStage().getFiliere() != null)
                                ? s.getDemandeStage().getFiliere() : "Autre",
                        Collectors.counting()));
        List<String> filiereLabels = new ArrayList<>(repartition.keySet());
        List<Long> filiereData = new ArrayList<>(repartition.values());

        List<Notification> activitesRecentes = notificationRepository.findAllByOrderByDateEnvoiDesc().stream().limit(6).toList();

        List<String> moisLabels = List.of("Fév", "Mar", "Avr", "Mai", "Juin", "Juil");
        List<Integer> dataDemandes = List.of(12, 19, 15, 25, 22, (int) demandesEnAttente);
        List<Integer> dataStagesEnCours = List.of(8, 12, 14, 16, 17, (int) stagesEnCours);
        List<Integer> dataStagesTermines = List.of(5, 9, 11, 15, 20, (int) stagesTermines);

        model.addAttribute("demandesEnAttente", demandesEnAttente);
        model.addAttribute("totalDemandes", totalDemandes);
        model.addAttribute("acceptees", acceptees);
        model.addAttribute("refusees", refusees);
        model.addAttribute("totalStagiaires", totalStagiaires);
        model.addAttribute("totalEncadreurs", encadreurRepository.count());
        model.addAttribute("totalStages", totalStages);
        model.addAttribute("totalStagiairesActifs", totalStagiairesActifs);
        model.addAttribute("stagesEnCours", stagesEnCours);
        model.addAttribute("stagesTermines", stagesTermines);
        model.addAttribute("stagesSuspendus", stagesSuspendus);
        model.addAttribute("echeancesCount", echeancesCount);
        model.addAttribute("echeances", echeances);
        model.addAttribute("tauxReussite", tauxReussite);
        model.addAttribute("progressionMoyenne", progressionMoyenne);
        model.addAttribute("moisLabelsJson", toJson(moisLabels));
        model.addAttribute("dataDemandesJson", toJson(dataDemandes));
        model.addAttribute("dataStagesEnCoursJson", toJson(dataStagesEnCours));
        model.addAttribute("dataStagesTerminesJson", toJson(dataStagesTermines));
        model.addAttribute("filiereLabelsJson", toJson(filiereLabels));
        model.addAttribute("filiereDataJson", toJson(filiereData));
        model.addAttribute("notificationsCount", 5);
        model.addAttribute("activitesRecentes", activitesRecentes);
        model.addAttribute("stagiaires", stagiaires.stream().limit(6).toList());
        model.addAttribute("stages", stages);
        model.addAttribute("dernieresDemandes", demandes.stream()
                .sorted(Comparator.comparing(DemandeStage::getDateDemande).reversed())
                .limit(5).toList());
        model.addAttribute("messagesCount", 3);

        return "responsable/dashboard";
    }

            @GetMapping("/demandes")
    public String afficherDemandes(Model model) {
        model.addAttribute("activePage", "demandes");
        List<DemandeStage> demandes = demandeStageRepository.findAll();
        long enAttente = demandes.stream()
                .filter(d -> d.getStatut() == DemandeStage.StatutDemande.en_attente).count();
        long acceptees = demandes.stream()
                .filter(d -> d.getStatut() == DemandeStage.StatutDemande.acceptee).count();
        long refusees = demandes.stream()
                .filter(d -> d.getStatut() == DemandeStage.StatutDemande.refusee).count();
        long total = demandes.size();
        long aujourdHui = demandes.stream()
                .filter(d -> d.getDateDemande() != null
                        && d.getDateDemande().toLocalDate().equals(java.time.LocalDate.now())).count();
        long taux = total > 0 ? (acceptees * 100 / total) : 0;

        model.addAttribute("demandes", demandes);
        model.addAttribute("totalDemandes", total);
        model.addAttribute("enAttente", enAttente);
        model.addAttribute("acceptees", acceptees);
        model.addAttribute("refusees", refusees);
        model.addAttribute("aujourdHui", aujourdHui);
        model.addAttribute("tauxAcceptation", taux);
        model.addAttribute("demandesRecentes", demandes.stream()
                .sorted(Comparator.comparing(DemandeStage::getDateDemande).reversed())
                .limit(5).toList());
        model.addAttribute("moisLabelsJson", toJson(List.of("Fév", "Mar", "Avr", "Mai", "Juin", "Juil")));
        model.addAttribute("demandesMoisJson", toJson(List.of(8, 13, 11, 18, 16, (int) total)));
        model.addAttribute("statutsDemandesJson", toJson(List.of(acceptees, enAttente, refusees)));
        model.addAttribute("notificationsCount", 5);
        model.addAttribute("messagesCount", 3);
        return "responsable/demandes";
    }

    @GetMapping("/demandes/accepter/{id}")
    public String accepterDemande(@PathVariable Integer id,
                                  RedirectAttributes redirectAttributes) {
        demandeStageRepository.findById(id).ifPresent(demande -> {
            demande.setStatut(DemandeStage.StatutDemande.acceptee);
            demandeStageRepository.save(demande);
            boolean envoye = emailService.envoyerDecisionDemande(
                    emailDemande(demande),
                    demande.getPrenom() + " " + demande.getNom(),
                    true,
                    null
            );
            ajouterRetourEmail(redirectAttributes, envoye, emailDemande(demande),
                    "La demande a été acceptée.");
        });
        return "redirect:/responsable/demandes";
    }

    @GetMapping("/demandes/refuser/{id}")
    public String refuserDemande(@PathVariable Integer id,
                                 RedirectAttributes redirectAttributes) {
        demandeStageRepository.findById(id).ifPresent(demande -> {
            demande.setStatut(DemandeStage.StatutDemande.refusee);
            demandeStageRepository.save(demande);
            boolean envoye = emailService.envoyerDecisionDemande(
                    emailDemande(demande),
                    demande.getPrenom() + " " + demande.getNom(),
                    false,
                    demande.getMotifRefus()
            );
            ajouterRetourEmail(redirectAttributes, envoye, emailDemande(demande),
                    "La demande a été refusée.");
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

    @GetMapping("/admissions")
    public String afficherAdmissions(Model model) {
        model.addAttribute("activePage", "admissions");
        List<DemandeStage> demandesAcceptees = demandeStageRepository
                .findByStatut(DemandeStage.StatutDemande.acceptee);
        model.addAttribute("demandesAcceptees", demandesAcceptees);
        return "responsable/admissions";
    }

    @GetMapping("/admissions/fiche/{id}")
    public String afficherFiche(@PathVariable Integer id, Model model) {
        Optional<DemandeStage> demandeOpt = demandeStageRepository.findById(id);
        if (demandeOpt.isEmpty()) return "redirect:/responsable/admissions";

        DemandeStage demande = demandeOpt.get();
        model.addAttribute("activePage", "admissions");

        // Generer le matricule
        long nombreStagiaires = stagiaireRepository.count() + 1;
        String matricule = "STG-" + LocalDate.now().getYear()
                + "-" + String.format("%03d", nombreStagiaires);

        model.addAttribute("demande", demande);
        model.addAttribute("matricule", matricule);
        model.addAttribute("encadreurs", encadreurRepository.findAll());
        model.addAttribute("services", serviceRepository.findAll());
        model.addAttribute("projets", projetRepository.findAll());

        return "responsable/fiche-stagiaire";
    }

    @PostMapping("/admissions/admettre/{id}")
public String admettreStagiaire(@PathVariable Integer id,
                                 @RequestParam Integer encadreurId,
                                 @RequestParam Integer serviceId,
                                 @RequestParam(required = false) Integer projetId,
                                 @RequestParam String dateDebut,
                                 @RequestParam String dateFin,
                                 Model model) {
    Optional<DemandeStage> demandeOpt = demandeStageRepository.findById(id);
    if (demandeOpt.isEmpty()) return "redirect:/responsable/admissions";

    DemandeStage demande = demandeOpt.get();

    try {
        // 1. Générer un email unique pour le stagiaire
        String emailBase = demande.getPrenom().toLowerCase()
                + "." + demande.getNom().toLowerCase()
                + "@stagiaire.com";
        String emailFinal = emailBase;
        int compteur = 1;
        while (utilisateurRepository.existsByEmail(emailFinal)) {
            emailFinal = demande.getPrenom().toLowerCase()
                    + "." + demande.getNom().toLowerCase()
                    + compteur + "@stagiaire.com";
            compteur++;
        }

        // 2. Mot de passe en clair (pour l'email) et haché (pour la BDD)
        String motDePasse = "stag" + LocalDate.now().getYear();
        Role roleStagiaire = roleRepository.findByLibelle("STAGIAIRE").orElseThrow();

        // 3. Créer le compte utilisateur du stagiaire
        Utilisateur utilisateur = new Utilisateur(roleStagiaire,
                demande.getNom(), demande.getPrenom(),
                emailFinal, passwordEncoder.encode(motDePasse));
        utilisateurRepository.save(utilisateur);

        // 4. Générer le matricule
        long nombreStagiaires = stagiaireRepository.count() + 1;
        String matricule = "STG-" + LocalDate.now().getYear()
                + "-" + String.format("%03d", nombreStagiaires);

        // 5. Créer la fiche stagiaire
        Stagiaire stagiaire = new Stagiaire(utilisateur, demande,
                matricule, LocalDate.now());
        stagiaireRepository.save(stagiaire);

        // 6. Créer le stage avec affectation
        Encadreur encadreur = encadreurRepository.findById(encadreurId).orElseThrow();
        ServiceEntreprise service = serviceRepository.findById(serviceId).orElseThrow();

        LocalDate debut = LocalDate.parse(dateDebut);
        LocalDate fin = LocalDate.parse(dateFin);
        long dureeJours = debut.until(fin).getDays();
        String duree = dureeJours + " jours";

        String numeroStage = "STG-NUM-" + LocalDate.now().getYear()
                + "-" + String.format("%03d", stageRepository.count() + 1);

        Stage stage = new Stage(stagiaire, encadreur, service,
                numeroStage, debut, fin, duree);

        if (projetId != null) {
            projetRepository.findById(projetId).ifPresent(stage::setProjet);
        }
        stageRepository.save(stage);

        // 7. Envoyer l'email de confirmation au candidat
        String emailCandidat = demande.getCommentaire() != null
                && demande.getCommentaire().startsWith("Email candidat : ")
                ? demande.getCommentaire().replace("Email candidat : ", "")
                : emailFinal;

        emailService.envoyerConfirmationAdmission(
                emailCandidat,
                demande.getPrenom() + " " + demande.getNom(),
                emailFinal,
                motDePasse
        );

        model.addAttribute("succes", true);
        model.addAttribute("demande", demande);
        model.addAttribute("matricule", matricule);
        model.addAttribute("encadreurs", encadreurRepository.findAll());
        model.addAttribute("services", serviceRepository.findAll());
        model.addAttribute("projets", projetRepository.findAll());

        return "responsable/fiche-stagiaire";

    } catch (Exception e) {
        model.addAttribute("erreur", "Erreur : " + e.getMessage());
        model.addAttribute("demande", demande);
        model.addAttribute("encadreurs", encadreurRepository.findAll());
        model.addAttribute("services", serviceRepository.findAll());
        model.addAttribute("projets", projetRepository.findAll());
        return "responsable/fiche-stagiaire";
    }
}

    @GetMapping("/cloture")
public String afficherCloture(Model model,
                               @RequestParam(required = false) String succes) {
    model.addAttribute("activePage", "cloture");
    List<Stage> tousStages = stageRepository.findAll();
    List<Stage> stagesEnCours = stageRepository
            .findByStatut(Stage.StatutStage.en_cours);
    List<Stage> stagesTermines = stageRepository
            .findByStatut(Stage.StatutStage.termine);
    model.addAttribute("stagesEnCours", stagesEnCours);
    model.addAttribute("stagesTermines", stagesTermines);
    model.addAttribute("tousStages", tousStages);
    model.addAttribute("stagesClotures", stagesTermines.size());
    model.addAttribute("rapportsValides", stagesEnCours.stream()
            .filter(stage -> !documentRepository.findByStageId(stage.getId()).isEmpty()).count());
    model.addAttribute("dossiersComplets", stagesEnCours.stream()
            .filter(stage -> !documentRepository.findByStageId(stage.getId()).isEmpty()
                    && !evaluationRepository.findByStageId(stage.getId()).isEmpty()).count());
    model.addAttribute("attestationsGenerees", archiveRepository.count());
    model.addAttribute("stagesEnAttente", stagesEnCours.stream()
            .filter(stage -> stage.getDateFin() != null && !stage.getDateFin().isAfter(LocalDate.now())).count());
    model.addAttribute("documentsParStage", tousStages.stream().collect(Collectors.toMap(
            Stage::getId, stage -> documentRepository.findByStageId(stage.getId()).size())));
    model.addAttribute("evaluationsParStage", tousStages.stream().collect(Collectors.toMap(
            Stage::getId, stage -> evaluationRepository.findByStageId(stage.getId()).size())));
    if (succes != null) model.addAttribute("succes", succes);
    return "responsable/cloture";
}

@GetMapping("/cloture/detail/{id}")
public String afficherDetailCloture(@PathVariable Integer id, Model model) {
    Optional<Stage> stageOpt = stageRepository.findById(id);
    if (stageOpt.isEmpty()) return "redirect:/responsable/cloture";

    Stage stage = stageOpt.get();
    model.addAttribute("activePage", "cloture");
    boolean rapportDepose = !documentRepository
            .findByStageId(stage.getId()).isEmpty();
    boolean evaluationsPresentes = !evaluationRepository
            .findByStageId(stage.getId()).isEmpty();
    boolean documentsPresents = rapportDepose;

    model.addAttribute("stage", stage);
    model.addAttribute("rapportDepose", rapportDepose);
    model.addAttribute("evaluationsPresentes", evaluationsPresentes);
    model.addAttribute("documentsPresents", documentsPresents);
    return "responsable/cloture-detail";
}

@PostMapping("/cloture/cloturer/{id}")
public String cloturer(@PathVariable Integer id) {
    stageRepository.findById(id).ifPresent(stage -> {
        stage.setStatut(Stage.StatutStage.termine);
        stageRepository.save(stage);
        Archive archive = new Archive(stage, "Cloture normale du stage");
        archiveRepository.save(archive);
    });
    return "redirect:/responsable/cloture?succes=Stage cloture et archive avec succes.";
}
@GetMapping("/affectations")
public String afficherAffectations(Model model) {
    model.addAttribute("activePage", "affectations");
    model.addAttribute("stages", stageRepository.findAll());
    return "responsable/affectations";
}

@GetMapping("/archives")
public String afficherArchives(Model model) {
    model.addAttribute("activePage", "archives");
    model.addAttribute("archives", archiveRepository.findAll());
    return "responsable/archives";
}

@GetMapping("/suivi")
public String afficherSuivi(Model model) {
    List<Stage> stages = stageRepository.findAll();
    model.addAttribute("activePage", "suivi");
    model.addAttribute("stages", stages);
    model.addAttribute("stagesEnCours", stages.stream()
            .filter(stage -> stage.getStatut() == Stage.StatutStage.en_cours).count());
    model.addAttribute("stagesTermines", stages.stream()
            .filter(stage -> stage.getStatut() == Stage.StatutStage.termine).count());
    model.addAttribute("stagesSuspendus", stages.stream()
            .filter(stage -> stage.getStatut() == Stage.StatutStage.suspendu).count());
    return "responsable/suivi";
}

@GetMapping("/notifications")
public String afficherNotifications(Model model) {
    List<Notification> notifications = notificationRepository.findAllByOrderByDateEnvoiDesc();
    model.addAttribute("activePage", "notifications");
    model.addAttribute("notifications", notifications);
    model.addAttribute("notificationsCount", notificationRepository
            .countByDestinataireTypeAndStatut("RESPONSABLE", "non_lue"));
    model.addAttribute("messagesCount", notificationRepository.countByDestinataireType("RESPONSABLE"));
    return "responsable/notifications";
}

       @GetMapping("/stagiaires")
    public String afficherStagiaires(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Stagiaire> stagiaires = stagiaireRepository.findAll();
        List<Stage> stages = stageRepository.findAll();
        
        // Stats de base
        long totalStagiaires = stagiaires.size();
        long enCours = stagiaires.stream().filter(s -> s.getStatut() == Stagiaire.StatutStagiaire.actif).count();
        long clotures = stagiaires.stream().filter(s -> s.getStatut() == Stagiaire.StatutStagiaire.termine).count();
        long affectations = stages.stream().filter(s -> s.getService() != null && s.getEncadreur() != null).count();
        
        // Progression moyenne (depuis champ Stagiaire.progression)
        int progressionMoyenne = (int) stagiaires.stream()
            .filter(s -> s.getProgression() != null)
            .mapToInt(Stagiaire::getProgression)
            .average()
            .orElse(0);
        
        // Notifications / Messages (depuis BDD)
        long notificationsCount = notificationRepository.countByDestinataireTypeAndStatut("RESPONSABLE", "non_lue");
        long messagesCount = notificationRepository.countByDestinataireType("RESPONSABLE");
        
        // Filtres dynamiques : Service
        List<String> servicesList = stages.stream()
            .filter(s -> s.getService() != null)
            .map(s -> s.getService().getNom())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        
        // Filtres dynamiques : Encadreur
        List<String> encadreursList = stages.stream()
            .filter(s -> s.getEncadreur() != null && s.getEncadreur().getUtilisateur() != null)
            .map(s -> s.getEncadreur().getUtilisateur().getPrenom() + " " + s.getEncadreur().getUtilisateur().getNom())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        
        // Profil responsable connecté
        Utilisateur responsable = userDetails.getUtilisateur();
        
        model.addAttribute("activePage", "stagiaires");
        model.addAttribute("stagiaires", stagiaires);
        model.addAttribute("stages", stages);
        model.addAttribute("stageParStagiaire", stages.stream()
                .filter(stage -> stage.getStagiaire() != null)
                .collect(Collectors.toMap(stage -> stage.getStagiaire().getId(), stage -> stage, (first, second) -> first)));
        Map<Integer, Long> objectifsAtteints = new HashMap<>();
        Map<Integer, Long> tachesTerminees = new HashMap<>();
        Map<Integer, Long> livrablesDeposes = new HashMap<>();
        stages.forEach(stage -> {
            objectifsAtteints.put(stage.getId(), objectifRepository.findByStageIdOrderByOrdreAsc(stage.getId()).stream()
                    .filter(objectif -> objectif.getStatut() == Objectif.StatutObjectif.atteint).count());
            tachesTerminees.put(stage.getId(), tacheRepository.findByStageId(stage.getId()).stream()
                    .filter(tache -> tache.getStatut() == Tache.StatutTache.terminee).count());
            livrablesDeposes.put(stage.getId(), (long) livrableRepository.findByStageId(stage.getId()).size());
        });
        model.addAttribute("objectifsAtteints", objectifsAtteints);
        model.addAttribute("tachesTerminees", tachesTerminees);
        model.addAttribute("livrablesDeposes", livrablesDeposes);
        model.addAttribute("totalStagiaires", totalStagiaires);
        model.addAttribute("enCours", enCours);
        model.addAttribute("clotures", clotures);
        model.addAttribute("affectations", affectations);
        model.addAttribute("progressionMoyenne", progressionMoyenne);
        model.addAttribute("nouveauxStagiaires", stagiaires.stream()
                .filter(s -> s.getDateAdmission() != null
                        && !s.getDateAdmission().isBefore(LocalDate.now().minusDays(30)))
                .count());
        model.addAttribute("tauxReussite", totalStagiaires > 0 ? clotures * 100 / totalStagiaires : 0);
        model.addAttribute("livrablesRecents", livrableRepository.findAll().stream()
                .sorted(Comparator.comparing(Livrable::getDateDepot).reversed())
                .limit(5).toList());
        model.addAttribute("notificationsCount", notificationsCount);
        model.addAttribute("messagesCount", messagesCount);
        model.addAttribute("servicesList", servicesList);
        model.addAttribute("encadreursList", encadreursList);
        model.addAttribute("encadreurs", encadreurRepository.findAll());
        model.addAttribute("services", serviceRepository.findAll());
        model.addAttribute("projets", projetRepository.findAll());
        model.addAttribute("responsable", responsable);

        // Échéances des 7 prochains jours
        LocalDate aujourdHui = LocalDate.now();

        List<Stage> echeances = stages.stream()
            .filter(s -> s.getDateFin() != null)
            .sorted(Comparator.comparing(Stage::getDateFin))
            .limit(5)
            .collect(Collectors.toList());

        model.addAttribute("echeances", echeances);

        // Activités récentes
        List<Notification> activitesRecentes =
            notificationRepository.findAllByOrderByDateEnvoiDesc()
            .stream()
            .limit(5)
            .collect(Collectors.toList());

        model.addAttribute("activitesRecentes", activitesRecentes);

        // Répartition par service
        Map<String, Long> repartitionServices =
            stages.stream()
                .filter(s -> s.getService() != null)
                .collect(Collectors.groupingBy(
                    s -> s.getService().getNom(),
                    Collectors.counting()
                ));

        model.addAttribute("repartitionServices", repartitionServices);

        // Affectations par encadreur
        Map<String, Long> affectationsParEncadreur =
            stages.stream()
                .filter(s -> s.getEncadreur() != null)
                .collect(Collectors.groupingBy(
                    s -> s.getEncadreur().getUtilisateur().getPrenom()
                        + " "
                        + s.getEncadreur().getUtilisateur().getNom(),
                    Collectors.counting()
                ));

        model.addAttribute(
            "affectationsParEncadreur",
            affectationsParEncadreur
        );
        return "responsable/stagiaires";

    }

@PostMapping("/stagiaires/affecter/{stageId}")
public String affecterStagiaire(@PathVariable Integer stageId,
                                @RequestParam Integer encadreurId,
                                @RequestParam Integer serviceId,
                                @RequestParam(required = false) Integer projetId,
                                @RequestParam String dateDebut,
                                @RequestParam String dateFin,
                                RedirectAttributes redirectAttributes) {
    Optional<Stage> stageOpt = stageRepository.findById(stageId);
    Optional<Encadreur> encadreurOpt = encadreurRepository.findById(encadreurId);
    Optional<ServiceEntreprise> serviceOpt = serviceRepository.findById(serviceId);
    if (stageOpt.isEmpty() || encadreurOpt.isEmpty() || serviceOpt.isEmpty()) {
        redirectAttributes.addFlashAttribute("erreur", "Affectation impossible : informations incomplètes.");
        return "redirect:/responsable/stagiaires";
    }
    LocalDate debut = LocalDate.parse(dateDebut);
    LocalDate fin = LocalDate.parse(dateFin);
    if (fin.isBefore(debut)) {
        redirectAttributes.addFlashAttribute("erreur", "La date de fin doit être postérieure à la date de début.");
        return "redirect:/responsable/stagiaires";
    }
    Stage stage = stageOpt.get();
    stage.setEncadreur(encadreurOpt.get());
    stage.setService(serviceOpt.get());
    stage.setDateDebut(debut);
    stage.setDateFin(fin);
    stage.setDuree(java.time.temporal.ChronoUnit.DAYS.between(debut, fin) + " jours");
    if (projetId == null) {
        stage.setProjet(null);
    } else {
        projetRepository.findById(projetId).ifPresent(stage::setProjet);
    }
    stageRepository.save(stage);
    redirectAttributes.addFlashAttribute("succes", "Le stage a été affecté avec succès.");
    return "redirect:/responsable/stagiaires";
}

@GetMapping("/dossiers")
public String afficherDossiers(Model model) {
    List<Document> documents = documentRepository.findAll();
    List<Stage> stages = stageRepository.findAll();
    Map<Integer, Long> documentsParStage = stages.stream().collect(Collectors.toMap(
            Stage::getId, stage -> (long) documentRepository.findByStageId(stage.getId()).size()));
    long complets = documentsParStage.values().stream().filter(total -> total >= 5).count();
    model.addAttribute("activePage", "dossiers");
    model.addAttribute("documents", documents);
    model.addAttribute("stages", stages);
    model.addAttribute("documentsParStage", documentsParStage);
    model.addAttribute("dossiersTotaux", stages.size());
    model.addAttribute("dossiersComplets", complets);
    model.addAttribute("dossiersIncomplets", Math.max(0, stages.size() - complets));
    model.addAttribute("dossiersArchives", archiveRepository.count());
    model.addAttribute("documentsEnAttente", documents.stream()
            .filter(document -> !"valide".equalsIgnoreCase(document.getStatut())
                    && !"disponible".equalsIgnoreCase(document.getStatut())).count());
    model.addAttribute("derniersDepots", documents.stream()
            .sorted(Comparator.comparing(Document::getDateDepot).reversed())
            .limit(5).toList());
    return "responsable/dossiers";
}

@GetMapping("/planning")
public String afficherPlanning(Model model) {
    List<Stage> stages = stageRepository.findAll();
    List<EvenementPersonnel> evenements = stages.stream()
            .flatMap(stage -> evenementPersonnelRepository.findByStageId(stage.getId()).stream())
            .sorted(Comparator.comparing(EvenementPersonnel::getDate))
            .toList();
    model.addAttribute("activePage", "planning");
    model.addAttribute("stages", stages);
    model.addAttribute("evenements", evenements);
    model.addAttribute("evenementsPlanifies", evenements.size());
    model.addAttribute("reunionsProgrammees", evenements.stream()
            .filter(event -> "reunion".equals(event.getTypeCouleur())).count());
    model.addAttribute("debutsStage", stages.stream()
            .filter(stage -> stage.getDateDebut() != null && stage.getDateDebut().getMonth() == LocalDate.now().getMonth()).count());
    model.addAttribute("finsStage", stages.stream()
            .filter(stage -> stage.getDateFin() != null && stage.getDateFin().getMonth() == LocalDate.now().getMonth()).count());
    model.addAttribute("echeancesAVenir", evenements.stream()
            .filter(event -> event.getDate() != null && !event.getDate().isBefore(LocalDate.now())).count());
    model.addAttribute("activitesDuMois", evenements.stream()
            .filter(event -> event.getDate() != null
                    && event.getDate().getMonth() == LocalDate.now().getMonth()
                    && event.getDate().getYear() == LocalDate.now().getYear()).count());
    return "responsable/planning";
}

@PostMapping("/planning/evenements/ajouter")
public String ajouterEvenementPlanning(@RequestParam Integer stageId,
                                       @RequestParam String titre,
                                       @RequestParam String type,
                                       @RequestParam String date,
                                       @RequestParam(required = false) String heure,
                                       @RequestParam(required = false) String lieu,
                                       @RequestParam(required = false) String description,
                                       @RequestParam(defaultValue = "1_jour") String rappel,
                                       RedirectAttributes redirectAttributes) {
    stageRepository.findById(stageId).ifPresent(stage -> {
        EvenementPersonnel evenement = new EvenementPersonnel(stage, titre.trim(), LocalDate.parse(date), type);
        if (heure != null && !heure.isBlank()) evenement.setHeure(java.time.LocalTime.parse(heure));
        evenement.setLieu(lieu);
        evenement.setDescription(description);
        evenement.setRappel(rappel);
        evenementPersonnelRepository.save(evenement);
    });
    redirectAttributes.addFlashAttribute("succes", "L'événement a été ajouté au planning.");
    return "redirect:/responsable/planning";
}

@GetMapping("/profil")
public String afficherProfil(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    Utilisateur responsable = userDetails.getUtilisateur();
    List<Stage> stages = stageRepository.findAll();
    model.addAttribute("activePage", "profil");
    model.addAttribute("responsable", responsable);
    model.addAttribute("stagiairesSuivis", stagiaireRepository.count());
    model.addAttribute("stagesGeres", stages.size());
    model.addAttribute("stagesClotures", stages.stream()
            .filter(stage -> stage.getStatut() == Stage.StatutStage.termine).count());
    model.addAttribute("anciennete", responsable.getDateCreation() == null ? 1 : Math.max(1,
            java.time.temporal.ChronoUnit.YEARS.between(
                    responsable.getDateCreation().toLocalDate(), LocalDate.now()) + 1));
    model.addAttribute("notificationsCount", notificationRepository
            .countByDestinataireTypeAndStatut("RESPONSABLE", "non_lue"));
    model.addAttribute("messagesCount", notificationRepository.countByDestinataireType("RESPONSABLE"));
    return "responsable/profil";
}

@PostMapping("/profil/modifier")
public String modifierProfil(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @RequestParam String nom,
                             @RequestParam String prenom,
                             @RequestParam String email,
                             @RequestParam(required = false) String telephone,
                             @RequestParam(required = false) String adresse,
                             RedirectAttributes redirectAttributes) {
    Utilisateur responsable = userDetails.getUtilisateur();
    responsable.setNom(nom.trim());
    responsable.setPrenom(prenom.trim());
    responsable.setEmail(email.trim().toLowerCase());
    responsable.setTelephone(telephone);
    responsable.setAdresse(adresse);
    utilisateurRepository.save(responsable);
    redirectAttributes.addFlashAttribute("succes", "Votre profil a été mis à jour.");
    return "redirect:/responsable/profil";
}

private String emailDemande(DemandeStage demande) {
    if (demande.getEmail() != null && !demande.getEmail().isBlank()) {
        return demande.getEmail().trim().toLowerCase();
    }
    String commentaire = demande.getCommentaire();
    String prefixe = "Email candidat : ";
    if (commentaire != null && commentaire.startsWith(prefixe)) {
        String email = commentaire.substring(prefixe.length()).trim().toLowerCase();
        return email.contains("@") ? email : null;
    }
    return null;
}

private void ajouterRetourEmail(RedirectAttributes redirectAttributes,
                                boolean envoye,
                                String destinataire,
                                String operation) {
    if (destinataire == null || destinataire.isBlank()) {
        redirectAttributes.addFlashAttribute("erreur",
                operation + " Aucune adresse e-mail fiable n'est enregistrée pour ce candidat.");
    } else if (envoye) {
        redirectAttributes.addFlashAttribute("succes",
                operation + " Notification envoyée à " + destinataire + ".");
    } else {
        redirectAttributes.addFlashAttribute("erreur",
                operation + " L'e-mail n'a pas pu être envoyé à " + destinataire
                        + ". Vérifiez la configuration SMTP.");
    }
}

}
