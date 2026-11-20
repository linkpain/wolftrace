package com.linkpin.wolftrace.controller;

import com.linkpin.wolftrace.model.Room;
import com.linkpin.wolftrace.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("")
@CrossOrigin(origins = "*")
public class GameController {

    @Autowired
    private GameService gameService;

    @GetMapping("/rooms")
    public Map<String, Object> getRooms() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", gameService.getAllRooms());
        return result;
    }

    @GetMapping("/rooms/{roomId}")
    public Map<String, Object> getRoom(@PathVariable String roomId) {
        Room room = gameService.getRoom(roomId);
        if (room == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "房间不存在");
            return result;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", gameService.convertRoomToDTO(room));
        return result;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "ok");
        return result;
    }
}
