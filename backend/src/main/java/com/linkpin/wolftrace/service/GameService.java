package com.linkpin.wolftrace.service;

import com.linkpin.wolftrace.dto.GameMessage;
import com.linkpin.wolftrace.model.*;
import com.linkpin.wolftrace.repository.*;
import com.linkpin.wolftrace.websocket.GameWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class GameService {

    private static final Logger logger = LoggerFactory.getLogger(GameService.class);

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToPlayerMap = new ConcurrentHashMap<>();
    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final RoomSelectionRepository roomSelectionRepository;
    private final WeaponUseRepository weaponUseRepository;
    private final ShieldUseRepository shieldUseRepository;
    private final MessageRepository messageRepository;
    private final VoteRepository voteRepository;
    private final ApplicationContext applicationContext;

    @Autowired
    public GameService(RoomRepository roomRepository, 
                     PlayerRepository playerRepository,
                     RoomSelectionRepository roomSelectionRepository,
                     WeaponUseRepository weaponUseRepository,
                     ShieldUseRepository shieldUseRepository,
                     MessageRepository messageRepository,
                     VoteRepository voteRepository,
                     ApplicationContext applicationContext) {
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
        this.roomSelectionRepository = roomSelectionRepository;
        this.weaponUseRepository = weaponUseRepository;
        this.shieldUseRepository = shieldUseRepository;
        this.messageRepository = messageRepository;
        this.voteRepository = voteRepository;
        this.applicationContext = applicationContext;
    }

    private GameWebSocketHandler getWebSocketHandler() {
        return applicationContext.getBean(GameWebSocketHandler.class);
    }

    public List<Map<String, Object>> getAllRooms() {
        List<Room> rooms = roomRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Room room : rooms) {
            result.add(convertRoomToDTO(room));
        }
        return result;
    }

    public Room getRoom(String roomId) {
        return roomRepository.findByRoomId(roomId);
    }

    @Transactional
    public void handleCreateRoom(WebSocketSession session, Map<String, Object> data) {
        String roomName = (String) data.get("roomName");
        String playerName = (String) data.get("playerName");
        Integer maxPlayers = (Integer) data.getOrDefault("maxPlayers", 8);

        System.out.println("开始创建房间 - roomName: " + roomName + ", playerName: " + playerName + ", maxPlayers: " + maxPlayers);

        String roomId = UUID.randomUUID().toString();
        Room room = new Room();
        room.setRoomId(roomId);
        room.setName(roomName);
        room.setMaxPlayers(maxPlayers);
        room.setGamePhase(Room.GamePhase.WAITING);
        room.setCurrentRound(0);
        room.setIsGameOver(false);
        roomRepository.save(room);

        Room savedRoom = roomRepository.findByRoomId(roomId);
        Player player = createPlayer(session, playerName);
        player.setRoom(savedRoom);
        playerRepository.save(player);

        savedRoom.setHostId(player.getPlayerId());
        roomRepository.save(savedRoom);

        player.setIsReady(true);
        playerRepository.save(player);

        session.getAttributes().put("roomId", roomId);
        session.getAttributes().put("playerId", player.getPlayerId());
        sessionToPlayerMap.put(session.getId(), player.getPlayerId());

        Room updatedRoom = roomRepository.findByRoomId(roomId);
        GameMessage response = new GameMessage();
        response.setType("ROOM_CREATED");
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("roomId", roomId);
        responseData.put("room", convertRoomToDTO(updatedRoom));
        response.setData(responseData);
        response.setTimestamp(System.currentTimeMillis());

        System.out.println("准备发送ROOM_CREATED消息 - roomId: " + roomId + ", playerId: " + player.getPlayerId());
        System.out.println("房间DTO: " + convertRoomToDTO(updatedRoom));
        getWebSocketHandler().sendToSession(session, response);
        System.out.println("房间创建成功: " + roomId);
    }

    @Transactional
    public void handleJoinRoom(WebSocketSession session, Map<String, Object> data) {
        String roomId = (String) data.get("roomId");
        String playerName = (String) data.get("playerName");

        Room room = roomRepository.findByRoomId(roomId);
        if (room == null) {
            sendError(session, "房间不存在");
            return;
        }

        List<Player> players = playerRepository.findByRoom(room);
        if (players.size() >= room.getMaxPlayers()) {
            sendError(session, "房间已满");
            return;
        }

        Player player = createPlayer(session, playerName);
        player.setRoom(room);
        playerRepository.save(player);

        session.getAttributes().put("roomId", roomId);
        session.getAttributes().put("playerId", player.getPlayerId());
        sessionToPlayerMap.put(session.getId(), player.getPlayerId());

        Room updatedRoom = roomRepository.findByRoomId(roomId);
        GameMessage response = new GameMessage();
        response.setType("PLAYER_JOINED");
        Map<String, Object> joinData = new HashMap<>();
        joinData.put("player", convertPlayerToDTO(player));
        joinData.put("room", convertRoomToDTO(updatedRoom));
        response.setData(joinData);
        response.setTimestamp(System.currentTimeMillis());

        getWebSocketHandler().broadcastToRoom(roomId, response);
        System.out.println("玩家加入房间: " + playerName);
    }

    @Transactional
    public void handleStartGame(WebSocketSession session, Map<String, Object> data) {
        String roomId = (String) data.get("roomId");
        String playerId = (String) data.get("playerId");
        
        if (roomId == null) {
            roomId = (String) session.getAttributes().get("roomId");
        }
        if (playerId == null) {
            playerId = (String) session.getAttributes().get("playerId");
        }
        
        Room room = roomRepository.findByRoomId(roomId);

        if (room == null) {
            sendError(session, "房间不存在");
            return;
        }

        if (!room.getHostId().equals(playerId)) {
            sendError(session, "只有房主可以开始游戏");
            return;
        }

        List<Player> players = playerRepository.findByRoom(room);
        if (players.size() < 4) {
            sendError(session, "至少需要4名玩家才能开始游戏");
            return;
        }

        boolean allReady = players.stream().allMatch(p -> p.getIsReady());
        if (!allReady) {
            sendError(session, "所有玩家必须准备后才能开始游戏");
            return;
        }

        assignNames(players);
        assignRoles(room, players);
        assignSeats(players);
        
        for (Player player : players) {
            player.setHasMadeSkillChoice(false);
        }
        
        room.setGamePhase(Room.GamePhase.NICKNAME_REVEAL);
        roomRepository.save(room);
        playerRepository.saveAll(players);

        Room updatedRoom = roomRepository.findByRoomId(roomId);
        GameMessage response = new GameMessage();
        response.setType("GAME_STARTED");
        Map<String, Object> gameData = new HashMap<>();
        gameData.put("room", convertRoomToDTO(updatedRoom));
        response.setData(gameData);
        response.setTimestamp(System.currentTimeMillis());

        getWebSocketHandler().broadcastToRoom(roomId, response);
        System.out.println("游戏开始: " + roomId);
    }

    @Transactional
    public void handleNicknameRevealConfirmed(WebSocketSession session, Map<String, Object> data) {
        String roomId = (String) data.get("roomId");
        String playerId = (String) data.get("playerId");
        
        if (roomId == null) {
            roomId = (String) session.getAttributes().get("roomId");
        }
        if (playerId == null) {
            playerId = (String) session.getAttributes().get("playerId");
        }

        Room room = roomRepository.findByRoomId(roomId);
        if (room == null) {
            sendError(session, "房间不存在");
            return;
        }

        room.setGamePhase(Room.GamePhase.ROLE_ANNOUNCEMENT);
        roomRepository.save(room);

        Room updatedRoom = roomRepository.findByRoomId(roomId);
        GameMessage response = new GameMessage();
        response.setType("ROLE_ANNOUNCEMENT_PHASE");
        Map<String, Object> roomData = new HashMap<>();
        roomData.put("room", convertRoomToDTO(updatedRoom));
        response.setData(roomData);
        response.setTimestamp(System.currentTimeMillis());

        getWebSocketHandler().broadcastToRoom(roomId, response);
    }

    @Transactional
    public void handleRoleAnnouncementConfirmed(WebSocketSession session, Map<String, Object> data) {
        String roomId = (String) data.get("roomId");
        String playerId = (String) data.get("playerId");
        
        if (roomId == null) {
            roomId = (String) session.getAttributes().get("roomId");
        }
        if (playerId == null) {
            playerId = (String) session.getAttributes().get("playerId");
        }

        Room room = roomRepository.findByRoomId(roomId);
        if (room == null) {
            sendError(session, "房间不存在");
            return;
        }

        room.setGamePhase(Room.GamePhase.ROOM_SELECTION);
        room.setCurrentRound(1);
        roomRepository.save(room);

        Room updatedRoom = roomRepository.findByRoomId(roomId);
        GameMessage response = new GameMessage();
        response.setType("ROOM_SELECTION_PHASE");
        Map<String, Object> roomData = new HashMap<>();
        roomData.put("room", convertRoomToDTO(updatedRoom));
        response.setData(roomData);
        response.setTimestamp(System.currentTimeMillis());

        getWebSocketHandler().broadcastToRoom(roomId, response);
    }

    @Transactional
    public void handleRoomSelection(WebSocketSession session, Map<String, Object> data) {
        String roomId = (String) data.get("roomId");
        String playerId = (String) data.get("playerId");
        String roomType = (String) data.get("roomType");
        
        if (roomId == null) {
            roomId = (String) session.getAttributes().get("roomId");
        }
        if (playerId == null) {
            playerId = (String) session.getAttributes().get("playerId");
        }

        Room room = roomRepository.findByRoomId(roomId);
        if (room == null) {
            sendError(session, "房间不存在");
            return;
        }

        Player player = playerRepository.findByPlayerId(playerId);
        if (player == null) {
            sendError(session, "玩家不存在");
            return;
        }

        Integer lastRoom = player.getLastRoom();
        if (lastRoom != null && lastRoom != -1 && lastRoom.toString().equals(roomType)) {
            sendError(session, "不能连续进入同一房间");
            return;
        }

        RoomSelection roomSelection = new RoomSelection();
        roomSelection.setRoom(room);
        roomSelection.setPlayerId(playerId);
        roomSelection.setRoundNumber(room.getCurrentRound());
        roomSelection.setRoomType(roomType);
        roomSelectionRepository.save(roomSelection);

        player.setLastRoom(lastRoom);
        player.setCurrentRoom(Integer.parseInt(roomType));
        playerRepository.save(player);

        List<Player> players = playerRepository.findByRoom(room);
        boolean allSelected = players.stream().allMatch(p -> p.getCurrentRoom() != null && p.getCurrentRoom() != -1);

        if (allSelected) {
            for (Player p : players) {
                p.setHasMadeSkillChoice(false);
            }
            playerRepository.saveAll(players);
            
            room.setGamePhase(Room.GamePhase.SKILL_SELECTION);
            roomRepository.save(room);

            Room updatedRoom = roomRepository.findByRoomId(roomId);
            GameMessage response = new GameMessage();
            response.setType("SKILL_SELECTION_PHASE");
            Map<String, Object> skillData = new HashMap<>();
            skillData.put("room", convertRoomToDTO(updatedRoom));
            response.setData(skillData);
            response.setTimestamp(System.currentTimeMillis());

            getWebSocketHandler().broadcastToRoom(roomId, response);
        } else {
            Room updatedRoom = roomRepository.findByRoomId(roomId);
            GameMessage response = new GameMessage();
            response.setType("ROOM_SELECTED");
            Map<String, Object> roomData = new HashMap<>();
            roomData.put("room", convertRoomToDTO(updatedRoom));
            roomData.put("playerId", playerId);
            roomData.put("roomType", roomType);
            response.setData(roomData);
            response.setTimestamp(System.currentTimeMillis());

            getWebSocketHandler().broadcastToRoom(roomId, response);
        }
    }

    @Transactional
    public void handleWeaponUse(WebSocketSession session, Map<String, Object> data) {
        String roomId = (String) data.get("roomId");
        String playerId = (String) data.get("playerId");
        String weaponType = (String) data.get("weaponType");
        
        if (roomId == null) {
            roomId = (String) session.getAttributes().get("roomId");
        }
        if (playerId == null) {
            playerId = (String) session.getAttributes().get("playerId");
        }

        Room room = roomRepository.findByRoomId(roomId);
        if (room == null) {
            sendError(session, "房间不存在");
            return;
        }

        Player attacker = playerRepository.findByPlayerId(playerId);
        if (attacker == null || !attacker.getIsAlive()) {
            sendError(session, "玩家不存在或已出局");
            return;
        }

        if (!attacker.getRole().equals(Player.Role.WOLF) && !attacker.getRole().equals(Player.Role.WOLF_KING)) {
            sendError(session, "只有狼人阵营可以使用武器");
            return;
        }

        if ("FATAL".equals(weaponType)) {
            if (attacker.getFatalWeapon() <= 0) {
                sendError(session, "必杀刀不足");
                return;
            }
            if (room.getCurrentRound() != 2 && room.getCurrentRound() != 3) {
                sendError(session, "必杀刀只能在第2或第3轮使用");
                return;
            }
            attacker.setFatalWeapon(attacker.getFatalWeapon() - 1);
        } else {
            if (attacker.getNormalWeapons() <= 0) {
                sendError(session, "普通刀不足");
                return;
            }
            attacker.setNormalWeapons(attacker.getNormalWeapons() - 1);
        }

        Integer attackerRoom = attacker.getCurrentRoom();
        
        List<Player> players = playerRepository.findByRoom(room);
        Player villageHead = players.stream()
            .filter(p -> p.getRole().equals(Player.Role.VILLAGE_HEAD) && p.getIsAlive())
            .findFirst()
            .orElse(null);

        boolean isSuccessful = false;
        String targetPlayerId = null;

        if (villageHead != null && villageHead.getCurrentRoom().equals(attackerRoom)) {
            if (villageHead.getShields() > 0) {
                villageHead.setShields(villageHead.getShields() - 1);
                isSuccessful = false;
                targetPlayerId = villageHead.getPlayerId();
            } else {
                villageHead.setIsAlive(false);
                isSuccessful = true;
                targetPlayerId = villageHead.getPlayerId();
            }
        } else {
            isSuccessful = false;
        }

        WeaponUse weaponUse = new WeaponUse();
        weaponUse.setRoom(room);
        weaponUse.setAttackerId(playerId);
        weaponUse.setTargetId(targetPlayerId);
        weaponUse.setWeaponType(weaponType);
        weaponUse.setRoundNumber(room.getCurrentRound());
        weaponUse.setIsSuccessful(isSuccessful);
        weaponUseRepository.save(weaponUse);

        attacker.setHasMadeSkillChoice(true);
        playerRepository.save(attacker);
        if (villageHead != null) {
            playerRepository.save(villageHead);
        }

        Room updatedRoom = roomRepository.findByRoomId(room.getRoomId());
        
        GameMessage response = new GameMessage();
        response.setType("WEAPON_USED");
        Map<String, Object> weaponData = new HashMap<>();
        weaponData.put("attackerId", playerId);
        weaponData.put("targetId", targetPlayerId);
        weaponData.put("weaponType", weaponType);
        weaponData.put("isSuccessful", isSuccessful);
        weaponData.put("room", convertRoomToDTO(updatedRoom));
        response.setData(weaponData);
        response.setTimestamp(System.currentTimeMillis());

        getWebSocketHandler().sendToSession(session, response);
        checkAllSkillsUsed(room);
    }

    @Transactional
    public void handleShieldUse(WebSocketSession session, Map<String, Object> data) {
        logger.info("handleShieldUse called - data: {}", data);
        
        String roomId = (String) data.get("roomId");
        String playerId = (String) data.get("playerId");
        String targetId = (String) data.get("targetId");
        
        logger.info("handleShieldUse - roomId: {}, playerId: {}, targetId: {}", roomId, playerId, targetId);
        
        if (roomId == null) {
            roomId = (String) session.getAttributes().get("roomId");
        }
        if (playerId == null) {
            playerId = (String) session.getAttributes().get("playerId");
        }

        logger.info("handleShieldUse - final roomId: {}, final playerId: {}", roomId, playerId);

        Room room = roomRepository.findByRoomId(roomId);
        if (room == null) {
            sendError(session, "房间不存在");
            return;
        }

        Player user = playerRepository.findByPlayerId(playerId);
        if (user == null || !user.getIsAlive()) {
            sendError(session, "玩家不存在或已出局");
            return;
        }

        Player target = playerRepository.findByPlayerId(targetId);
        if (target == null) {
            sendError(session, "目标玩家不存在");
            return;
        }

        if (user.getShields() <= 0) {
            sendError(session, "护盾不足");
            return;
        }

        logger.info("Before: user shields = {}", user.getShields());
        user.setShields(user.getShields() - 1);
        logger.info("After: user shields = {}", user.getShields());

        Integer userRoom = user.getCurrentRoom();
        Integer targetRoom = target.getCurrentRoom();
        boolean isEffective = (userRoom != null && targetRoom != null) && userRoom.equals(targetRoom);

        ShieldUse shieldUse = new ShieldUse();
        shieldUse.setRoom(room);
        shieldUse.setUserId(playerId);
        shieldUse.setTargetId(targetId);
        shieldUse.setRoundNumber(room.getCurrentRound());
        shieldUse.setIsSuccessful(true);
        shieldUse.setIsEffective(isEffective);
        shieldUseRepository.save(shieldUse);

        user.setHasMadeSkillChoice(true);
        playerRepository.save(user);

        logger.info("After save: user shields = {}", user.getShields());

        Room updatedRoom = roomRepository.findByRoomId(room.getRoomId());
        Player updatedUser = playerRepository.findByPlayerId(playerId);
        logger.info("After reload: updatedUser shields = {}", updatedUser.getShields());
        
        GameMessage response = new GameMessage();
        response.setType("SHIELD_USED");
        Map<String, Object> shieldData = new HashMap<>();
        shieldData.put("userId", playerId);
        shieldData.put("targetId", targetId);
        shieldData.put("isEffective", isEffective);
        shieldData.put("room", convertRoomToDTO(updatedRoom));
        response.setData(shieldData);
        response.setTimestamp(System.currentTimeMillis());

        getWebSocketHandler().sendToSession(session, response);
        checkAllSkillsUsed(room);
    }

    @Transactional
    public void handleSkipSkill(WebSocketSession session, Map<String, Object> data) {
        String roomId = (String) data.get("roomId");
        String playerId = (String) data.get("playerId");
        
        if (roomId == null) {
            roomId = (String) session.getAttributes().get("roomId");
        }
        if (playerId == null) {
            playerId = (String) session.getAttributes().get("playerId");
        }

        Room room = roomRepository.findByRoomId(roomId);
        if (room == null) {
            sendError(session, "房间不存在");
            return;
        }

        Player player = playerRepository.findByPlayerId(playerId);
        if (player == null || !player.getIsAlive()) {
            sendError(session, "玩家不存在或已出局");
            return;
        }

        player.setHasMadeSkillChoice(true);
        playerRepository.save(player);

        Room updatedRoom = roomRepository.findByRoomId(room.getRoomId());
        
        GameMessage response = new GameMessage();
        response.setType("SKILL_SKIPPED");
        Map<String, Object> skipData = new HashMap<>();
        skipData.put("playerId", playerId);
        skipData.put("room", convertRoomToDTO(updatedRoom));
        response.setData(skipData);
        response.setTimestamp(System.currentTimeMillis());

        getWebSocketHandler().sendToSession(session, response);
        checkAllSkillsUsed(room);
    }

    @Transactional
    public void checkAllSkillsUsed(Room room) {
        List<Player> players = playerRepository.findByRoom(room);
        List<Player> wolfPlayers = players.stream()
            .filter(p -> p.getIsAlive() && (p.getRole() == Player.Role.WOLF_KING || p.getRole() == Player.Role.WOLF))
            .collect(Collectors.toList());

        List<Player> villagePlayers = players.stream()
            .filter(p -> p.getIsAlive() && (p.getRole() == Player.Role.VILLAGE_HEAD || p.getRole() == Player.Role.VILLAGER))
            .collect(Collectors.toList());

        List<WeaponUse> weaponUses = weaponUseRepository.findByRoomIdAndRoundNumber(room.getId(), room.getCurrentRound());
        List<ShieldUse> shieldUses = shieldUseRepository.findByRoomIdAndRoundNumber(room.getId(), room.getCurrentRound());

        boolean allWolfMadeChoice = wolfPlayers.stream()
            .allMatch(Player::getHasMadeSkillChoice);

        boolean allVillageMadeChoice = villagePlayers.stream()
            .allMatch(Player::getHasMadeSkillChoice);

        System.out.println("checkAllSkillsUsed - Room: " + room.getRoomId() + ", Round: " + room.getCurrentRound());
        System.out.println("Wolf players: " + wolfPlayers.size() + ", All made choice: " + allWolfMadeChoice);
        for (Player p : wolfPlayers) {
            System.out.println("  Wolf: " + p.getPlayerId() + ", hasMadeSkillChoice: " + p.getHasMadeSkillChoice());
        }
        System.out.println("Village players: " + villagePlayers.size() + ", All made choice: " + allVillageMadeChoice);
        for (Player p : villagePlayers) {
            System.out.println("  Village: " + p.getPlayerId() + ", hasMadeSkillChoice: " + p.getHasMadeSkillChoice());
        }

        if (allWolfMadeChoice && allVillageMadeChoice) {
            System.out.println("All players made skill choice, processing night result");
            processNightResult(room);
        }
    }

    @Transactional
    public void processNightResult(Room room) {
        List<WeaponUse> weaponUses = weaponUseRepository.findByRoomIdAndRoundNumber(room.getId(), room.getCurrentRound());
        List<ShieldUse> shieldUses = shieldUseRepository.findByRoomIdAndRoundNumber(room.getId(), room.getCurrentRound());
        List<Player> players = playerRepository.findByRoom(room);

        Map<String, Integer> shieldCounts = new HashMap<>();
        for (ShieldUse shieldUse : shieldUses) {
            shieldCounts.put(shieldUse.getTargetId(), shieldCounts.getOrDefault(shieldUse.getTargetId(), 0) + 1);
        }

        List<String> deadPlayers = new ArrayList<>();
        for (WeaponUse weaponUse : weaponUses) {
            Player target = players.stream()
                .filter(p -> p.getPlayerId().equals(weaponUse.getTargetId()))
                .findFirst()
                .orElse(null);

            if (target != null && target.getRole() != Player.Role.VILLAGE_HEAD) {
                weaponUse.setIsSuccessful(false);
                continue;
            }

            int shieldCount = shieldCounts.getOrDefault(weaponUse.getTargetId(), 0);
            if ("FATAL".equals(weaponUse.getWeaponType())) {
                weaponUse.setIsSuccessful(true);
                if (!deadPlayers.contains(weaponUse.getTargetId())) {
                    deadPlayers.add(weaponUse.getTargetId());
                }
            } else {
                if (shieldCount > 0) {
                    weaponUse.setIsSuccessful(false);
                    ShieldUse shieldUse = shieldUses.stream()
                        .filter(s -> s.getTargetId().equals(weaponUse.getTargetId()))
                        .findFirst()
                        .orElse(null);
                    if (shieldUse != null) {
                        shieldUse.setIsSuccessful(true);
                        shieldUseRepository.save(shieldUse);
                    }
                } else {
                    weaponUse.setIsSuccessful(true);
                    if (!deadPlayers.contains(weaponUse.getTargetId())) {
                        deadPlayers.add(weaponUse.getTargetId());
                    }
                }
            }
            weaponUseRepository.save(weaponUse);
        }

        for (String playerId : deadPlayers) {
            Player player = players.stream()
                .filter(p -> p.getPlayerId().equals(playerId))
                .findFirst()
                .orElse(null);
            if (player != null) {
                player.setIsAlive(false);
                playerRepository.save(player);
            }
        }

        for (Player player : players) {
            player.setHasMadeSkillChoice(false);
        }
        playerRepository.saveAll(players);

        room.setGamePhase(Room.GamePhase.NIGHT_RESULT);
        roomRepository.save(room);

        Map<String, List<String>> roomPlayersMap = getRoomPlayersMap(room, room.getCurrentRound());

        Room updatedRoom = roomRepository.findByRoomId(room.getRoomId());
        GameMessage response = new GameMessage();
        response.setType("NIGHT_RESULT");
        Map<String, Object> nightData = new HashMap<>();
        nightData.put("room", convertRoomToDTO(updatedRoom));
        nightData.put("deadPlayers", deadPlayers);
        nightData.put("roomPlayers", roomPlayersMap);
        nightData.put("round", room.getCurrentRound());
        response.setData(nightData);
        response.setTimestamp(System.currentTimeMillis());

        getWebSocketHandler().broadcastToRoom(room.getRoomId(), response);

        checkGameEnd(room);
    }

    @Transactional
    public void handleNightResultConfirmed(WebSocketSession session, Map<String, Object> data) {
        String roomId = (String) session.getAttributes().get("roomId");

        Room room = roomRepository.findByRoomId(roomId);
        if (room == null) {
            sendError(session, "房间不存在");
            return;
        }

        room.setGamePhase(Room.GamePhase.DISCUSSION);
        roomRepository.save(room);

        Room updatedRoom = roomRepository.findByRoomId(roomId);
        GameMessage response = new GameMessage();
        response.setType("DISCUSSION_PHASE");
        Map<String, Object> discussionData = new HashMap<>();
        discussionData.put("room", convertRoomToDTO(updatedRoom));
        response.setData(discussionData);
        response.setTimestamp(System.currentTimeMillis());

        getWebSocketHandler().broadcastToRoom(roomId, response);
    }

    @Transactional
    public void handleVote(WebSocketSession session, Map<String, Object> data) {
        String roomId = (String) session.getAttributes().get("roomId");
        String playerId = (String) session.getAttributes().get("playerId");
        String targetId = (String) data.get("targetId");

        Room room = roomRepository.findByRoomId(roomId);
        if (room == null) {
            sendError(session, "房间不存在");
            return;
        }

        Player player = playerRepository.findByPlayerId(playerId);
        if (player == null || !player.getIsAlive()) {
            sendError(session, "玩家不存在或已出局");
            return;
        }

        Vote vote = new Vote();
        vote.setRoom(room);
        vote.setVoterId(playerId);
        vote.setTargetId(targetId);
        vote.setRoundNumber(room.getCurrentRound());
        voteRepository.save(vote);

        GameMessage response = new GameMessage();
        response.setType("VOTE_CAST");
        Map<String, Object> voteData = new HashMap<>();
        voteData.put("voterId", playerId);
        voteData.put("targetId", targetId);
        response.setData(voteData);
        response.setTimestamp(System.currentTimeMillis());

        getWebSocketHandler().broadcastToRoom(roomId, response);

        int alivePlayerCount = (int) playerRepository.findByRoom(room).stream()
            .filter(Player::getIsAlive)
            .count();

        List<Vote> votes = voteRepository.findByRoomIdAndRoundNumber(room.getId(), room.getCurrentRound());
        if (votes.size() >= alivePlayerCount) {
            processVotingResult(room);
        }
    }

    @Transactional
    public void processVotingResult(Room room) {
        List<Vote> votes = voteRepository.findByRoomIdAndRoundNumber(room.getId(), room.getCurrentRound());
        Map<String, Integer> voteCount = new HashMap<>();
        for (Vote vote : votes) {
            voteCount.put(vote.getTargetId(), voteCount.getOrDefault(vote.getTargetId(), 0) + 1);
        }

        String eliminatedId = Collections.max(voteCount.entrySet(), 
            Comparator.comparingInt(Map.Entry::getValue)).getKey();

        Player eliminatedPlayer = playerRepository.findByPlayerId(eliminatedId);
        if (eliminatedPlayer != null) {
            eliminatedPlayer.setIsAlive(false);
            playerRepository.save(eliminatedPlayer);
        }

        room.setGamePhase(Room.GamePhase.GAME_OVER);
        room.setIsGameOver(true);
        
        if (eliminatedPlayer != null && eliminatedPlayer.getRole() == Player.Role.WOLF_KING) {
            room.setWinner("VILLAGE");
        } else {
            room.setWinner("WOLF");
        }
        
        roomRepository.save(room);

        Room updatedRoom = roomRepository.findByRoomId(room.getRoomId());
        GameMessage response = new GameMessage();
        response.setType("GAME_OVER");
        Map<String, Object> gameOverData = new HashMap<>();
        gameOverData.put("room", convertRoomToDTO(updatedRoom));
        gameOverData.put("eliminatedId", eliminatedId);
        gameOverData.put("voteCount", voteCount);
        gameOverData.put("winner", room.getWinner());
        response.setData(gameOverData);
        response.setTimestamp(System.currentTimeMillis());

        getWebSocketHandler().broadcastToRoom(room.getRoomId(), response);
    }

    @Transactional
    public void handleReady(WebSocketSession session, Map<String, Object> data) {
        String roomId = (String) data.get("roomId");
        String playerId = (String) data.get("playerId");
        
        if (roomId == null) {
            roomId = (String) session.getAttributes().get("roomId");
        }
        if (playerId == null) {
            playerId = (String) session.getAttributes().get("playerId");
        }
        
        Room room = roomRepository.findByRoomId(roomId);

        if (room == null) {
            sendError(session, "房间不存在");
            return;
        }

        Player player = playerRepository.findByPlayerId(playerId);
        if (player == null) {
            sendError(session, "玩家不存在");
            return;
        }

        player.setIsReady(!player.getIsReady());
        playerRepository.save(player);

        Room updatedRoom = roomRepository.findByRoomId(roomId);
        GameMessage response = new GameMessage();
        response.setType("PLAYER_READY");
        Map<String, Object> readyData = new HashMap<>();
        readyData.put("playerId", playerId);
        readyData.put("isReady", player.getIsReady());
        readyData.put("room", convertRoomToDTO(updatedRoom));
        response.setData(readyData);
        response.setTimestamp(System.currentTimeMillis());

        getWebSocketHandler().broadcastToRoom(roomId, response);
    }

    @Transactional
    public void handleDisconnect(WebSocketSession session) {
        String playerId = sessionToPlayerMap.get(session.getId());
        String roomId = (String) session.getAttributes().get("roomId");

        if (playerId != null && roomId != null) {
            Player player = playerRepository.findByPlayerId(playerId);
            if (player != null) {
                Room room = roomRepository.findByRoomId(roomId);
                if (room != null) {
                    List<Player> players = playerRepository.findByRoom(room);
                    if (players.isEmpty()) {
                        roomRepository.delete(room);
                    } else {
                        GameMessage response = new GameMessage();
                        response.setType("PLAYER_LEFT");
                        Map<String, Object> leftData = new HashMap<>();
                        leftData.put("playerId", playerId);
                        leftData.put("playerName", player.getName());
                        response.setData(leftData);
                        response.setTimestamp(System.currentTimeMillis());

                        getWebSocketHandler().broadcastToRoom(roomId, response);
                    }
                }
            }
        }
        sessionToPlayerMap.remove(session.getId());
    }

    private Player createPlayer(WebSocketSession session, String playerName) {
        Player player = new Player();
        player.setPlayerId(UUID.randomUUID().toString());
        player.setSessionId(session.getId());
        player.setRealName(playerName);
        player.setSeatNumber(0);
        player.setRole(Player.Role.VILLAGER);
        player.setIsAlive(true);
        player.setIsReady(false);
        player.setNormalWeapons(0);
        player.setFatalWeapon(0);
        player.setShields(0);
        player.setHasMadeSkillChoice(false);
        player.setLastRoom(-1);
        player.setCurrentRoom(-1);
        return player;
    }

    private void assignRoles(Room room, List<Player> players) {
        int playerCount = players.size();
        int wolfCount = 0;
        if (playerCount >= 10) {
            wolfCount = 2;
        } else if (playerCount >= 9) {
            wolfCount = 1;
        }

        Collections.shuffle(players);

        players.get(0).setRole(Player.Role.WOLF_KING);
        players.get(0).setNormalWeapons(1);
        players.get(0).setFatalWeapon(1);

        for (int i = 1; i <= wolfCount; i++) {
            players.get(i).setRole(Player.Role.WOLF);
            players.get(i).setNormalWeapons(3);
            players.get(i).setFatalWeapon(0);
        }

        players.get(wolfCount + 1).setRole(Player.Role.VILLAGE_HEAD);
        players.get(wolfCount + 1).setShields(2);

        for (int i = wolfCount + 2; i < players.size(); i++) {
            players.get(i).setRole(Player.Role.VILLAGER);
            players.get(i).setShields(1);
        }
    }

    private void assignSeats(List<Player> players) {
        players.sort((p1, p2) -> Long.compare(p1.getCreatedAt(), p2.getCreatedAt()));
        for (int i = 0; i < players.size(); i++) {
            players.get(i).setSeatNumber(i + 1);
        }
    }

    private void assignNames(List<Player> players) {
        System.out.println("开始分配昵称，玩家数量: " + players.size());
        
        System.out.println("分配昵称前的玩家列表（未排序）:");
        for (Player player : players) {
            System.out.println("  玩家: " + player.getRealName() + ", 身份: " + player.getRole() + ", 座位: " + player.getSeatNumber());
        }
        
        List<String> names = players.stream()
            .map(Player::getRealName)
            .collect(Collectors.toList());
        Collections.shuffle(names);

        System.out.println("随机分配后的昵称顺序: " + names);
        
        players.sort((p1, p2) -> Integer.compare(p1.getSeatNumber(), p2.getSeatNumber()));

        System.out.println("按座位号排序后的玩家列表:");
        for (Player player : players) {
            System.out.println("  玩家: " + player.getRealName() + ", 身份: " + player.getRole() + ", 座位: " + player.getSeatNumber());
        }

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            String assignedName = names.get(i);
            player.setAssignedName(assignedName);
            System.out.println("座位号 " + player.getSeatNumber() + " 的玩家 " + player.getRealName() + " (身份: " + player.getRole() + ") 被分配昵称: " + assignedName);
        }
        
        System.out.println("分配昵称后的玩家列表:");
        for (Player player : players) {
            System.out.println("  玩家: " + player.getRealName() + ", 身份: " + player.getRole() + ", 座位: " + player.getSeatNumber() + ", 分配昵称: " + player.getAssignedName());
        }
    }

    private Map<String, List<String>> getRoomPlayersMap(Room room, int roundNumber) {
        Map<String, List<String>> result = new HashMap<>();
        List<RoomSelection> selections = roomSelectionRepository.findByRoomIdAndRoundNumber(room.getId(), roundNumber);
        
        for (RoomSelection selection : selections) {
            String roomType = selection.getRoomType();
            if (!result.containsKey(roomType)) {
                result.put(roomType, new ArrayList<>());
            }
            result.get(roomType).add(selection.getPlayerId());
        }
        
        return result;
    }

    private void checkGameEnd(Room room) {
        Player villageHead = playerRepository.findByRoom(room).stream()
            .filter(p -> p.getRole() == Player.Role.VILLAGE_HEAD)
            .findFirst()
            .orElse(null);

        if (villageHead != null && !villageHead.getIsAlive()) {
            room.setGamePhase(Room.GamePhase.GAME_OVER);
            room.setIsGameOver(true);
            room.setWinner("WOLF");
            roomRepository.save(room);

            Room updatedRoom = roomRepository.findByRoomId(room.getRoomId());
            GameMessage response = new GameMessage();
            response.setType("GAME_OVER");
            Map<String, Object> gameOverData = new HashMap<>();
            gameOverData.put("room", convertRoomToDTO(updatedRoom));
            gameOverData.put("winner", "WOLF");
            gameOverData.put("reason", "村长被杀");
            response.setData(gameOverData);
            response.setTimestamp(System.currentTimeMillis());
            getWebSocketHandler().broadcastToRoom(room.getRoomId(), response);
        }
    }

    private void sendError(WebSocketSession session, String message) {
        GameMessage response = new GameMessage();
        response.setType("ERROR");
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("message", message);
        response.setData(errorData);
        response.setTimestamp(System.currentTimeMillis());
        getWebSocketHandler().sendToSession(session, response);
    }

    public Map<String, Object> convertRoomToDTO(Room room) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", room.getRoomId());
        dto.put("name", room.getName());
        dto.put("hostId", room.getHostId());
        dto.put("maxPlayers", room.getMaxPlayers());
        dto.put("players", convertPlayersToDTO(playerRepository.findByRoom(room)));
        dto.put("gamePhase", room.getGamePhase());
        dto.put("currentRound", room.getCurrentRound());
        dto.put("isGameOver", room.getIsGameOver());
        dto.put("winner", room.getWinner());
        return dto;
    }

    private Map<String, Object> convertPlayerToDTO(Player player) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", player.getPlayerId());
        dto.put("name", player.getName());
        dto.put("realName", player.getRealName());
        dto.put("assignedName", player.getAssignedName());
        dto.put("seatNumber", player.getSeatNumber());
        dto.put("role", player.getRole());
        dto.put("isAlive", player.getIsAlive());
        dto.put("isReady", player.getIsReady());
        dto.put("normalWeapons", player.getNormalWeapons());
        dto.put("fatalWeapon", player.getFatalWeapon());
        dto.put("shields", player.getShields());
        dto.put("lastRoom", player.getLastRoom());
        dto.put("currentRoom", player.getCurrentRoom());
        return dto;
    }

    private List<Map<String, Object>> convertPlayersToDTO(List<Player> players) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Player player : players) {
            result.add(convertPlayerToDTO(player));
        }
        return result;
    }
}
