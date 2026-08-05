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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import com.gestionstages.gestion_stages.security.CustomUserDetails;
import com.gestionstages.gestion_stages.services.ActivityLogService;
import jakarta.servlet.http.HttpSession;
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
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ResponsablePreferenceRepository responsablePreferenceRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ActivityLogService activityLogService;
    private final SessionRegistry sessionRegistry;
    private final com.gestionstages.gestion_stages.services.MessagingService messagingService;
    
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

private static final Locale LOCALE_FR = Locale.FRENCH;

private static List<YearMonth> sixDerniersMois() {
    YearMonth courant = YearMonth.now();
    List<YearMonth> mois = new ArrayList<>();
    for (int i = 5; i >= 0; i--) mois.add(courant.minusMonths(i));
    return mois;
}

private static List<String> libellesMois(List<YearMonth> mois) {
    return mois.stream().map(m -> {
        String nom = m.getMonth().getDisplayName(TextStyle.SHORT, LOCALE_FR).replace(".", "");
        return nom.substring(0, 1).toUpperCase(LOCALE_FR) + nom.substring(1);
    }).toList();
}

@ModelAttribute
public void ajouterDonneesCommunes(Model model, Authentication authentication) {
    LocalDate aujourdHui = LocalDate.now();
    model.addAttribute("dateCourante", aujourdHui);
    model.addAttribute("moisCourant", aujourdHui.getMonth().getDisplayName(TextStyle.FULL, LOCALE_FR));
    model.addAttribute("moisAnneeCourant",
            aujourdHui.getMonth().getDisplayName(TextStyle.FULL, LOCALE_FR) + " " + aujourdHui.getYear());
    model.addAttribute("notificationsCount",
            notificationRepository.countByDestinataireTypeAndStatut("RESPONSABLE", "non_lue"));
    long messagesCount = 0;
    if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details) {
        Integer userId = details.getUtilisateur().getId();
        messagesCount = conversationRepository.findByParticipantIdOrderByDernierMessageDesc(userId)
                .stream()
                .mapToLong(conversation -> conversationRepository
                        .countNonLuByConversation(conversation.getId(), userId))
                .sum();
    }
    model.addAttribute("messagesCount", messagesCount);
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
                              JavaMailSender mailSender,
                              ConversationRepository conversationRepository,
                              MessageRepository messageRepository,
                              ResponsablePreferenceRepository responsablePreferenceRepository,
                              ActivityLogRepository activityLogRepository,
                              ActivityLogService activityLogService,
                              SessionRegistry sessionRegistry,
                              com.gestionstages.gestion_stages.services.MessagingService messagingService) {
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
    this.conversationRepository = conversationRepository;
    this.messageRepository = messageRepository;
    this.responsablePreferenceRepository = responsablePreferenceRepository;
    this.activityLogRepository = activityLogRepository;
    this.activityLogService = activityLogService;
    this.sessionRegistry = sessionRegistry;
    this.messagingService = messagingService;
}

        @GetMapping("/dashboard")
    public String afficherDashboard(Model model) {
        model.addAttribute("activePage", "dashboard");
        List<DemandeStage> demandes = demandeStageRepository.findAll();
        LocalDate aujourdHui = LocalDate.now();

        long demandesEnAttente = demandes.stream()
                .filter(d -> d.getStatut() == DemandeStage.StatutDemande.en_attente).count();
        long acceptees = demandes.stream()
                .filter(d -> d.getStatut() == DemandeStage.StatutDemande.acceptee).count();
        long refusees = demandes.stream()
                .filter(d -> d.getStatut() == DemandeStage.StatutDemande.refusee).count();
        long totalDemandes = demandes.size();

        List<Stagiaire> stagiaires = stagiaireRepository.findAll();
        List<Stage> stages = stageRepository.findAll();
        boolean statutsModifies = false;
        for (Stage stage : stages) {
            if (stage.getDateFin() != null && stage.getDateFin().isBefore(aujourdHui)
                    && stage.getStatut() != Stage.StatutStage.termine) {
                stage.setStatut(Stage.StatutStage.termine);
                if (stage.getStagiaire() != null) {
                    stage.getStagiaire().setStatut(Stagiaire.StatutStagiaire.termine);
                    stagiaireRepository.save(stage.getStagiaire());
                }
                statutsModifies = true;
            }
        }
        if (statutsModifies) {
            stageRepository.saveAll(stages);
        }
        long totalStagiaires = stagiaires.size();
        long totalStages = stages.size();
        long totalStagiairesActifs = stagiaires.stream()
                .filter(stagiaire -> stagiaire.getStatut() == Stagiaire.StatutStagiaire.actif).count();
        long stagesEnCours = stages.stream()
                .filter(stage -> stage.getStatut() == Stage.StatutStage.en_cours).count();
        long stagesTermines = stages.stream()
                .filter(stage -> stage.getStatut() == Stage.StatutStage.termine).count();
        long stagesEnAttente = stages.stream()
                .filter(stage -> stage.getStatut() == Stage.StatutStage.suspendu).count();

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
            m.put("joursRestants", Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(aujourdHui, s.getDateFin())));
            return m;
        }).toList();

        long tauxReussite = totalStages > 0 ? (stagesTermines * 100 / totalStages) : 0;
        int progressionMoyenne = stagiaires.isEmpty() ? 0 : (int) Math.round(stagiaires.stream()
                .map(Stagiaire::getProgression)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average().orElse(0));

        Map<String, Long> repartition = stagiaires.stream()
                .collect(Collectors.groupingBy(
                        s -> (s.getDemandeStage() != null && s.getDemandeStage().getFiliere() != null)
                                ? s.getDemandeStage().getFiliere() : "Autre",
                        Collectors.counting()));
        List<String> filiereLabels = new ArrayList<>(repartition.keySet());
        List<Long> filiereData = new ArrayList<>(repartition.values());

        List<Notification> activitesRecentes = notificationRepository.findAllByOrderByDateEnvoiDesc().stream().limit(6).toList();

        List<YearMonth> sixMois = sixDerniersMois();
        List<String> moisLabels = libellesMois(sixMois);
        List<Long> dataDemandes = sixMois.stream().map(m -> demandes.stream()
                .filter(d -> d.getDateDemande() != null && YearMonth.from(d.getDateDemande()).equals(m)).count()).toList();
        List<Long> dataStagesEnCours = sixMois.stream().map(m -> stages.stream()
                .filter(s -> s.getDateDebut() != null && !YearMonth.from(s.getDateDebut()).isAfter(m)
                        && (s.getDateFin() == null || !YearMonth.from(s.getDateFin()).isBefore(m))).count()).toList();
        List<Long> dataStagesTermines = sixMois.stream().map(m -> stages.stream()
                .filter(s -> s.getStatut() == Stage.StatutStage.termine && s.getDateFin() != null
                        && YearMonth.from(s.getDateFin()).equals(m)).count()).toList();

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
        model.addAttribute("stagesEnAttente", stagesEnAttente);
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
        model.addAttribute("activitesRecentes", activitesRecentes);
        model.addAttribute("stagiaires", stagiaires.stream().limit(6).toList());
        model.addAttribute("stages", stages);
        model.addAttribute("dernieresDemandes", demandes.stream()
                .sorted(Comparator.comparing(DemandeStage::getDateDemande).reversed())
                .limit(5).toList());

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
        long demandesTraitees = acceptees + refusees;
        long tempsMoyenTraitement = demandesTraitees == 0 ? 0 : Math.round(demandes.stream()
                .filter(d -> d.getStatut() != DemandeStage.StatutDemande.en_attente && d.getDateDemande() != null)
                .mapToLong(d -> Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(
                        d.getDateDemande().toLocalDate(), LocalDate.now())))
                .average().orElse(0));
        List<YearMonth> sixMois = sixDerniersMois();

        model.addAttribute("demandes", demandes);
        Map<Integer, List<Document>> documentsParDemande = demandes.stream().collect(Collectors.toMap(
                DemandeStage::getId,
                demande -> documentRepository.findByDemandeStageId(demande.getId())));
        model.addAttribute("documentsParDemande", documentsParDemande);
        model.addAttribute("totalDemandes", total);
        model.addAttribute("enAttente", enAttente);
        model.addAttribute("acceptees", acceptees);
        model.addAttribute("refusees", refusees);
        model.addAttribute("aujourdHui", aujourdHui);
        model.addAttribute("tauxAcceptation", taux);
        model.addAttribute("tempsMoyenTraitement", tempsMoyenTraitement);
        model.addAttribute("demandesRecentes", demandes.stream()
                .sorted(Comparator.comparing(DemandeStage::getDateDemande).reversed())
                .limit(5).toList());
        model.addAttribute("moisLabelsJson", toJson(libellesMois(sixMois)));
        model.addAttribute("demandesMoisJson", toJson(sixMois.stream().map(m -> demandes.stream()
                .filter(d -> d.getDateDemande() != null && YearMonth.from(d.getDateDemande()).equals(m)).count()).toList()));
        model.addAttribute("statutsDemandesJson", toJson(List.of(acceptees, enAttente, refusees)));
        return "responsable/demandes";
    }

    @PostMapping("/demandes/{id}/accepter")
    public String accepterDemande(@PathVariable Integer id,
                                  @RequestParam String emailConnexion,
                                  @RequestParam String motDePasseTemporaire,
                                  RedirectAttributes redirectAttributes) {
        Optional<DemandeStage> demandeOpt = demandeStageRepository.findById(id);
        if (demandeOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("erreur", "La demande sélectionnée est introuvable.");
            return "redirect:/responsable/demandes";
        }

        DemandeStage demande = demandeOpt.get();
        String emailCompte = emailConnexion == null ? "" : emailConnexion.trim().toLowerCase();
        String motDePasse = motDePasseTemporaire == null ? "" : motDePasseTemporaire.trim();

        if (!emailCompte.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            redirectAttributes.addFlashAttribute("erreur", "L'adresse e-mail de connexion n'est pas valide.");
            return "redirect:/responsable/demandes";
        }
        if (motDePasse.length() < 8) {
            redirectAttributes.addFlashAttribute("erreur", "Le mot de passe temporaire doit contenir au moins 8 caractères.");
            return "redirect:/responsable/demandes";
        }
        if (stagiaireRepository.findByDemandeStageId(id).isPresent()) {
            redirectAttributes.addFlashAttribute("erreur", "Un compte stagiaire existe déjà pour cette demande.");
            return "redirect:/responsable/demandes";
        }
        if (utilisateurRepository.existsByEmail(emailCompte)) {
            redirectAttributes.addFlashAttribute("erreur", "Cette adresse e-mail est déjà utilisée par un autre compte.");
            return "redirect:/responsable/demandes";
        }

        try {
            Role roleStagiaire = roleRepository.findByLibelle("STAGIAIRE")
                    .orElseThrow(() -> new IllegalStateException("Le rôle STAGIAIRE est introuvable."));
            Utilisateur utilisateur = new Utilisateur(
                    roleStagiaire,
                    demande.getNom(),
                    demande.getPrenom(),
                    emailCompte,
                    passwordEncoder.encode(motDePasse));
            utilisateurRepository.save(utilisateur);

            String matricule = prochainMatriculeStagiaire();
            Stagiaire stagiaire = new Stagiaire(utilisateur, demande, matricule, LocalDate.now());
            stagiaireRepository.save(stagiaire);

            demande.setStatut(DemandeStage.StatutDemande.acceptee);
            demandeStageRepository.save(demande);

            String destinataire = emailDemande(demande);
            if (destinataire == null || destinataire.isBlank()) destinataire = emailCompte;
            boolean envoye = emailService.envoyerConfirmationAdmission(
                    destinataire,
                    demande.getPrenom() + " " + demande.getNom(),
                    emailCompte,
                    motDePasse);
            ajouterRetourEmail(redirectAttributes, envoye, destinataire,
                    "La demande a été acceptée et le compte stagiaire créé.");
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("erreur",
                    "La création du compte a échoué : " + exception.getMessage());
        }
        return "redirect:/responsable/demandes";
    }

    @PostMapping("/demandes/{id}/refuser")
    public String refuserDemande(@PathVariable Integer id,
                                 @RequestParam String motifRefus,
                                 RedirectAttributes redirectAttributes) {
        String motif = motifRefus == null ? "" : motifRefus.trim();
        if (motif.length() < 10) {
            redirectAttributes.addFlashAttribute("erreur",
                    "Le motif du refus doit contenir au moins 10 caractères.");
            return "redirect:/responsable/demandes";
        }
        Optional<DemandeStage> demandeOpt = demandeStageRepository.findById(id);
        if (demandeOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("erreur", "La demande sélectionnée est introuvable.");
            return "redirect:/responsable/demandes";
        }
        DemandeStage demande = demandeOpt.get();
        demande.setMotifRefus(motif);
        demande.setStatut(DemandeStage.StatutDemande.refusee);
        demandeStageRepository.save(demande);
        String destinataire = emailDemande(demande);
        boolean envoye = emailService.envoyerRefusDemande(
                destinataire,
                demande.getPrenom() + " " + demande.getNom(),
                motif);
        ajouterRetourEmail(redirectAttributes, envoye, destinataire,
                "La demande a été refusée.");
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
        Optional<Stagiaire> stagiaireExistant = stagiaireRepository.findByDemandeStageId(id);
        Stagiaire stagiaire;
        String emailFinal;
        String motDePasse = null;
        boolean compteCreePendantAdmission = stagiaireExistant.isEmpty();

        if (stagiaireExistant.isPresent()) {
            stagiaire = stagiaireExistant.get();
            emailFinal = stagiaire.getUtilisateur().getEmail();
        } else {
            String emailBase = demande.getPrenom().toLowerCase()
                    + "." + demande.getNom().toLowerCase()
                    + "@stagiaire.com";
            emailFinal = emailBase;
            int compteur = 1;
            while (utilisateurRepository.existsByEmail(emailFinal)) {
                emailFinal = demande.getPrenom().toLowerCase()
                        + "." + demande.getNom().toLowerCase()
                        + compteur + "@stagiaire.com";
                compteur++;
            }

            motDePasse = "stag" + LocalDate.now().getYear();
            Role roleStagiaire = roleRepository.findByLibelle("STAGIAIRE").orElseThrow();
            Utilisateur utilisateur = new Utilisateur(roleStagiaire,
                    demande.getNom(), demande.getPrenom(),
                    emailFinal, passwordEncoder.encode(motDePasse));
            utilisateurRepository.save(utilisateur);

            stagiaire = new Stagiaire(utilisateur, demande,
                    prochainMatriculeStagiaire(), LocalDate.now());
            stagiaireRepository.save(stagiaire);
        }
        String matricule = stagiaire.getMatricule();

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

        if (compteCreePendantAdmission) {
            emailService.envoyerConfirmationAdmission(
                    emailCandidat,
                    demande.getPrenom() + " " + demande.getNom(),
                    emailFinal,
                    motDePasse
            );
        }

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
    model.addAttribute("tauxCloture", tousStages.isEmpty() ? 0 : (stagesTermines.size() * 100 / tousStages.size()));
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
    List<YearMonth> sixMois = sixDerniersMois();
    model.addAttribute("clotureMoisLabelsJson", toJson(libellesMois(sixMois)));
    model.addAttribute("cloturesMoisJson", toJson(sixMois.stream().map(m -> stagesTermines.stream()
            .filter(stage -> stage.getDateFin() != null && YearMonth.from(stage.getDateFin()).equals(m)).count()).toList()));
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
        List<YearMonth> sixMois = sixDerniersMois();
        model.addAttribute("stagiairesMoisLabelsJson", toJson(libellesMois(sixMois)));
        model.addAttribute("admissionsMoisJson", toJson(sixMois.stream().map(m -> stagiaires.stream()
                .filter(s -> s.getDateAdmission() != null && YearMonth.from(s.getDateAdmission()).equals(m)).count()).toList()));
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

@GetMapping("/stagiaires/export")
public ResponseEntity<byte[]> exporterStagiairesExcel() {
    Map<Integer, Stage> stagesParStagiaire = stageRepository.findAll().stream()
            .filter(stage -> stage.getStagiaire() != null)
            .collect(Collectors.toMap(
                    stage -> stage.getStagiaire().getId(),
                    stage -> stage,
                    (premier, second) -> premier));
    StringBuilder xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\"?>")
            .append("<?mso-application progid=\"Excel.Sheet\"?>")
            .append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\" ")
            .append("xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">")
            .append("<Worksheet ss:Name=\"Stagiaires\"><Table>");
    ajouterLigneExcel(xml, List.of(
            "Stagiaire", "Matricule", "Service", "Encadreur",
            "Début", "Fin", "Progression", "Statut"));
    for (Stagiaire stagiaire : stagiaireRepository.findAll()) {
        Stage stage = stagesParStagiaire.get(stagiaire.getId());
        ajouterLigneExcel(xml, List.of(
                stagiaire.getUtilisateur().getPrenom() + " " + stagiaire.getUtilisateur().getNom(),
                stagiaire.getMatricule(),
                stage != null && stage.getService() != null ? stage.getService().getNom() : "Non affecté",
                stage != null && stage.getEncadreur() != null
                        ? stage.getEncadreur().getUtilisateur().getPrenom() + " "
                            + stage.getEncadreur().getUtilisateur().getNom()
                        : "Non affecté",
                stage != null && stage.getDateDebut() != null ? stage.getDateDebut().toString() : "",
                stage != null && stage.getDateFin() != null ? stage.getDateFin().toString() : "",
                (stagiaire.getProgression() == null ? 0 : stagiaire.getProgression()) + "%",
                stage != null ? stage.getStatut().name() : stagiaire.getStatut().name()));
    }
    xml.append("</Table></Worksheet></Workbook>");
    byte[] contenu = xml.toString().getBytes(StandardCharsets.UTF_8);
    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"stagiaires-" + LocalDate.now() + ".xls\"")
            .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
            .contentLength(contenu.length)
            .body(contenu);
}

