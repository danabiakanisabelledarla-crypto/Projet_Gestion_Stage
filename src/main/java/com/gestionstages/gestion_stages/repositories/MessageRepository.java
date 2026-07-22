package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Integer> {

    List<Message> findByConversationIdOrderByDateEnvoiAsc(Integer conversationId);
}
