package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.ResponsablePreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResponsablePreferenceRepository extends JpaRepository<ResponsablePreference, Integer> {
    Optional<ResponsablePreference> findByUtilisateurId(Integer utilisateurId);
}
