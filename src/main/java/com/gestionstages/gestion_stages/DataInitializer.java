package com.gestionstages.gestion_stages;

import com.gestionstages.gestion_stages.entities.*;
import com.gestionstages.gestion_stages.repositories.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EncadreurRepository encadreurRepository;
    private final ServiceEntrepriseRepository serviceRepository;
    private final ProjetRepository projetRepository;
    private final CritereEvaluationRepository critereRepository;
    private final DemandeStageRepository demandeStageRepository;
    private final StagiaireRepository stagiaireRepository;
    private final StageRepository stageRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                           UtilisateurRepository utilisateurRepository,
                           EncadreurRepository encadreurRepository,
                           ServiceEntrepriseRepository serviceRepository,
                           ProjetRepository projetRepository,
                           CritereEvaluationRepository critereRepository,
                           DemandeStageRepository demandeStageRepository,
                           StagiaireRepository stagiaireRepository,
                           StageRepository stageRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.encadreurRepository = encadreurRepository;
        this.serviceRepository = serviceRepository;
        this.projetRepository = projetRepository;
        this.critereRepository = critereRepository;
        this.demandeStageRepository = demandeStageRepository;
        this.stagiaireRepository = stagiaireRepository;
        this.stageRepository = stageRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        creerRoleSiAbsent("ADMINISTRATEUR", "Administrateur de la plateforme");
        creerRoleSiAbsent("RESPONSABLE_STAGE", "Responsable des stages");
        creerRoleSiAbsent("ENCADREUR", "Encadreur de stagiaires");
        creerRoleSiAbsent("STAGIAIRE", "Stagiaire");

        creerUtilisateurSiAbsent("ADMINISTRATEUR", "Admin", "Test",
                "admin@gestion-stages.com", "admin1234");
        creerUtilisateurSiAbsent("RESPONSABLE_STAGE", "Responsable", "Test",
                "responsable@gestion-stages.com", "resp1234");

        Utilisateur utilisateurEncadreur = creerUtilisateurSiAbsent(
                "ENCADREUR", "Nikoa", "M",
                "encadreur@gestion-stages.com", "enc1234");

        Utilisateur utilisateurStagiaire = creerUtilisateurSiAbsent(
                "STAGIAIRE", "Mebale", "Darla",
                "stagiaire@gestion-stages.com", "stag1234");

        if (utilisateurEncadreur != null &&
                encadreurRepository.findByUtilisateurId(utilisateurEncadreur.getId()).isEmpty()) {
            Encadreur encadreur = new Encadreur(utilisateurEncadreur,
                    "Developpement Web", "Ingenieur Senior");
            encadreurRepository.save(encadreur);
            System.out.println(">>> Fiche encadreur creee pour : "
                    + utilisateurEncadreur.getEmail());
        }

        creerServiceSiAbsent("Developpement logiciel",
                "Service en charge du developpement des applications");
        creerServiceSiAbsent("Infrastructure",
                "Service en charge des serveurs et reseaux");
        creerServiceSiAbsent("Data et Intelligence Artificielle",
                "Service en charge des donnees et modeles IA");

        creerProjetSiAbsent("Plateforme de gestion des stages",
                "Developpement d'une plateforme web de suivi des stages",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        creerProjetSiAbsent("Application mobile RH",
                "Developpement d'une app mobile pour la gestion RH",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 9, 30));

        creerCritereSiAbsent("Technique", "Maitrise des outils et technologies");
        creerCritereSiAbsent("Autonomie", "Capacite a travailler de facon independante");
        creerCritereSiAbsent("Communication", "Qualite de la communication orale et ecrite");
        creerCritereSiAbsent("Ponctualite", "Respect des horaires et des delais");
        creerCritereSiAbsent("Initiative", "Prise d initiative et propositions");

        creerStageDemo(utilisateurStagiaire, utilisateurEncadreur);
    }

    private void creerStageDemo(Utilisateur utilisateurStagiaire, Utilisateur utilisateurEncadreur) {
        if (utilisateurStagiaire == null || utilisateurEncadreur == null) return;

        try {
            var stagiaireExistant = stagiaireRepository.findByUtilisateurId(utilisateurStagiaire.getId());
            if (stagiaireExistant.isPresent()) {
                if (stageRepository.findByStagiaireId(stagiaireExistant.get().getId()).isEmpty()) {
                    creerStagePourStagiaire(stagiaireExistant.get(), utilisateurEncadreur);
                }
                return;
            }

            String matricule = "STG-DEMO-" + utilisateurStagiaire.getId();
            if (stagiaireRepository.findByMatricule(matricule).isPresent()) return;

            DemandeStage demande = new DemandeStage("Mebale", "Darla",
                    "Universite de Yaounde I", "Informatique", "Licence 3", "3 mois");
            demande.setStatut(DemandeStage.StatutDemande.acceptee);
            demande.setCommentaire("Email candidat : stagiaire@gestion-stages.com");
            demandeStageRepository.save(demande);

            Stagiaire stagiaire = new Stagiaire(utilisateurStagiaire, demande,
                    matricule, LocalDate.now().minusMonths(1));
            stagiaireRepository.save(stagiaire);
            creerStagePourStagiaire(stagiaire, utilisateurEncadreur);
        } catch (Exception e) {
            System.err.println(">>> Impossible de creer le stage demo : " + e.getMessage());
        }
    }

    private void creerStagePourStagiaire(Stagiaire stagiaire, Utilisateur utilisateurEncadreur) {
        var encadreurOpt = encadreurRepository.findByUtilisateurId(utilisateurEncadreur.getId());
        var serviceOpt = serviceRepository.findAll().stream().findFirst();
        var projetOpt = projetRepository.findAll().stream().findFirst();

        if (encadreurOpt.isEmpty() || serviceOpt.isEmpty()) return;

        String numeroStage = "STG-NUM-DEMO-" + stagiaire.getId();
        Stage stage = new Stage(stagiaire, encadreurOpt.get(), serviceOpt.get(),
                numeroStage,
                LocalDate.now().minusMonths(1),
                LocalDate.now().plusMonths(2),
                "90 jours");
        projetOpt.ifPresent(stage::setProjet);
        stageRepository.save(stage);
        System.out.println(">>> Stage demo cree pour : stagiaire@gestion-stages.com");
    }

    private void creerRoleSiAbsent(String libelle, String description) {
        if (roleRepository.findByLibelle(libelle).isEmpty()) {
            roleRepository.save(new Role(libelle, description));
        }
    }

    private Utilisateur creerUtilisateurSiAbsent(String roleLibelle, String nom,
                                                   String prenom, String email,
                                                   String motDePasse) {
        if (!utilisateurRepository.existsByEmail(email)) {
            Role role = roleRepository.findByLibelle(roleLibelle).orElseThrow();
            Utilisateur u = new Utilisateur(role, nom, prenom, email,
                    passwordEncoder.encode(motDePasse));
            utilisateurRepository.save(u);
            System.out.println(">>> Utilisateur cree : " + email + " / " + motDePasse);
            return u;
        }
        return utilisateurRepository.findByEmail(email).orElse(null);
    }

    private void creerServiceSiAbsent(String nom, String description) {
        if (serviceRepository.findAll().stream()
                .noneMatch(s -> s.getNom().equals(nom))) {
            serviceRepository.save(new ServiceEntreprise(nom, description));
            System.out.println(">>> Service cree : " + nom);
        }
    }

    private void creerProjetSiAbsent(String titre, String description,
                                      LocalDate debut, LocalDate fin) {
        if (projetRepository.findAll().stream()
                .noneMatch(p -> p.getTitre().equals(titre))) {
            projetRepository.save(new Projet(titre, description, debut, fin));
            System.out.println(">>> Projet cree : " + titre);
        }
    }

    private void creerCritereSiAbsent(String libelle, String description) {
        if (critereRepository.findAll().stream()
                .noneMatch(c -> c.getLibelle().equals(libelle))) {
            critereRepository.save(new CritereEvaluation(libelle, description));
            System.out.println(">>> Critere cree : " + libelle);
        }
    }
}
