package com.linkpin.wolftrace.repository;

import com.linkpin.wolftrace.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByRoomIdAndRoundNumberOrderByCreatedAtAsc(Long roomId, Integer roundNumber);
    List<Message> findByRoomIdOrderByCreatedAtAsc(Long roomId);
    void deleteByRoomId(Long roomId);
}
