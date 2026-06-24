package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByLibelle(String libelle);
}