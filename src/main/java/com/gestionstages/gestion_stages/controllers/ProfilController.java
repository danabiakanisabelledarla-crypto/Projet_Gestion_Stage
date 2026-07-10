package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.entities.Utilisateur;
import com.gestionstages.gestion_stages.repositories.UtilisateurRepository;
import com.gestionstages.gestion_stages.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/profil")
public class ProfilController {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfilController(UtilisateurRepository utilisateurRepository,
                            PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String afficherProfil(@AuthenticationPrincipal CustomUserDetails userDetails,
                                  Model model) {
        model.addAttribute("utilisateur", userDetails.getUtilisateur());
        return "profil/index";
    }

    @PostMapping("/modifier")
    public String modifierProfil(@AuthenticationPrincipal CustomUserDetails userDetails,
                                  @RequestParam String prenom,
                                  @RequestParam String nom,
                                  @RequestParam(required = false) String telephone,
                                  Model model) {
        Utilisateur u = utilisateurRepository.findById(userDetails.getUtilisateur().getId())
                .orElseThrow();
        u.setPrenom(prenom);
        u.setNom(nom);
        u.setTelephone(telephone);
        utilisateurRepository.save(u);
        model.addAttribute("utilisateur", u);
        model.addAttribute("succes", "Profil mis à jour avec succès.");
        return "profil/index";
    }

    @GetMapping("/mot-de-passe")
    public String afficherMotDePasse(@AuthenticationPrincipal CustomUserDetails userDetails,
                                      Model model) {
        model.addAttribute("utilisateur", userDetails.getUtilisateur());
        return "profil/mot-de-passe";
    }

    @PostMapping("/mot-de-passe")
    public String changerMotDePasse(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @RequestParam String ancienMotDePasse,
                                     @RequestParam String nouveauMotDePasse,
                                     @RequestParam String confirmation,
                                     Model model) {
        Utilisateur u = utilisateurRepository.findById(userDetails.getUtilisateur().getId())
                .orElseThrow();
        model.addAttribute("utilisateur", u);

        if (!passwordEncoder.matches(ancienMotDePasse, u.getMotDePasse())) {
            model.addAttribute("erreur", "L'ancien mot de passe est incorrect.");
            return "profil/mot-de-passe";
        }
        if (!nouveauMotDePasse.equals(confirmation)) {
            model.addAttribute("erreur", "Les mots de passe ne correspondent pas.");
            return "profil/mot-de-passe";
        }
        if (nouveauMotDePasse.length() < 6) {
            model.addAttribute("erreur", "Le mot de passe doit contenir au moins 6 caractères.");
            return "profil/mot-de-passe";
        }

        u.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        utilisateurRepository.save(u);
        model.addAttribute("succes", "Mot de passe modifié avec succès.");
        return "profil/mot-de-passe";
    }
}
