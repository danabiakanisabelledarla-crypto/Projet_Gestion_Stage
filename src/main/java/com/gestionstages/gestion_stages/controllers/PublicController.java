package com.gestionstages.gestion_stages.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PublicController {

    @GetMapping("/a-propos")
    public String aPropos(Model model) {
        model.addAttribute("activePage", "a-propos");
        return "public/a-propos";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("activePage", "contact");
        return "public/contact";
    }

    @PostMapping("/contact")
    public String envoyerContact(@RequestParam String nom,
                                  @RequestParam String email,
                                  @RequestParam String sujet,
                                  @RequestParam String message,
                                  Model model) {
        model.addAttribute("activePage", "contact");
        model.addAttribute("succes", "Votre message a bien été envoyé. Nous vous répondrons sous 48 heures.");
        return "public/contact";
    }

    @GetMapping("/faq")
    public String faq(Model model) {
        model.addAttribute("activePage", "faq");
        return "public/faq";
    }

    @GetMapping("/mentions-legales")
    public String mentionsLegales(Model model) {
        model.addAttribute("activePage", "mentions-legales");
        return "public/mentions-legales";
    }

    @GetMapping("/confidentialite")
    public String confidentialite(Model model) {
        model.addAttribute("activePage", "confidentialite");
        return "public/confidentialite";
    }
}
