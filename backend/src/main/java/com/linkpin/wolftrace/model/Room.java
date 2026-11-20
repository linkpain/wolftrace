package com.linkpin.wolftrace.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @Column(unique = true, nullable = false)
    private String roomId;

    @Column(nullable = false)
    private String name;

    @Column
    private String hostId;

    @Column(nullable = false)
    private Integer maxPlayers;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Player> players;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<RoomSelection> roomSelections;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<WeaponUse> weaponUses;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ShieldUse> shieldUses;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Message> messages;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Vote> votes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GamePhase gamePhase;

    @Column
    private Integer currentRound;

    @Column
    private Boolean isGameOver;

    @Column
    private String winner;

    @Column(nullable = false)
    private Long createdAt;

    @Column(nullable = false)
    private Long updatedAt;
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getRoomId() {
        return roomId;
    }
    
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getHostId() {
        return hostId;
    }
    
    public void setHostId(String hostId) {
        this.hostId = hostId;
    }
    
    public Integer getMaxPlayers() {
        return maxPlayers;
    }
    
    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
    
    public List<Player> getPlayers() {
        return players;
    }
    
    public void setPlayers(List<Player> players) {
        this.players = players;
    }
    
    public List<RoomSelection> getRoomSelections() {
        return roomSelections;
    }
    
    public void setRoomSelections(List<RoomSelection> roomSelections) {
        this.roomSelections = roomSelections;
    }
    
    public List<WeaponUse> getWeaponUses() {
        return weaponUses;
    }
    
    public void setWeaponUses(List<WeaponUse> weaponUses) {
        this.weaponUses = weaponUses;
    }
    
    public List<ShieldUse> getShieldUses() {
        return shieldUses;
    }
    
    public void setShieldUses(List<ShieldUse> shieldUses) {
        this.shieldUses = shieldUses;
    }
    
    public List<Message> getMessages() {
        return messages;
    }
    
    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
    
    public List<Vote> getVotes() {
        return votes;
    }
    
    public void setVotes(List<Vote> votes) {
        this.votes = votes;
    }
    
    public GamePhase getGamePhase() {
        return gamePhase;
    }
    
    public void setGamePhase(GamePhase gamePhase) {
        this.gamePhase = gamePhase;
    }
    
    public Integer getCurrentRound() {
        return currentRound;
    }
    
    public void setCurrentRound(Integer currentRound) {
        this.currentRound = currentRound;
    }
    
    public Boolean getIsGameOver() {
        return isGameOver;
    }
    
    public void setIsGameOver(Boolean isGameOver) {
        this.isGameOver = isGameOver;
    }
    
    public String getWinner() {
        return winner;
    }
    
    public void setWinner(String winner) {
        this.winner = winner;
    }
    
    public Long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
    
    public Long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = System.currentTimeMillis();
        updatedAt = System.currentTimeMillis();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = System.currentTimeMillis();
    }

    public enum GamePhase {
        WAITING,
        NICKNAME_REVEAL,
        ROLE_ANNOUNCEMENT,
        ROOM_SELECTION,
        SKILL_SELECTION,
        NIGHT_RESULT,
        DISCUSSION,
        VOTING,
        GAME_OVER
    }
}
