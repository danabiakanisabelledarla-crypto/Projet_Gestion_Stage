package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Integer> {

    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p.id = :userId ORDER BY c.dernierMessage DESC NULLS LAST, c.dateCreation DESC")
    List<Conversation> findByParticipantIdOrderByDernierMessageDesc(@Param("userId") Integer userId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :convId AND m.expediteur.id <> :userId AND m.lu = false")
    long countNonLuByConversation(@Param("convId") Integer convId, @Param("userId") Integer userId);
}
