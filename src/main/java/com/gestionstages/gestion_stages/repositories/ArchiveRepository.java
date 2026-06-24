package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.Archive;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArchiveRepository extends JpaRepository<Archive, Integer> {
}