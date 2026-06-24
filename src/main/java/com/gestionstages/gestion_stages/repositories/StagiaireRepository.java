package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.Stagiaire;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StagiaireRepository extends JpaRepository<Stagiaire, Integer> {

    Optional<Stagiaire> findByUtilisateurId(Integer utilisateurId);

    Optional<Stagiaire> findByMatricule(String matricule);
}