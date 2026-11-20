package com.linkpin.wolftrace.repository;

import com.linkpin.wolftrace.model.RoomSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomSelectionRepository extends JpaRepository<RoomSelection, Long> {
    List<RoomSelection> findByRoomIdAndRoundNumber(Long roomId, Integer roundNumber);
    List<RoomSelection> findByPlayerIdAndRoundNumber(String playerId, Integer roundNumber);
    void deleteByRoomIdAndRoundNumber(Long roomId, Integer roundNumber);
}
