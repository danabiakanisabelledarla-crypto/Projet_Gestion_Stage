package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.Encadreur;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EncadreurRepository extends JpaRepository<Encadreur, Integer> {

    Optional<Encadreur> findByUtilisateurId(Integer utilisateurId);
}