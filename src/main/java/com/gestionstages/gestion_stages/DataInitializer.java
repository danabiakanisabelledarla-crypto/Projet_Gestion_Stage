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
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                            UtilisateurRepository utilisateurRepository,
                            EncadreurRepository encadreurRepository,
                            ServiceEntrepriseRepository serviceRepository,
                            ProjetRepository projetRepository,
                            PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.encadreurRepository = encadreurRepository;
        this.serviceRepository = serviceRepository;
        this.projetRepository = projetRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        // 1. Roles
        creerRoleSiAbsent("ADMINISTRATEUR", "Administrateur de la plateforme");
        creerRoleSiAbsent("RESPONSABLE_STAGE", "Responsable des stages");
        creerRoleSiAbsent("ENCADREUR", "Encadreur de stagiaires");
        creerRoleSiAbsent("STAGIAIRE", "Stagiaire");

        // 2. Utilisateurs de test
        creerUtilisateurSiAbsent("ADMINISTRATEUR", "Admin", "Test",
                "admin@gestion-stages.com", "admin1234");
        creerUtilisateurSiAbsent("RESPONSABLE_STAGE", "Responsable", "Test",
                "responsable@gestion-stages.com", "resp1234");

        Utilisateur utilisateurEncadreur = creerUtilisateurSiAbsent(
                "ENCADREUR", "Nikoa", "M",
                "encadreur@gestion-stages.com", "enc1234");

        creerUtilisateurSiAbsent("STAGIAIRE", "Mebale", "Darla",
                "stagiaire@gestion-stages.com", "stag1234");

        // 3. Fiche encadreur
        if (utilisateurEncadreur != null &&
                encadreurRepository.findByUtilisateurId(utilisateurEncadreur.getId()).isEmpty()) {
            Encadreur encadreur = new Encadreur(utilisateurEncadreur,
                    "Developpement Web", "Ingenieur Senior");
            encadreurRepository.save(encadreur);
            System.out.println(">>> Fiche encadreur creee pour : "
                    + utilisateurEncadreur.getEmail());
        }

        // 4. Services
        creerServiceSiAbsent("Developpement logiciel",
                "Service en charge du developpement des applications");
        creerServiceSiAbsent("Infrastructure",
                "Service en charge des serveurs et reseaux");
        creerServiceSiAbsent("Data et Intelligence Artificielle",
                "Service en charge des donnees et modeles IA");

        // 5. Projets
        creerProjetSiAbsent("Plateforme de gestion des stages",
                "Developpement d'une plateforme web de suivi des stages",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        creerProjetSiAbsent("Application mobile RH",
                "Developpement d'une app mobile pour la gestion RH",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 9, 30));
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
}