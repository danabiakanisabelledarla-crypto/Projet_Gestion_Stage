package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.AdminPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminPreferenceRepository extends JpaRepository<AdminPreference, Integer> {
    Optional<AdminPreference> findByUtilisateurId(Integer utilisateurId);
}
