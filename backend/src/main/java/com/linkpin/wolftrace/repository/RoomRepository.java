package com.linkpin.wolftrace.repository;

import com.linkpin.wolftrace.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Room findByRoomId(String roomId);
}
