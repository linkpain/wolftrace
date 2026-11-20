package com.linkpin.wolftrace.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;

@Entity
@Table(name = "players")
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @Column(unique = true, nullable = false)
    private String playerId;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private String realName;

    @Column
    private String assignedName;

    @Column(nullable = false)
    private Integer seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private Boolean isAlive;

    @Column(nullable = false)
    private Boolean isReady;

    @Column(nullable = false)
    private Integer normalWeapons;

    @Column(nullable = false)
    private Integer fatalWeapon;

    @Column(nullable = false)
    private Integer shields;

    @Column(nullable = false)
    private Boolean hasMadeSkillChoice;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = true)
    @JsonIgnore
    private Room room;

    @Column
    private Integer lastRoom;

    @Column
    private Integer currentRoom;

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
    
    public String getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getRealName() {
        return realName;
    }
    
    public void setRealName(String realName) {
        this.realName = realName;
    }
    
    public String getAssignedName() {
        return assignedName;
    }
    
    public void setAssignedName(String assignedName) {
        this.assignedName = assignedName;
    }
    
    public String getName() {
        return assignedName != null ? assignedName : realName;
    }
    
    public Integer getSeatNumber() {
        return seatNumber;
    }
    
    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }
    
    public Role getRole() {
        return role;
    }
    
    public void setRole(Role role) {
        this.role = role;
    }
    
    public Boolean getIsAlive() {
        return isAlive;
    }
    
    public void setIsAlive(Boolean isAlive) {
        this.isAlive = isAlive;
    }
    
    public Boolean getIsReady() {
        return isReady;
    }
    
    public void setIsReady(Boolean isReady) {
        this.isReady = isReady;
    }
    
    public Integer getNormalWeapons() {
        return normalWeapons;
    }
    
    public void setNormalWeapons(Integer normalWeapons) {
        this.normalWeapons = normalWeapons;
    }
    
    public Integer getFatalWeapon() {
        return fatalWeapon;
    }
    
    public void setFatalWeapon(Integer fatalWeapon) {
        this.fatalWeapon = fatalWeapon;
    }
    
    public Integer getShields() {
        return shields;
    }
    
    public void setShields(Integer shields) {
        this.shields = shields;
    }
    
    public Boolean getHasMadeSkillChoice() {
        return hasMadeSkillChoice;
    }
    
    public void setHasMadeSkillChoice(Boolean hasMadeSkillChoice) {
        this.hasMadeSkillChoice = hasMadeSkillChoice;
    }
    
    public Room getRoom() {
        return room;
    }
    
    public void setRoom(Room room) {
        this.room = room;
    }
    
    public Integer getLastRoom() {
        return lastRoom;
    }
    
    public void setLastRoom(Integer lastRoom) {
        this.lastRoom = lastRoom;
    }
    
    public Integer getCurrentRoom() {
        return currentRoom;
    }
    
    public void setCurrentRoom(Integer currentRoom) {
        this.currentRoom = currentRoom;
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

    public enum Role {
        WOLF_KING,
        WOLF,
        VILLAGE_HEAD,
        VILLAGER
    }
}