private void ajouterLigneExcel(StringBuilder xml, List<String> valeurs) {
    xml.append("<Row>");
    valeurs.forEach(valeur -> xml.append("<Cell><Data ss:Type=\"String\">")
            .append(echapperXml(valeur))
            .append("</Data></Cell>"));
    xml.append("</Row>");
}

private String echapperXml(String valeur) {
    return valeur == null ? "" : valeur
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
}

@PostMapping("/stagiaires/affecter/{stagiaireId}")
public String affecterStagiaire(@PathVariable Integer stagiaireId,
                                @RequestParam Integer encadreurId,
                                @RequestParam Integer serviceId,
                                @RequestParam(required = false) Integer projetId,
                                @RequestParam String dateDebut,
                                @RequestParam String dateFin,
                                RedirectAttributes redirectAttributes) {
    Optional<Stagiaire> stagiaireOpt = stagiaireRepository.findById(stagiaireId);
    Optional<Encadreur> encadreurOpt = encadreurRepository.findById(encadreurId);
    Optional<ServiceEntreprise> serviceOpt = serviceRepository.findById(serviceId);
    if (stagiaireOpt.isEmpty() || encadreurOpt.isEmpty() || serviceOpt.isEmpty()) {
        redirectAttributes.addFlashAttribute("erreur", "Affectation impossible : informations incomplètes.");
        return "redirect:/responsable/stagiaires";
    }
    LocalDate debut = LocalDate.parse(dateDebut);
    LocalDate fin = LocalDate.parse(dateFin);
    if (fin.isBefore(debut)) {
        redirectAttributes.addFlashAttribute("erreur", "La date de fin doit être postérieure à la date de début.");
        return "redirect:/responsable/stagiaires";
    }
    Stage stage = stageRepository.findByStagiaireId(stagiaireId).orElseGet(() -> {
        Stage nouveauStage = new Stage();
        nouveauStage.setStagiaire(stagiaireOpt.get());
        nouveauStage.setNumeroStage("STAGE-" + LocalDate.now().getYear() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        nouveauStage.setStatut(Stage.StatutStage.en_cours);
        return nouveauStage;
    });
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
    List<YearMonth> sixMois = sixDerniersMois();
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
    model.addAttribute("dossiersMoisLabelsJson", toJson(libellesMois(sixMois)));
    model.addAttribute("documentsMoisJson", toJson(sixMois.stream().map(m -> documents.stream()
            .filter(d -> d.getDateDepot() != null && YearMonth.from(d.getDateDepot()).equals(m)).count()).toList()));
    return "responsable/dossiers";
}

@PostMapping("/dossiers/importer")
public String importerDocumentExterne(@RequestParam Integer stageId,
                                      @RequestParam String typeDocument,
                                      @RequestParam MultipartFile fichier,
                                      RedirectAttributes redirectAttributes) {
    Optional<Stage> stageOpt = stageRepository.findById(stageId);
    if (stageOpt.isEmpty()) {
        redirectAttributes.addFlashAttribute("erreur", "Le dossier sélectionné est introuvable.");
        return "redirect:/responsable/dossiers";
    }
    if (fichier == null || fichier.isEmpty()) {
        redirectAttributes.addFlashAttribute("erreur", "Sélectionnez un fichier à importer.");
        return "redirect:/responsable/dossiers";
    }
    try {
        Path dossier = Paths.get("uploads", "documents").toAbsolutePath().normalize();
        Files.createDirectories(dossier);
        String original = Paths.get(fichier.getOriginalFilename() == null
                ? "document" : fichier.getOriginalFilename()).getFileName().toString();
        Path cible = dossier.resolve(UUID.randomUUID() + "_" + original).normalize();
        if (!cible.startsWith(dossier)) throw new IllegalArgumentException("Chemin de fichier invalide.");
        Files.copy(fichier.getInputStream(), cible, StandardCopyOption.REPLACE_EXISTING);

        Document document = new Document(original, typeDocument.trim(), cible.toString());
        document.setStage(stageOpt.get());
        document.setTailleOctets(fichier.getSize());
        document.setStatut("disponible");
        documentRepository.save(document);
        redirectAttributes.addFlashAttribute("succes", "Le document externe a été importé.");
    } catch (Exception exception) {
        redirectAttributes.addFlashAttribute("erreur",
                "L'import du document a échoué : " + exception.getMessage());
    }
    return "redirect:/responsable/dossiers";
}

@GetMapping("/dossiers/documents/{id}")
public String consulterDocumentDossier(@PathVariable Integer id,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
    Optional<Document> document = documentRepository.findById(id);
    if (document.isEmpty() || document.get().getStage() == null) {
        redirectAttributes.addFlashAttribute("erreur", "Le document demandé est introuvable.");
        return "redirect:/responsable/dossiers";
    }
    model.addAttribute("activePage", "dossiers");
    model.addAttribute("document", document.get());
    return "responsable/document-detail";
}

@PostMapping("/dossiers/documents/{id}/statut")
public String modifierStatutDocument(@PathVariable Integer id,
                                     @RequestParam String statut,
                                     RedirectAttributes redirectAttributes) {
    List<String> statutsAutorises = List.of("disponible", "en_validation", "valide", "a_corriger", "rejete");
    if (!statutsAutorises.contains(statut)) {
        redirectAttributes.addFlashAttribute("erreur", "Le statut sélectionné n'est pas valide.");
        return "redirect:/responsable/dossiers/documents/" + id;
    }
    documentRepository.findById(id).ifPresent(document -> {
        document.setStatut(statut);
        documentRepository.save(document);
    });
    redirectAttributes.addFlashAttribute("succes", "Le statut du document a été mis à jour.");
    return "redirect:/responsable/dossiers/documents/" + id;
}

@PostMapping("/dossiers/documents/{id}/commenter")
public String commenterDocumentDossier(@PathVariable Integer id,
                                       @RequestParam String commentaire,
                                       @AuthenticationPrincipal CustomUserDetails userDetails,
                                       RedirectAttributes redirectAttributes) {
    documentRepository.findById(id).ifPresent(document -> {
        String auteur = userDetails.getUtilisateur().getPrenom() + " "
                + userDetails.getUtilisateur().getNom();
        String nouveauCommentaire = auteur + " : " + commentaire.trim();
        String historique = document.getDescriptionModifications();
        document.setDescriptionModifications(
                historique == null || historique.isBlank()
                        ? nouveauCommentaire
                        : historique + "\n" + nouveauCommentaire);
        documentRepository.save(document);
    });
    redirectAttributes.addFlashAttribute("succes", "Le commentaire a été enregistré.");
    return "redirect:/responsable/dossiers/documents/" + id;
}

@GetMapping("/planning")
public String afficherPlanning(Model model) {
    List<Stage> stages = stageRepository.findAll();
    List<EvenementPersonnel> evenements = evenementPersonnelRepository.findAll().stream()
            .sorted(Comparator.comparing(EvenementPersonnel::getDate))
            .toList();
    LocalDate aujourdHui = LocalDate.now();
    LocalDate finSemaine = aujourdHui.plusDays(6);
    long evenementsPlanifies = evenements.stream()
            .filter(event -> event.getDate() != null && !event.getDate().isBefore(aujourdHui)).count();
    long reunionsProgrammees = evenements.stream()
            .filter(event -> "reunion".equals(event.getTypeCouleur()) && event.getDate() != null
                    && !event.getDate().isBefore(aujourdHui)).count();
    long soutenancesProgrammees = evenements.stream()
            .filter(event -> "soutenance".equals(event.getTypeCouleur()) && event.getDate() != null
                    && !event.getDate().isBefore(aujourdHui)).count();
    long echeancesSemaine = evenements.stream()
            .filter(event -> event.getDate() != null && !event.getDate().isBefore(aujourdHui)
                    && !event.getDate().isAfter(finSemaine)).count();
    long stagesPlanifies = stages.stream().filter(stage -> stage.getDateFin() != null).count();
    long stagesDansDelais = stages.stream().filter(stage -> stage.getDateFin() != null
            && (stage.getStatut() == Stage.StatutStage.termine || !stage.getDateFin().isBefore(aujourdHui))).count();
    long tauxRespectPlanning = stagesPlanifies == 0 ? 100 : stagesDansDelais * 100 / stagesPlanifies;
    model.addAttribute("activePage", "planning");
    model.addAttribute("stages", stages);
    model.addAttribute("evenements", evenements);
    model.addAttribute("evenementsPlanifies", evenementsPlanifies);
    model.addAttribute("reunionsProgrammees", reunionsProgrammees);
    model.addAttribute("soutenancesProgrammees", soutenancesProgrammees);
    model.addAttribute("echeancesSemaine", echeancesSemaine);
    model.addAttribute("tauxRespectPlanning", tauxRespectPlanning);
    model.addAttribute("planningAnnee", aujourdHui.getYear());
    model.addAttribute("planningMoisIndex", aujourdHui.getMonthValue() - 1);
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
public String ajouterEvenementPlanning(@RequestParam(defaultValue = "certains") String audience,
                                       @RequestParam(required = false) List<Integer> stageIds,
                                       @RequestParam String titre,
                                       @RequestParam String type,
                                       @RequestParam String date,
                                       @RequestParam(required = false) String heure,
                                       @RequestParam(required = false) String lieu,
                                       @RequestParam(required = false) String description,
                                       @RequestParam(defaultValue = "1_jour") String rappel,
                                       RedirectAttributes redirectAttributes) {
    List<Stage> stagesSelectionnes = switch (audience) {
        case "tous" -> stageRepository.findAll();
        case "aucun" -> List.of();
        default -> stageIds == null ? List.of() : stageIds.stream()
                .map(stageRepository::findById)
                .flatMap(Optional::stream)
                .distinct()
                .toList();
    };
    Stage stageReference = stagesSelectionnes.stream().findFirst().orElse(null);
    EvenementPersonnel evenement =
            new EvenementPersonnel(stageReference, titre.trim(), LocalDate.parse(date), type);
    if (heure != null && !heure.isBlank()) evenement.setHeure(java.time.LocalTime.parse(heure));
    evenement.setLieu(lieu);
    evenement.setDescription(description);
    evenement.setRappel(rappel);
    evenementPersonnelRepository.save(evenement);

    if (!"aucun".equals(audience)) {
        List<Utilisateur> destinataires = new ArrayList<>();
        stagesSelectionnes.forEach(stage -> {
            if (stage.getStagiaire() != null && stage.getStagiaire().getUtilisateur() != null) {
                destinataires.add(stage.getStagiaire().getUtilisateur());
            }
            if (stage.getEncadreur() != null && stage.getEncadreur().getUtilisateur() != null) {
                destinataires.add(stage.getEncadreur().getUtilisateur());
            }
        });
        if ("tous".equals(audience)) {
            destinataires.addAll(utilisateurRepository.findByRole_Libelle("ADMINISTRATEUR"));
        }
        destinataires.stream()
                .filter(utilisateur -> utilisateur.getEmail() != null)
                .collect(Collectors.toMap(Utilisateur::getEmail, utilisateur -> utilisateur, (a, b) -> a))
                .values()
                .forEach(utilisateur -> {
                    Notification notification = new Notification(
                            "Nouvelle activité planifiée",
                            titre.trim() + " le " + LocalDate.parse(date)
                                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            utilisateur.getRole().getLibelle(),
                            "normale",
                            "Responsable des stages");
                    notification.setDestinataireEmail(utilisateur.getEmail());
                    notification.setStatut("non_lue");
                    notificationRepository.save(notification);
                });
    }
    redirectAttributes.addFlashAttribute("succes", "L'événement a été ajouté au planning.");
    return "redirect:/responsable/planning";
}

@GetMapping("/messages")
public String afficherMessages(@AuthenticationPrincipal CustomUserDetails userDetails,
                               @RequestParam(required = false) Integer convId,
                               Model model) {
    model.addAttribute("activePage", "messages");
    messagingService.preparerModele(
            model, userDetails.getUtilisateur(), convId, "/responsable/messages", "Responsable");
    return "responsable/messages";
}

@PostMapping("/messages/nouveau")
public String nouvelleConversation(@AuthenticationPrincipal CustomUserDetails userDetails,
                                   @RequestParam Integer destinataireId,
                                   @RequestParam String message) {
    Utilisateur currentUser = userDetails.getUtilisateur();
    Utilisateur destinataire = utilisateurRepository.findById(destinataireId).orElse(null);
    if (destinataire == null || message == null || message.isBlank()) {
        return "redirect:/responsable/messages";
    }
    Conversation conversation = conversationRepository
            .findByParticipantIdOrderByDernierMessageDesc(currentUser.getId())
            .stream()
            .filter(existing -> existing.getParticipants().stream()
                    .anyMatch(participant -> participant.getId().equals(destinataireId)))
            .findFirst()
            .orElseGet(() -> {
                Conversation nouvelle = new Conversation();
                nouvelle.setSujet("Discussion avec " + destinataire.getPrenom() + " " + destinataire.getNom());
                nouvelle.setDateCreation(java.time.LocalDateTime.now());
                nouvelle.getParticipants().add(currentUser);
                nouvelle.getParticipants().add(destinataire);
                return nouvelle;
            });
    conversation.setDernierMessage(java.time.LocalDateTime.now());
    conversation = conversationRepository.save(conversation);
    enregistrerMessage(conversation, currentUser, message);
    return "redirect:/responsable/messages?convId=" + conversation.getId();
}

@PostMapping("/messages/envoyer")
public String envoyerMessage(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @RequestParam Integer convId,
                             @RequestParam String contenu) {
    Conversation conversation = conversationRepository.findById(convId)
            .filter(existing -> existing.getParticipants().stream()
                    .anyMatch(participant -> participant.getId()
                            .equals(userDetails.getUtilisateur().getId())))
            .orElse(null);
    if (conversation != null && contenu != null && !contenu.isBlank()) {
        conversation.setDernierMessage(java.time.LocalDateTime.now());
        conversationRepository.save(conversation);
        enregistrerMessage(conversation, userDetails.getUtilisateur(), contenu);
    }
    return "redirect:/responsable/messages?convId=" + convId;
}

private void enregistrerMessage(Conversation conversation,
                                Utilisateur expediteur,
                                String contenu) {
    Message message = new Message();
    message.setConversation(conversation);
    message.setExpediteur(expediteur);
    message.setContenu(contenu.trim());
    message.setDateEnvoi(java.time.LocalDateTime.now());
    message.setLu(false);
    messageRepository.save(message);
}

@GetMapping("/profil")
public String afficherProfil(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    Utilisateur responsable = userDetails.getUtilisateur();
    List<Stage> stages = stageRepository.findAll();
    ResponsablePreference preferences = responsablePreferenceRepository
            .findByUtilisateurId(responsable.getId())
            .orElseGet(() -> responsablePreferenceRepository.save(new ResponsablePreference(responsable)));
    List<ActivityLog> historiqueConnexions = activityLogRepository
            .findTop20ByUtilisateurNomOrderByDateActiviteDesc(responsable.getEmail())
            .stream()
            .filter(log -> "Connexion".equalsIgnoreCase(log.getAction()))
            .limit(10)
            .toList();
    model.addAttribute("activePage", "profil");
    model.addAttribute("responsable", responsable);
    model.addAttribute("preferences", preferences);
    model.addAttribute("historiqueConnexions", historiqueConnexions);
    model.addAttribute("sessionsActives", sessionsActives(responsable.getId()));
    model.addAttribute("stagiairesSuivis", stagiaireRepository.count());
    model.addAttribute("stagesGeres", stages.size());
    model.addAttribute("stagesClotures", stages.stream()
            .filter(stage -> stage.getStatut() == Stage.StatutStage.termine).count());
    long rapportsValides = documentRepository.findAll().stream()
            .filter(document -> "valide".equalsIgnoreCase(document.getStatut())
                    || "disponible".equalsIgnoreCase(document.getStatut())).count();
    long reunionsOrganisees = stages.stream()
            .flatMap(stage -> evenementPersonnelRepository.findByStageId(stage.getId()).stream())
            .filter(event -> "reunion".equals(event.getTypeCouleur())).count();
    int tauxSuivi = stages.isEmpty() ? 0 : (int) Math.round(stages.stream()
            .map(Stage::getStagiaire).filter(java.util.Objects::nonNull)
            .map(Stagiaire::getProgression).filter(java.util.Objects::nonNull)
            .mapToInt(Integer::intValue).average().orElse(0));
    model.addAttribute("rapportsValides", rapportsValides);
    model.addAttribute("reunionsOrganisees", reunionsOrganisees);
    model.addAttribute("tauxSuivi", tauxSuivi);
    model.addAttribute("activitesProfil", activityLogRepository
            .findTop20ByUtilisateurNomOrderByDateActiviteDesc(responsable.getEmail())
            .stream().limit(6).toList());
    model.addAttribute("anciennete", responsable.getDateCreation() == null ? 1 : Math.max(1,
            java.time.temporal.ChronoUnit.YEARS.between(
                    responsable.getDateCreation().toLocalDate(), LocalDate.now()) + 1));
    model.addAttribute("notificationsCount", notificationRepository
            .countByDestinataireTypeAndStatut("RESPONSABLE", "non_lue"));
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
    activityLogService.log("Profil modifie",
            "Mise a jour des informations personnelles du compte responsable.",
            responsable.getEmail());
    redirectAttributes.addFlashAttribute("succes", "Votre profil a été mis à jour.");
    return "redirect:/responsable/profil";
}

@GetMapping("/profil/mot-de-passe")
public String afficherMotDePasseResponsable(@AuthenticationPrincipal CustomUserDetails userDetails,
                                            Model model) {
    model.addAttribute("activePage", "profil");
    model.addAttribute("responsable", userDetails.getUtilisateur());
    return "responsable/mot-de-passe";
}

@PostMapping("/profil/mot-de-passe")
public String changerMotDePasseResponsable(@AuthenticationPrincipal CustomUserDetails userDetails,
                                           @RequestParam String ancienMotDePasse,
                                           @RequestParam String nouveauMotDePasse,
                                           @RequestParam String confirmation,
                                           Model model,
                                           RedirectAttributes redirectAttributes) {
    Utilisateur responsable = utilisateurRepository.findById(userDetails.getUtilisateur().getId())
            .orElseThrow();
    if (!passwordEncoder.matches(ancienMotDePasse, responsable.getMotDePasse())) {
        model.addAttribute("activePage", "profil");
        model.addAttribute("responsable", responsable);
        model.addAttribute("erreur", "L'ancien mot de passe est incorrect.");
        return "responsable/mot-de-passe";
    }
    if (!nouveauMotDePasse.equals(confirmation)) {
        model.addAttribute("activePage", "profil");
        model.addAttribute("responsable", responsable);
        model.addAttribute("erreur", "Les nouveaux mots de passe ne correspondent pas.");
        return "responsable/mot-de-passe";
    }
    if (nouveauMotDePasse.length() < 8) {
        model.addAttribute("activePage", "profil");
        model.addAttribute("responsable", responsable);
        model.addAttribute("erreur", "Le nouveau mot de passe doit contenir au moins 8 caracteres.");
        return "responsable/mot-de-passe";
    }
    responsable.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
    utilisateurRepository.save(responsable);
    activityLogService.log("Mot de passe modifie",
            "Le mot de passe du compte responsable a ete change.",
            responsable.getEmail());
    redirectAttributes.addFlashAttribute("succes", "Votre mot de passe a ete modifie.");
    return "redirect:/responsable/profil";
}

@PostMapping("/profil/preferences")
public String enregistrerPreferences(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @RequestParam(defaultValue = "false") boolean notificationsEmail,
                                     @RequestParam(defaultValue = "false") boolean notificationsSysteme,
                                     @RequestParam(defaultValue = "false") boolean modeSombre,
                                     @RequestParam(defaultValue = "false") boolean alertesSoutenance,
                                     @RequestParam(defaultValue = "false") boolean alertesRapport,
                                     RedirectAttributes redirectAttributes) {
    Utilisateur responsable = userDetails.getUtilisateur();
    ResponsablePreference preferences = responsablePreferenceRepository
            .findByUtilisateurId(responsable.getId())
            .orElseGet(() -> new ResponsablePreference(responsable));
    preferences.setNotificationsEmail(notificationsEmail);
    preferences.setNotificationsSysteme(notificationsSysteme);
    preferences.setModeSombre(modeSombre);
    preferences.setAlertesSoutenance(alertesSoutenance);
    preferences.setAlertesRapport(alertesRapport);
    responsablePreferenceRepository.save(preferences);
    activityLogService.log("Preferences modifiees",
            "Mise a jour des notifications, alertes et du theme.",
            responsable.getEmail());
    redirectAttributes.addFlashAttribute("succes", "Vos preferences ont ete enregistrees.");
    return "redirect:/responsable/profil";
}

@PostMapping("/profil/sessions/deconnecter")
public String deconnecterAutresSessions(@AuthenticationPrincipal CustomUserDetails userDetails,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
    int sessionsFermees = 0;
    for (Object principal : sessionRegistry.getAllPrincipals()) {
        if (!(principal instanceof CustomUserDetails details)
                || !details.getUtilisateur().getId().equals(userDetails.getUtilisateur().getId())) {
            continue;
        }
        for (SessionInformation information : sessionRegistry.getAllSessions(principal, false)) {
            if (!information.getSessionId().equals(session.getId())) {
                information.expireNow();
                sessionsFermees++;
            }
        }
    }
    activityLogService.log("Sessions deconnectees",
            sessionsFermees + " autre(s) session(s) ont ete fermees.",
            userDetails.getUtilisateur().getEmail());
    redirectAttributes.addFlashAttribute("succes",
            sessionsFermees == 0
                    ? "Aucune autre session active n'a ete trouvee."
                    : sessionsFermees + " autre(s) session(s) ont ete deconnectees.");
    return "redirect:/responsable/profil";
}

private long sessionsActives(Integer utilisateurId) {
    return sessionRegistry.getAllPrincipals().stream()
            .filter(CustomUserDetails.class::isInstance)
            .map(CustomUserDetails.class::cast)
            .filter(details -> details.getUtilisateur().getId().equals(utilisateurId))
            .flatMap(details -> sessionRegistry.getAllSessions(details, false).stream())
            .filter(information -> !information.isExpired())
            .count();
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

private String prochainMatriculeStagiaire() {
    long sequence = stagiaireRepository.count() + 1;
    String matricule;
    do {
        matricule = "STG-" + LocalDate.now().getYear() + "-" + String.format("%03d", sequence++);
    } while (stagiaireRepository.findByMatricule(matricule).isPresent());
    return matricule;
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
