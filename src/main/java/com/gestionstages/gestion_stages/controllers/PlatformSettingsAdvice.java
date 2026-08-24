package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.services.ApplicationSettingService;
import com.gestionstages.gestion_stages.repositories.AdminPreferenceRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;

/**
 * Expose les paramètres globaux aux vues afin que les changements effectués
 * par l'administrateur soient disponibles partout après sauvegarde.
 */
@ControllerAdvice
public class PlatformSettingsAdvice {

    private final ApplicationSettingService applicationSettingService;
    private final AdminPreferenceRepository adminPreferenceRepository;

    public PlatformSettingsAdvice(ApplicationSettingService applicationSettingService,
                                  AdminPreferenceRepository adminPreferenceRepository) {
        this.applicationSettingService = applicationSettingService;
        this.adminPreferenceRepository = adminPreferenceRepository;
    }

    @ModelAttribute("platformSettings")
    public Map<String, String> platformSettings() {
        return applicationSettingService.getAll();
    }

    @ModelAttribute("adminDarkMode")
    public boolean adminDarkMode() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal()
                instanceof com.gestionstages.gestion_stages.security.CustomUserDetails details)) {
            return false;
        }
        return adminPreferenceRepository.findByUtilisateurId(details.getUtilisateur().getId())
                .map(preferences -> preferences.isModeSombre())
                .orElse(false);
    }
}
