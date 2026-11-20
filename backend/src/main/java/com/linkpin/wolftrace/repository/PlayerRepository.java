package com.linkpin.wolftrace.repository;

import com.linkpin.wolftrace.model.Player;
import com.linkpin.wolftrace.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    Player findByPlayerId(String playerId);
    Player findBySessionId(String sessionId);
    List<Player> findByRoom(Room room);
    void deleteBySessionId(String sessionId);
}
