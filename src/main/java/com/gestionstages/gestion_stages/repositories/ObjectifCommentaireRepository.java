package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.ObjectifCommentaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ObjectifCommentaireRepository extends JpaRepository<ObjectifCommentaire, Integer> {

    List<ObjectifCommentaire> findByObjectifIdOrderByDateCommentaireAsc(Integer objectifId);
}
