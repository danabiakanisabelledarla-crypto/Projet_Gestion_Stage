package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String afficherLogin() {
        return "auth/login";
    }

    @GetMapping("/redirection")
    public String redirigerSelonRole(@AuthenticationPrincipal CustomUserDetails userDetails) {
        String role = userDetails.getUtilisateur().getRole().getLibelle();

        switch (role) {
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
}