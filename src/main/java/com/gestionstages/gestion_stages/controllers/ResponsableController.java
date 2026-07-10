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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
    private final PasswordEncoder passwordEncoder;

    private final ArchiveRepository archiveRepository;
    private final DocumentRepository documentRepository;
    private final EvaluationRepository evaluationRepository;
    
    private final EmailService emailService;

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
                              EmailService emailService,
                              JavaMailSender mailSender) {
    this.demandeStageRepository = demandeStageRepository;
    this.stagiaireRepository = stagiaireRepository;
    this.stageRepository = stageRepository;
    this.utilisateurRepository = utilisateurRepository;
    this.roleRepository = roleRepository;
    this.encadreurRepository = encadreurRepository;
    this.serviceRepository = serviceRepository;
    this.projetRepository = projetRepository;
    this.passwordEncoder = passwordEncoder;
    this.archiveRepository = archiveRepository;
    this.documentRepository = documentRepository;
    this.evaluationRepository = evaluationRepository;
    this.mailSender = mailSender;
    this.emailService = emailService;
}

    @GetMapping("/dashboard")
    public String afficherDashboard(Model model) {
        List<DemandeStage> demandes = demandeStageRepository.findAll();

        long demandesEnAttente = demandes.stream()
                .filter(d -> d.getStatut() == DemandeStage.StatutDemande.en_attente)
                .count();
        long acceptees = demandes.stream()
                .filter(d -> d.getStatut() == DemandeStage.StatutDemande.acceptee)
                .count();
        long refusees = demandes.stream()
                .filter(d -> d.getStatut() == DemandeStage.StatutDemande.refusee)
                .count();
        long totalDemandes = demandes.size();

        List<DemandeStage> demandesRecents = demandes.stream()
                .sorted(Comparator.comparing(DemandeStage::getDateDemande, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(3)
                .toList();

        long totalStagiaires = stagiaireRepository.count();
        long totalStages = stageRepository.count();

        model.addAttribute("demandesEnAttente", demandesEnAttente);
        model.addAttribute("totalDemandes", totalDemandes);
        model.addAttribute("acceptees", acceptees);
        model.addAttribute("refusees", refusees);
        model.addAttribute("demandesRecents", demandesRecents);
        model.addAttribute("totalStagiaires", totalStagiaires);
        model.addAttribute("totalStages", totalStages);

        return "responsable/dashboard";
    }

    @GetMapping("/demandes")
    public String afficherDemandes(Model model) {
        List<DemandeStage> demandes = demandeStageRepository.findAll();
        long enAttente = demandes.stream()
                .filter(d -> d.getStatut() == DemandeStage.StatutDemande.en_attente)
                .count();
        long acceptees = demandes.stream()
                .filter(d -> d.getStatut() == DemandeStage.StatutDemande.acceptee)
                .count();
        long refusees = demandes.stream()
                .filter(d -> d.getStatut() == DemandeStage.StatutDemande.refusee)
                .count();

        model.addAttribute("demandes", demandes);
        model.addAttribute("totalDemandes", demandes.size());
        model.addAttribute("enAttente", enAttente);
        model.addAttribute("acceptees", acceptees);
        model.addAttribute("refusees", refusees);
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

    @GetMapping("/admissions")
    public String afficherAdmissions(Model model) {
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
    List<Stage> stagesEnCours = stageRepository
            .findByStatut(Stage.StatutStage.en_cours);
    model.addAttribute("stagesEnCours", stagesEnCours);
    if (succes != null) model.addAttribute("succes", succes);
    return "responsable/cloture";
}

@GetMapping("/cloture/detail/{id}")
public String afficherDetailCloture(@PathVariable Integer id, Model model) {
    Optional<Stage> stageOpt = stageRepository.findById(id);
    if (stageOpt.isEmpty()) return "redirect:/responsable/cloture";

    Stage stage = stageOpt.get();
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

@GetMapping("/stagiaires")
public String afficherStagiairesResp(Model model) {
    model.addAttribute("activePage", "stagiaires");
    model.addAttribute("stagiaires", stagiaireRepository.findAll());
    model.addAttribute("stages", stageRepository.findAll());
    return "responsable/stagiaires";
}

@GetMapping("/dossiers")
public String afficherDossiers(Model model) {
    model.addAttribute("activePage", "dossiers");
    model.addAttribute("documents", documentRepository.findAll());
    return "responsable/dossiers";
}

@GetMapping("/planning")
public String afficherPlanning(Model model) {
    model.addAttribute("activePage", "planning");
    model.addAttribute("stages", stageRepository.findAll());
    return "responsable/planning";
}

@GetMapping("/suivi")
public String afficherSuivi(Model model) {
    model.addAttribute("activePage", "suivi");
    model.addAttribute("stages", stageRepository.findByStatut(Stage.StatutStage.en_cours));
    return "responsable/suivi";
}

@GetMapping("/notifications")
public String afficherNotificationsResp(Model model) {
    model.addAttribute("activePage", "notifications");
    return "responsable/notifications";
}

@GetMapping("/rapports")
public String afficherRapportsResp(Model model) {
    model.addAttribute("activePage", "rapports");
    model.addAttribute("totalStagiaires", stagiaireRepository.count());
    model.addAttribute("totalStages", stageRepository.count());
    return "responsable/rapports";
}

@GetMapping("/parametres")
public String afficherParametresResp(Model model) {
    model.addAttribute("activePage", "parametres");
    return "responsable/parametres";
}
}