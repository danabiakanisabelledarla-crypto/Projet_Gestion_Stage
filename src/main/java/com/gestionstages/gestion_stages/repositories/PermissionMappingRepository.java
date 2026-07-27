package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.PermissionMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

public interface PermissionMappingRepository extends JpaRepository<PermissionMapping, Integer> {
    Optional<PermissionMapping> findByPermissionNom(String permissionNom);
    Optional<PermissionMapping> findByCheminPage(String cheminPage);
    Set<PermissionMapping> findByActifTrue();
    boolean existsByPermissionNom(String permissionNom);
    boolean existsByCheminPage(String cheminPage);
}
