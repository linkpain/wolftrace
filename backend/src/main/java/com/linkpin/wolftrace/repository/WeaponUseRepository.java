package com.linkpin.wolftrace.repository;

import com.linkpin.wolftrace.model.WeaponUse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WeaponUseRepository extends JpaRepository<WeaponUse, Long> {
    List<WeaponUse> findByRoomIdAndRoundNumber(Long roomId, Integer roundNumber);
    List<WeaponUse> findByAttackerIdAndRoundNumber(String attackerId, Integer roundNumber);
    List<WeaponUse> findByTargetIdAndRoundNumber(String targetId, Integer roundNumber);
    void deleteByRoomIdAndRoundNumber(Long roomId, Integer roundNumber);
}
