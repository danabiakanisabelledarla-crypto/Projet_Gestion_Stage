package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.entities.Utilisateur;
import com.gestionstages.gestion_stages.repositories.UtilisateurRepository;
import com.gestionstages.gestion_stages.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profil")
public class ProfilController {

    private static final java.util.regex.Pattern FORMAT_EMAIL =
            java.util.regex.Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

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
                              @RequestParam String email,
                              @RequestParam(required = false) String telephone,
                              @RequestParam(required = false) String adresse,
                              HttpServletRequest request,
                              HttpServletResponse response,
                              Model model) {
    Utilisateur u = utilisateurRepository.findById(userDetails.getUtilisateur().getId())
            .orElseThrow();
    String nouvelEmail = email == null ? "" : email.trim();
    model.addAttribute("utilisateur", u);

    if (!FORMAT_EMAIL.matcher(nouvelEmail).matches()) {
        model.addAttribute("erreur", "Adresse email invalide.");
        return "profil/index";
    }
    boolean emailModifie = !nouvelEmail.equalsIgnoreCase(u.getEmail());
    if (emailModifie && utilisateurRepository.existsByEmail(nouvelEmail)) {
        model.addAttribute("erreur", "Cette adresse email est déjà utilisée par un autre compte.");
        return "profil/index";
    }

    u.setPrenom(prenom);
    u.setNom(nom);
    u.setEmail(nouvelEmail);
    u.setTelephone(telephone);
    u.setAdresse(adresse);
    utilisateurRepository.save(u);
    if (emailModifie) {
        rafraichirSession(u, request, response);
    }
    model.addAttribute("succes", "Profil mis à jour avec succès.");
    return "profil/index";
}

    private void rafraichirSession(Utilisateur utilisateur,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        CustomUserDetails principal = new CustomUserDetails(utilisateur);
        SecurityContext contexte = SecurityContextHolder.createEmptyContext();
        contexte.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, principal.getPassword(), principal.getAuthorities()));
        SecurityContextHolder.setContext(contexte);
        securityContextRepository.saveContext(contexte, request, response);
    }

    @GetMapping("/mot-de-passe")
    public String afficherMotDePasse(@AuthenticationPrincipal CustomUserDetails userDetails,
                                      Model model) {
        model.addAttribute("utilisateur", userDetails.getUtilisateur());
        model.addAttribute("retourProfil", urlProfil(userDetails.getUtilisateur()));
        return "profil/mot-de-passe";
    }

    @PostMapping("/mot-de-passe")
    public String changerMotDePasse(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @RequestParam String ancienMotDePasse,
                                     @RequestParam String nouveauMotDePasse,
                                     @RequestParam String confirmation,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        Utilisateur u = utilisateurRepository.findById(userDetails.getUtilisateur().getId())
                .orElseThrow();
        model.addAttribute("utilisateur", u);
        model.addAttribute("retourProfil", urlProfil(u));

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
        redirectAttributes.addFlashAttribute("succes", "Mot de passe modifié avec succès.");
        return "redirect:" + urlProfil(u);
    }

    private String urlProfil(Utilisateur utilisateur) {
        if (utilisateur.getRole() == null || utilisateur.getRole().getLibelle() == null) {
            return "/profil";
        }
        return switch (utilisateur.getRole().getLibelle()) {
            case "RESPONSABLE_STAGE" -> "/responsable/profil";
            case "ENCADREUR" -> "/encadreur/profil";
            case "STAGIAIRE" -> "/stagiaire/profil";
            default -> "/profil";
        };
    }
}
