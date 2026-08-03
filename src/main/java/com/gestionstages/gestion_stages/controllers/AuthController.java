package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.EmailService;
import com.gestionstages.gestion_stages.entities.PasswordResetToken;
import com.gestionstages.gestion_stages.entities.Utilisateur;
import com.gestionstages.gestion_stages.repositories.PasswordResetTokenRepository;
import com.gestionstages.gestion_stages.repositories.UtilisateurRepository;
import com.gestionstages.gestion_stages.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Controller
public class AuthController {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AuthController(UtilisateurRepository utilisateurRepository,
                          PasswordResetTokenRepository resetTokenRepository,
                          PasswordEncoder passwordEncoder,
                          EmailService emailService) {
        this.utilisateurRepository = utilisateurRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @GetMapping("/login")
    public String afficherLogin() {
        return "auth/login";
    }

    @GetMapping("/mot-de-passe-oublie")
    public String afficherMotDePasseOublie() {
        return "auth/mot-de-passe-oublie";
    }

    @PostMapping("/mot-de-passe-oublie")
    public String demanderReinitialisation(@RequestParam String email,
                                           RedirectAttributes redirectAttributes) {
        Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findByEmail(email.trim().toLowerCase());
        utilisateurOpt.ifPresent(utilisateur -> {
            String token = UUID.randomUUID().toString();
            resetTokenRepository.save(new PasswordResetToken(
                    token, utilisateur, LocalDateTime.now().plusMinutes(30)));
            String lien = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/reinitialiser-mot-de-passe")
                    .queryParam("token", token)
                    .toUriString();
            emailService.envoyerLienReinitialisation(
                    utilisateur.getEmail(),
                    utilisateur.getPrenom() + " " + utilisateur.getNom(),
                    lien);
        });
        redirectAttributes.addFlashAttribute("message",
                "Si un compte correspond à cette adresse, un lien valable 30 minutes vient d'être envoyé.");
        return "redirect:/mot-de-passe-oublie";
    }

    @GetMapping("/reinitialiser-mot-de-passe")
    public String afficherReinitialisation(@RequestParam String token, Model model) {
        Optional<PasswordResetToken> resetOpt = resetTokenRepository.findByToken(token);
        if (resetOpt.isEmpty() || !resetOpt.get().estValide()) {
            model.addAttribute("erreur", "Ce lien est invalide, expiré ou a déjà été utilisé.");
            return "auth/reinitialiser-mot-de-passe";
        }
        model.addAttribute("token", token);
        return "auth/reinitialiser-mot-de-passe";
    }

    @PostMapping("/reinitialiser-mot-de-passe")
    public String reinitialiserMotDePasse(@RequestParam String token,
                                          @RequestParam String motDePasse,
                                          @RequestParam String confirmation,
                                          Model model,
                                          RedirectAttributes redirectAttributes) {
        Optional<PasswordResetToken> resetOpt = resetTokenRepository.findByToken(token);
        if (resetOpt.isEmpty() || !resetOpt.get().estValide()) {
            model.addAttribute("erreur", "Ce lien est invalide, expiré ou a déjà été utilisé.");
            return "auth/reinitialiser-mot-de-passe";
        }
        if (motDePasse.length() < 8 || !motDePasse.equals(confirmation)) {
            model.addAttribute("token", token);
            model.addAttribute("erreur", "Les mots de passe doivent être identiques et contenir au moins 8 caractères.");
            return "auth/reinitialiser-mot-de-passe";
        }
        PasswordResetToken reset = resetOpt.get();
        Utilisateur utilisateur = reset.getUtilisateur();
        utilisateur.setMotDePasse(passwordEncoder.encode(motDePasse));
        utilisateurRepository.save(utilisateur);
        reset.setUtilise(true);
        resetTokenRepository.save(reset);
        redirectAttributes.addFlashAttribute("motDePasseModifie",
                "Votre mot de passe a été réinitialisé. Vous pouvez maintenant vous connecter.");
        return "redirect:/login";
    }

    @GetMapping("/redirection")
    public String redirigerSelonRole(@AuthenticationPrincipal CustomUserDetails userDetails) {
        String libelleRole = userDetails.getUtilisateur().getRole().getLibelle();
        String espace = switch (libelleRole) {
            case "ADMINISTRATEUR", "RESPONSABLE_STAGE", "ENCADREUR", "STAGIAIRE" -> libelleRole;
            default -> userDetails.getUtilisateur().getRole().getEspaceEffectif();
        };

        switch (espace) {
            case "ADMINISTRATEUR":
                return "redirect:/admin/dashboard";
            case "RESPONSABLE_STAGE":
                return "redirect:/responsable/dashboard";
            case "ENCADREUR":
                return "redirect:/encadreur/dashboard";
            case "STAGIAIRE":
                return "redirect:/stagiaire/dashboard";
            default:
                return "redirect:/login?error=true";
        }
    }
    @GetMapping("/")
    public String afficherAccueil() {
    return "accueil";
}
}
