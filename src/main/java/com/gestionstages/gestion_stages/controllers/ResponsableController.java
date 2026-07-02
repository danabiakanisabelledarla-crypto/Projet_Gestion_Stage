package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.entities.*;
import com.gestionstages.gestion_stages.repositories.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/responsable")
public class ResponsableController {

    private final DemandeStageRepository demandeStageRepository;
    private final StagiaireRepository stagiaireRepository;
    private final StageRepository stageRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final EncadreurRepository encadreurRepository;
    private final ServiceEntrepriseRepository serviceRepository;
    private final ProjetRepository projetRepository;
    private final PasswordEncoder passwordEncoder;

    public ResponsableController(DemandeStageRepository demandeStageRepository,
                                  StagiaireRepository stagiaireRepository,
                                  StageRepository stageRepository,
                                  UtilisateurRepository utilisateurRepository,
                                  RoleRepository roleRepository,
                                  EncadreurRepository encadreurRepository,
                                  ServiceEntrepriseRepository serviceRepository,
                                  ProjetRepository projetRepository,
                                  PasswordEncoder passwordEncoder) {
        this.demandeStageRepository = demandeStageRepository;
        this.stagiaireRepository = stagiaireRepository;
        this.stageRepository = stageRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.roleRepository = roleRepository;
        this.encadreurRepository = encadreurRepository;
        this.serviceRepository = serviceRepository;
        this.projetRepository = projetRepository;
        this.passwordEncoder = passwordEncoder;
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
        model.addAttribute("demandes", demandeStageRepository.findAll());
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
            // 1. Creer le compte utilisateur du stagiaire
            Role roleStagiaire = roleRepository.findByLibelle("STAGIAIRE").orElseThrow();
            String emailStagiaire = demande.getPrenom().toLowerCase()
                    + "." + demande.getNom().toLowerCase()
                    + "@stagiaire.com";
            String motDePasse = "stag" + LocalDate.now().getYear();

            Utilisateur utilisateur = new Utilisateur(roleStagiaire,
                    demande.getNom(), demande.getPrenom(),
                    emailStagiaire, passwordEncoder.encode(motDePasse));
            utilisateurRepository.save(utilisateur);

            // 2. Generer le matricule
            long nombreStagiaires = stagiaireRepository.count() + 1;
            String matricule = "STG-" + LocalDate.now().getYear()
                    + "-" + String.format("%03d", nombreStagiaires);

            // 3. Creer la fiche stagiaire
            Stagiaire stagiaire = new Stagiaire(utilisateur, demande,
                    matricule, LocalDate.now());
            stagiaireRepository.save(stagiaire);

            // 4. Creer le stage avec affectation
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
}