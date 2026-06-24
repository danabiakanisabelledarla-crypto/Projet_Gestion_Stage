package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.Projet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjetRepository extends JpaRepository<Projet, Integer> {
}
