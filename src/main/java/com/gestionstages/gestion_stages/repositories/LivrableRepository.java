package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.Livrable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LivrableRepository extends JpaRepository<Livrable, Integer> {

    List<Livrable> findByTacheId(Integer tacheId);
}