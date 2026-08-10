package com.gestionstages.gestion_stages.security;

import com.gestionstages.gestion_stages.entities.Utilisateur;
import com.gestionstages.gestion_stages.repositories.UtilisateurRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TwoFactorInterceptor implements HandlerInterceptor {

    private final UtilisateurRepository utilisateurRepository;

    public TwoFactorInterceptor(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails details)) {
            return true;
        }
        Utilisateur utilisateur = utilisateurRepository
                .findById(details.getUtilisateur().getId())
                .orElse(details.getUtilisateur());
        boolean verificationRequise = utilisateur.isTwoFactorRequired()
                || utilisateur.isTwoFactorEnabled();
        boolean verifie = Boolean.TRUE.equals(
                request.getSession().getAttribute("twoFactorVerified"));
        if (verificationRequise && !verifie) {
            response.sendRedirect(utilisateur.isTwoFactorEnabled()
                    ? "/2fa/verification" : "/2fa/configuration");
            return false;
        }
        return true;
    }
}
