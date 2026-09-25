package ue.edu.co.splitbill.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ue.edu.co.splitbill.entity.Message;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    /** Todo el chat del grupo, en el orden en que se escribio. */
    List<Message> findByGroupIdOrderBySentAtAsc(UUID groupId);

    /** Solo lo que llego al servidor despues de la fecha dada. */
    List<Message> findByGroupIdAndCreatedAtAfterOrderBySentAtAsc(UUID groupId, Instant createdAt);
}
