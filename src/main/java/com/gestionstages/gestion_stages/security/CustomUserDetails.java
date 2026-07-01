package com.gestionstages.gestion_stages.security;

import com.gestionstages.gestion_stages.entities.Utilisateur;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;


public class CustomUserDetails implements UserDetails{

    private final Utilisateur utilisateur;

    public CustomUserDetails(Utilisateur utilisateur){
        this.utilisateur = utilisateur;
    }
    public Utilisateur getUtilisateur(){
        return utilisateur;
    }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(){
        String roleName = "ROLE_" + utilisateur.getRole().getLibelle();
        return List.of(new SimpleGrantedAuthority(roleName));
    }

    @Override
    public String getPassword(){
        return utilisateur.getMotDePasse();
    }
    @Override
    public String getUsername(){
        return utilisateur.getEmail();
    }
    @Override
    public boolean isAccountNonExpired(){
        return true;
    }
    @Override
    public boolean isAccountNonLocked(){
        return true;
    }
    @Override
    public boolean isCredentialsNonExpired(){
        return true;
    }
    @Override
    public boolean isEnabled() {
        return utilisateur.getStatut() == Utilisateur.StatutUtilisateur.actif;
    }
    
}
