package com.linkpin.wolftrace.repository;

import com.linkpin.wolftrace.model.ShieldUse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShieldUseRepository extends JpaRepository<ShieldUse, Long> {
    List<ShieldUse> findByRoomIdAndRoundNumber(Long roomId, Integer roundNumber);
    List<ShieldUse> findByUserIdAndRoundNumber(String userId, Integer roundNumber);
    List<ShieldUse> findByTargetIdAndRoundNumber(String targetId, Integer roundNumber);
    void deleteByRoomIdAndRoundNumber(Long roomId, Integer roundNumber);
}
