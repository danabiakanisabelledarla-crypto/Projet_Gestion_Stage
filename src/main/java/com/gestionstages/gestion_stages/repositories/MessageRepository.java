package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Integer> {

    List<Message> findByConversationIdOrderByDateEnvoiAsc(Integer conversationId);
    List<Message> findByConversationIdAndIdGreaterThanOrderByDateEnvoiAsc(
            Integer conversationId, Integer messageId);
    Optional<Message> findFirstByConversationIdOrderByDateEnvoiDesc(Integer conversationId);
}
