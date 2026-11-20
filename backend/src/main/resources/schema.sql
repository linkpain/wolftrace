CREATE DATABASE IF NOT EXISTS wolftrace CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE wolftrace;

CREATE TABLE IF NOT EXISTS rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    host_id VARCHAR(255),
    max_players INT NOT NULL,
    game_phase VARCHAR(50) NOT NULL DEFAULT 'WAITING',
    current_round INT DEFAULT 0,
    is_game_over BIT(1) DEFAULT 0,
    winner VARCHAR(50),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    INDEX idx_room_id (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS players (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id VARCHAR(255) NOT NULL UNIQUE,
    session_id VARCHAR(255) NOT NULL,
    real_name VARCHAR(255) NOT NULL,
    assigned_name VARCHAR(255),
    seat_number INT NOT NULL,
    role VARCHAR(50) NOT NULL,
    is_alive TINYINT(1) NOT NULL DEFAULT 1,
    is_ready BIT(1) NOT NULL DEFAULT 0,
    normal_weapons INT NOT NULL DEFAULT 0,
    fatal_weapon INT NOT NULL DEFAULT 0,
    shields INT NOT NULL DEFAULT 0,
    has_made_skill_choice BIT(1) NOT NULL DEFAULT 0,
    room_id BIGINT,
    last_room INT DEFAULT -1,
    current_room INT DEFAULT -1,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    INDEX idx_player_id (player_id),
    INDEX idx_session_id (session_id),
    INDEX idx_room_id (room_id),
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS room_selections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    player_id VARCHAR(255) NOT NULL,
    round_number INT NOT NULL,
    room_type VARCHAR(50) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    INDEX idx_room_id (room_id),
    INDEX idx_player_id (player_id),
    INDEX idx_round_number (round_number),
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS weapon_uses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    attacker_id VARCHAR(255) NOT NULL,
    target_id VARCHAR(255) NOT NULL,
    weapon_type VARCHAR(50) NOT NULL,
    round_number INT NOT NULL,
    is_successful BIT(1) NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    INDEX idx_room_id (room_id),
    INDEX idx_attacker_id (attacker_id),
    INDEX idx_target_id (target_id),
    INDEX idx_round_number (round_number),
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS shield_uses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    target_id VARCHAR(255) NOT NULL,
    round_number INT NOT NULL,
    is_successful BIT(1) NOT NULL DEFAULT 0,
    is_effective BIT(1) NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    INDEX idx_room_id (room_id),
    INDEX idx_user_id (user_id),
    INDEX idx_target_id (target_id),
    INDEX idx_round_number (round_number),
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    sender_id VARCHAR(255) NOT NULL,
    sender_name VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    round_number INT NOT NULL,
    created_at BIGINT NOT NULL,
    INDEX idx_room_id (room_id),
    INDEX idx_sender_id (sender_id),
    INDEX idx_round_number (round_number),
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS votes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    voter_id VARCHAR(255) NOT NULL,
    target_id VARCHAR(255) NOT NULL,
    round_number INT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    INDEX idx_room_id (room_id),
    INDEX idx_voter_id (voter_id),
    INDEX idx_round_number (round_number),
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
