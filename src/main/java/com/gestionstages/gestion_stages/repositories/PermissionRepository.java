package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Integer> {
    Optional<Permission> findByCode(String code);
    Optional<Permission> findByNom(String nom);
    boolean existsByNomIgnoreCase(String nom);
}
