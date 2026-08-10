package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.EmailService;
import com.gestionstages.gestion_stages.entities.PasswordResetToken;
import com.gestionstages.gestion_stages.entities.Utilisateur;
import com.gestionstages.gestion_stages.repositories.PasswordResetTokenRepository;
import com.gestionstages.gestion_stages.repositories.UtilisateurRepository;
import com.gestionstages.gestion_stages.security.CustomUserDetails;
import com.gestionstages.gestion_stages.services.ActivityLogService;
import com.gestionstages.gestion_stages.services.TotpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
    private final ActivityLogService activityLogService;
    private final TotpService totpService;

    public AuthController(UtilisateurRepository utilisateurRepository,
                          PasswordResetTokenRepository resetTokenRepository,
                          PasswordEncoder passwordEncoder,
                          EmailService emailService,
                          ActivityLogService activityLogService,
                          TotpService totpService) {
        this.utilisateurRepository = utilisateurRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.activityLogService = activityLogService;
        this.totpService = totpService;
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
    public String redirigerSelonRole(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     HttpServletRequest request) {
        Utilisateur utilisateurCourant = utilisateurRepository
                .findById(userDetails.getUtilisateur().getId())
                .orElse(userDetails.getUtilisateur());
        if ((utilisateurCourant.isTwoFactorRequired() || utilisateurCourant.isTwoFactorEnabled())
                && !Boolean.TRUE.equals(request.getSession().getAttribute("twoFactorVerified"))) {
            return utilisateurCourant.isTwoFactorEnabled()
                    ? "redirect:/2fa/verification"
                    : "redirect:/2fa/configuration";
        }
        if (request.getSession().getAttribute("connexionJournalisee") == null) {
            utilisateurCourant.setDerniereConnexion(java.time.LocalDateTime.now());
            utilisateurRepository.save(utilisateurCourant);
            String userAgent = request.getHeader("User-Agent");
            String appareil = userAgent == null || userAgent.isBlank()
                    ? "Navigateur non identifie"
                    : userAgent.substring(0, Math.min(userAgent.length(), 180));
            activityLogService.log("Connexion",
                    "Adresse IP: " + request.getRemoteAddr() + " | Appareil: " + appareil,
                    utilisateurCourant.getEmail());
            request.getSession().setAttribute("connexionJournalisee", Boolean.TRUE);
        }
        String libelleRole = utilisateurCourant.getRole().getLibelle();
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

    @GetMapping("/2fa/configuration")
    public String afficherConfigurationDeuxFacteurs(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        Utilisateur utilisateur = utilisateurRepository
                .findById(userDetails.getUtilisateur().getId()).orElseThrow();
        if (utilisateur.isTwoFactorEnabled()) {
            return "redirect:/2fa/verification";
        }
        if (utilisateur.getTwoFactorSecret() == null
                || utilisateur.getTwoFactorSecret().isBlank()) {
            utilisateur.setTwoFactorSecret(totpService.generateSecret());
            utilisateurRepository.save(utilisateur);
        }
        model.addAttribute("secret", utilisateur.getTwoFactorSecret());
        model.addAttribute("provisioningUri", totpService.provisioningUri(
                utilisateur.getTwoFactorSecret(), utilisateur.getEmail(), "Gestion des Stages"));
        model.addAttribute("email", utilisateur.getEmail());
        return "auth/configuration-2fa";
    }

    @PostMapping("/2fa/configuration")
    public String confirmerConfigurationDeuxFacteurs(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String code,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Utilisateur utilisateur = utilisateurRepository
                .findById(userDetails.getUtilisateur().getId()).orElseThrow();
        if (!totpService.verify(utilisateur.getTwoFactorSecret(), code.trim())) {
            redirectAttributes.addFlashAttribute("erreur",
                    "Le code est invalide ou expire. Verifiez l'heure de votre telephone.");
            return "redirect:/2fa/configuration";
        }
        utilisateur.setTwoFactorEnabled(true);
        utilisateur.setTwoFactorRequired(true);
        utilisateurRepository.save(utilisateur);
        session.setAttribute("twoFactorVerified", Boolean.TRUE);
        activityLogService.log("2FA activee", utilisateur.getEmail(), utilisateur.getEmail());
        return "redirect:/redirection";
    }

    @GetMapping("/2fa/verification")
    public String afficherVerificationDeuxFacteurs(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Utilisateur utilisateur = utilisateurRepository
                .findById(userDetails.getUtilisateur().getId()).orElseThrow();
        if (!utilisateur.isTwoFactorEnabled()) {
            return "redirect:/2fa/configuration";
        }
        return "auth/verification-2fa";
    }

    @PostMapping("/2fa/verification")
    public String verifierDeuxFacteurs(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String code,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Utilisateur utilisateur = utilisateurRepository
                .findById(userDetails.getUtilisateur().getId()).orElseThrow();
        if (!totpService.verify(utilisateur.getTwoFactorSecret(), code.trim())) {
            redirectAttributes.addFlashAttribute("erreur",
                    "Code de verification invalide ou expire.");
            return "redirect:/2fa/verification";
        }
        session.setAttribute("twoFactorVerified", Boolean.TRUE);
        activityLogService.log("Verification 2FA reussie",
                utilisateur.getEmail(), utilisateur.getEmail());
        return "redirect:/redirection";
    }
    @GetMapping("/")
    public String afficherAccueil() {
    return "accueil";
}
}
