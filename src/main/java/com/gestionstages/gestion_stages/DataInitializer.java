package com.gestionstages.gestion_stages;

import com.gestionstages.gestion_stages.entities.Role;
import com.gestionstages.gestion_stages.entities.Utilisateur;
import com.gestionstages.gestion_stages.repositories.RoleRepository;
import com.gestionstages.gestion_stages.repositories.UtilisateurRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                            UtilisateurRepository utilisateurRepository,
                            PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        // Creer les 4 roles
        creerRoleSiAbsent("ADMINISTRATEUR", "Administrateur de la plateforme");
        creerRoleSiAbsent("RESPONSABLE_STAGE", "Responsable des stages");
        creerRoleSiAbsent("ENCADREUR", "Encadreur de stagiaires");
        creerRoleSiAbsent("STAGIAIRE", "Stagiaire");

        // Admin
        creerUtilisateurSiAbsent("ADMINISTRATEUR", "Admin", "Test",
                "admin@gestion-stages.com", "admin1234");

        // Responsable
        creerUtilisateurSiAbsent("RESPONSABLE_STAGE", "Responsable", "Test",
                "responsable@gestion-stages.com", "resp1234");

        // Encadreur
        creerUtilisateurSiAbsent("ENCADREUR", "Nikoa", "M",
                "encadreur@gestion-stages.com", "enc1234");

        // Stagiaire
        creerUtilisateurSiAbsent("STAGIAIRE", "Mebale", "Darla",
                "stagiaire@gestion-stages.com", "stag1234");
    }

    private void creerRoleSiAbsent(String libelle, String description) {
        if (roleRepository.findByLibelle(libelle).isEmpty()) {
            roleRepository.save(new Role(libelle, description));
        }
    }

    private void creerUtilisateurSiAbsent(String roleLibelle, String nom, String prenom,
                                            String email, String motDePasse) {
        if (!utilisateurRepository.existsByEmail(email)) {
            Role role = roleRepository.findByLibelle(roleLibelle).orElseThrow();
            Utilisateur u = new Utilisateur(role, nom, prenom, email,
                    passwordEncoder.encode(motDePasse));
            utilisateurRepository.save(u);
            System.out.println(">>> Utilisateur cree : " + email + " / " + motDePasse);
        }
    }
}