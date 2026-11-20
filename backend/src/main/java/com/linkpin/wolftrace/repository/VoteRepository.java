package com.linkpin.wolftrace.repository;

import com.linkpin.wolftrace.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    List<Vote> findByRoomIdAndRoundNumber(Long roomId, Integer roundNumber);
    List<Vote> findByVoterIdAndRoundNumber(String voterId, Integer roundNumber);
    List<Vote> findByTargetIdAndRoundNumber(String targetId, Integer roundNumber);
    void deleteByRoomIdAndRoundNumber(Long roomId, Integer roundNumber);
}
