package com.linkpin.wolftrace.websocket;

import com.alibaba.fastjson.JSON;
import com.linkpin.wolftrace.dto.GameMessage;
import com.linkpin.wolftrace.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ApplicationContext applicationContext;

    @Autowired
    public GameWebSocketHandler(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    private GameService getGameService() {
        return applicationContext.getBean(GameService.class);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        System.out.println("WebSocket连接建立: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        GameMessage gameMessage = JSON.parseObject(payload, GameMessage.class);
        
        System.out.println("收到消息: " + gameMessage.getType());
        
        Map<String, Object> data = (Map<String, Object>) gameMessage.getData();
        
        switch (gameMessage.getType()) {
            case "JOIN_ROOM":
                getGameService().handleJoinRoom(session, data);
                break;
            case "CREATE_ROOM":
                getGameService().handleCreateRoom(session, data);
                break;
            case "START_GAME":
                getGameService().handleStartGame(session, data);
                break;
            case "READY":
                getGameService().handleReady(session, data);
                break;
            case "ROLE_ANNOUNCEMENT_CONFIRMED":
                getGameService().handleRoleAnnouncementConfirmed(session, data);
                break;
            case "NICKNAME_REVEAL_CONFIRMED":
                getGameService().handleNicknameRevealConfirmed(session, data);
                break;
            case "ROOM_SELECTION":
                getGameService().handleRoomSelection(session, data);
                break;
            case "WEAPON_USE":
                getGameService().handleWeaponUse(session, data);
                break;
            case "SHIELD_USE":
                getGameService().handleShieldUse(session, data);
                break;
            case "SKIP_SKILL":
                getGameService().handleSkipSkill(session, data);
                break;
            case "NIGHT_RESULT_CONFIRMED":
                getGameService().handleNightResultConfirmed(session, data);
                break;
            case "VOTE":
                getGameService().handleVote(session, data);
                break;
            default:
                System.out.println("未知消息类型: " + gameMessage.getType());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        getGameService().handleDisconnect(session);
        System.out.println("WebSocket连接关闭: " + session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket传输错误: " + exception.getMessage());
    }

    public void broadcastToRoom(String roomId, GameMessage message) {
        sessions.values().forEach(session -> {
            if (session.isOpen() && roomId.equals(session.getAttributes().get("roomId"))) {
                try {
                    session.sendMessage(new TextMessage(JSON.toJSONString(message)));
                } catch (IOException e) {
                    System.err.println("发送消息失败: " + e.getMessage());
                }
            }
        });
    }

    public void sendToSession(WebSocketSession session, GameMessage message) {
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(JSON.toJSONString(message)));
            } catch (IOException e) {
                System.err.println("发送消息失败: " + e.getMessage());
            }
        }
    }
}
