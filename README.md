狼影迷踪 · Wolftrace

<div align="center">

# 狼影迷踪 · Wolftrace

> 一款基于 Web 的多人回合制身份推理游戏 —— 选房间、用技能、辨身份，在三轮博弈中找出狼王。

**关键词**：`多人在线` `身份推理` `WebSocket 实时同步` `Spring Boot + Vue3` `狼人杀变体` `名字打乱` `座位系统`

![Java](https://img.shields.io/badge/Java-1.8-007396?logo=openjdk) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-6DB33F?logo=springboot) ![Vue](https://img.shields.io/badge/Vue-3.4-4FC08D?logo=vuedotjs) ![TypeScript](https://img.shields.io/badge/TypeScript-5.3.3-3178C6?logo=typescript) ![WebSocket](https://img.shields.io/badge/Real--time-WebSocket-blue) ![License](https://img.shields.io/badge/License-MIT-yellow.svg)

作者：[linkpain](https://github.com/linkpain) · 仓库：[linkpain/wolftrace](https://github.com/linkpain/wolftrace) · 来源：[Gitee 原仓库](https://gitee.com/li-tianyiTY/wolftrace)

</div>

> **狼影迷踪**（工程名 `wolftrace`，后端包名 `com.linkpin.wolftrace`）是一款 Web 多人回合制身份推理游戏。4–12 名玩家在线进入同一房间，被随机分配为 **狼王 / 狼人 / 村长 / 村民** 四种身份；玩家输入的名字会被打乱重新分配，配合固定座位编号，让「你是谁」成为推理的一部分。每轮通过「选房间 → 用技能 → 夜晚结算 → 讨论投票」推进，直到狼方杀掉村长、或三轮过后村民仍未指认出狼王、或村民成功投票出狼王为止。项目采用 Spring Boot + Vue 3 前后端分离架构，依靠 WebSocket 做实时状态同步。

> 本项目迁移自 Gitee 的 `li-tianyiTY/wolftrace`（MIT 许可，原始提交 HEAD `66550da6`），代码内容与原仓库一致。

---

## 📖 目录

- [🎯 项目简介](#-项目简介)
- [✨ 核心特性](#-核心特性)
- [🚀 快速开始](#-快速开始)
- [📋 构建环境](#-构建环境)
- [📁 项目结构](#-项目结构)
- [🏗 架构概览](#-架构概览)
- [🎮 游戏玩法与规则](#-游戏玩法与规则)
- [⚙️ 配置说明](#️-配置说明)
- [📄 许可](#-许可)
- [🙏 致谢](#-致谢)

---

## 🎯 项目简介

狼影迷踪把经典「狼人杀」改造成一套**可按轮次推进、强依赖身份与房间博弈**的 Web 游戏。它的核心设计有三点：

- **身份隐藏 + 名字打乱**：玩家输入昵称后，系统会把名字打乱再分配（`NicknameAssignment` / `NicknameReveal` 两个阶段，需玩家确认后才揭示），并给每位玩家固定座位编号，讨论时用「几号」指代，而非真实昵称，显著增加推理难度。
- **空间博弈**：每轮玩家在「天 / 地 / 人」三个房间中选择其一进入（`RoomType.HEAVEN` / `EARTH` / `HUMAN`），且**不能连续进入同一个房间**。狼人能否刀到村长、护盾是否能保护队友，都取决于双方是否在同一房间。
- **技能与投票**：狼方使用刀具（普通刀任意轮可用、必杀刀仅第 2/3 轮可用），好人方使用护盾防御，各自技能全部用完后进入夜晚结算与公开讨论，最后由全体玩家投票票出疑似狼王。

后端用 JPA 把房间、玩家、选房、用刀、护盾、发言、投票等状态落到 MySQL，并用 Redis 做缓存；前端用 Pinia 管理游戏状态，用一个统一的 WebSocket 通道收发 `GameMessage`，实现多端实时同步。

---

## ✨ 核心特性

| 环节 | 实现要点 | 涉及类 / 文件 |
| --- | --- | --- |
| 🟢 房间与身份 | 支持 4–12 人；创建/加入房间、准备、开始游戏；四种身份 `WOLF_KING` / `WOLF` / `VILLAGE_HEAD` / `VILLAGER` | `GameController`、`GameWebSocketHandler.handleCreateRoom/JOIN_ROOM`、`Player.Role`、`Room` |
| 🔵 名字打乱 | 输入昵称后打乱分配并揭示，玩家确认后进入身份分配；增加「认人」推理难度 | `NicknameAssignment.vue`、`NicknameReveal.vue`、`NICKNAME_REVEAL_CONFIRMED` 消息 |
| 🟡 座位系统 | 每位玩家分配固定座位编号，讨论与投票用编号指代 | `model/Player`、`model/RoomPlayer`、`convertRoomToDTO()` |
| 🟠 房间选择 | 天/地/人三房间；校验「不能连续进入同一房间」（依据 `lastRoom`）；全员选完自动进入技能阶段 | `handleRoomSelection()`、`RoomSelection.vue`、`RoomType` 枚举 |
| 🔴 技能系统 | 狼方普通刀（任意轮）/ 必杀刀（仅第 2、3 轮）；好人方护盾（仅同房间有效）；可跳过技能 | `handleWeaponUse()`、`handleShieldUse()`、`handleSkipSkill()`、`SkillSelection.vue` |
| 🟣 夜晚结算 | 狼刀命中同房间的村长，村长有护盾则消耗护盾抵消，否则出局；技能全用完自动推进阶段 | `checkAllSkillsUsed()`、`NightResult.vue`、`WeaponUse` / `ShieldUse` 实体 |
| 🟤 讨论与投票 | 公开讨论后全员投票，结合 `RoomVote` / `Vote` 模型统计票型，票出疑似狼王 | `Voting.vue`、`handleVote()`、`GamePhase.DISCUSSION / VOTING` |
| ⚪ 实时同步 | 基于 `/ws` 的 WebSocket 全双工通信；断线指数退避重连最多 5 次；附带 REST 健康检查与房间查询 | `GameWebSocketHandler`、`useWebSocket.ts`、`GameController`(`/rooms`、`/health`) |

补充：整套流程由一个明确的**阶段状态机**驱动，前端 `GamePhase` 共 9 个状态：`WAITING → NICKNAME_REVEAL → ROLE_ANNOUNCEMENT → ROOM_SELECTION → SKILL_SELECTION → NIGHT_RESULT → DISCUSSION → VOTING → GAME_OVER`，后端 `Room.gamePhase` 与之对应，所有阶段切换都通过 WebSocket 广播下发。

---

## 🚀 快速开始

### 1. 环境要求

- **JDK**：1.8 及以上
- **Maven**：3.6 及以上
- **Node.js**：16 及以上（含 npm）
- **MySQL**：5.7 / 8.x（需创建 `wolftrace` 数据库）
- **Redis**：5.0 及以上

### 2. 准备数据库

```sql
CREATE DATABASE IF NOT EXISTS wolftrace DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> 表结构由 `backend/src/main/resources/schema.sql` 提供（rooms / players / room_selections / weapon_uses / shield_uses / messages / votes），JPA 同时配置了 `ddl-auto=update` 自动同步。

### 3. 启动后端

```bash
cd backend
# 先按需修改 application.yml 中的数据库/Redis 连接（见「配置说明」）
mvn clean package -DskipTests
java -jar target/*.jar
# 或者： mvn spring-boot:run
```

后端默认监听 `http://localhost:8080`，可用 `GET /health` 验证（返回 `{"status":"ok"}`）。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

浏览器打开 `http://localhost:3000`。Vite 已配置代理：`/api` → `http://localhost:8080`，`/ws` → `ws://localhost:8080`（WebSocket）。如需连接非本地后端，可设置环境变量 `VITE_WS_URL`。

> 两个终端分别运行后端与前端；进入首页后创建房间（填房间名、昵称、人数 4–12），另一浏览器/设备用房间号加入即可开局。

---

## 📋 构建环境

### 后端（`backend/pom.xml`）

| 项目 | 版本 / 配置 |
| --- | --- |
| 语言 / 框架 | Java 1.8、Spring Boot 2.7.18 |
| Web / 通信 | `spring-boot-starter-web`、`spring-boot-starter-websocket` |
| 持久层 | `spring-boot-starter-data-jpa`、MySQL Connector 8.0.33 |
| 缓存 | `spring-boot-starter-data-redis` |
| 工具库 | Fastjson 1.2.83、Lombok、Guava 31.1-jre、Commons Lang3、Logback |
| 打包 | Maven，产出可执行 JAR |
| 包名 | `com.linkpin.wolftrace` |

### 前端（`frontend/package.json`）

| 项目 | 版本 / 配置 |
| --- | --- |
| 框架 / 语言 | Vue 3.4、TypeScript 5.3.3 |
| 路由 / 状态 | Vue Router 4.2.5、Pinia 2.1.7 |
| HTTP / UI | Axios 1.6.2、Element Plus 2.4.4 |
| 构建 / 样式 | Vite 5.0.8、Sass 1.69.5、@vitejs/plugin-vue |
| 代码规范 | ESLint |
| 脚本 | `dev` / `build`（`vue-tsc && vite build`）/ `preview` / `lint` |

| 端口 | 说明 |
| --- | --- |
| 8080 | 后端 Spring Boot 服务（`server.port`） |
| 3000 | 前端 Vite 开发服务器 |
| /ws | WebSocket 端点（后端） |

---

## 📁 项目结构

```text
wolftrace/
├── backend/                                   # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/linkpin/wolftrace/
│       │   ├── WolftraceApplication.java      # 启动类
│       │   ├── config/
│       │   │   ├── CorsConfig.java            # 跨域配置
│       │   │   └── WebSocketConfig.java       # /ws 端点注册
│       │   ├── controller/
│       │   │   └── GameController.java        # REST：/rooms、/rooms/{id}、/health
│       │   ├── dto/GameMessage.java           # 消息体 { type, data, timestamp }
│       │   ├── model/                         # JPA 实体
│       │   │   ├── Message.java  Player.java  Room.java  RoomPlayer.java
│       │   │   └── RoomSelection.java  ShieldUse.java  Vote.java  WeaponUse.java  RoomVote.java
│       │   ├── repository/                    # 7 个 Spring Data JPA Repository
│       │   ├── service/GameService.java       # 核心业务（约 42KB）：阶段流转与规则结算
│       │   └── websocket/GameWebSocketHandler.java # 消息分发与房间广播
│       │   └── resources/
│       │       ├── application.yml            # 服务/数据库/Redis/游戏参数配置
│       │       └── schema.sql                 # 7 张业务表 DDL（utf8mb4）
└── frontend/                                  # Vue 3 + Vite 前端
    ├── package.json  index.html  vite.config.ts  tsconfig*.json
    └── src/
        ├── main.ts  App.vue                   # 入口与根组件
        ├── router/index.ts                    # 路由：/（Home）、/game（Game）
        ├── store/game.ts                      # Pinia 游戏状态
        ├── types/game.ts                      # Role / Weapon / GamePhase / RoomType 枚举
        ├── composables/useWebSocket.ts        # WS 连接、重连、send/onMessage
        ├── assets/styles/main.scss            # 全局样式
        ├── components/                        # RoomSelection / SkillSelection / Voting
        └── views/                             # Home / NameInput / NicknameAssignment /
                                               # NicknameReveal / RoleAnnouncement / RoomSelection /
                                               # SkillSelection / NightResult / Voting / Game
```

---

## 🏗 架构概览

```text
浏览器(Web/移动端)
     │  HTTP  /api          WebSocket  /ws (GameMessage: type/data/timestamp)
     ▼                            ▼
┌──────────────────────────────────────────────┐
│ 前端 Vue 3                                    │
│  views(阶段页面) → components → store(Pinia)   │
│         ↑ onMessage        ↓ send(type,data)   │
│  composables/useWebSocket.ts（指数退避重连5次）│
└──────────────────────────────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────────────┐
│ 后端 Spring Boot                              │
│  GameController(REST)  GameWebSocketHandler    │
│          ↓ 按 type switch 分发 12 类消息        │
│  GameService（规则结算 / 阶段推进 / 广播）       │
│          ↓                                    │
│  Repository(JPA) ──► MySQL   Redis(缓存)        │
└──────────────────────────────────────────────┘
```

- **消息协议**：前后端统一用 `GameMessage { type, data, timestamp }`。WebSocket 处理器按 `type` 分发 12 类客户端消息：`JOIN_ROOM`、`CREATE_ROOM`、`START_GAME`、`READY`、`ROLE_ANNOUNCEMENT_CONFIRMED`、`NICKNAME_REVEAL_CONFIRMED`、`ROOM_SELECTION`、`WEAPON_USE`、`SHIELD_USE`、`SKIP_SKILL`、`NIGHT_RESULT_CONFIRMED`、`VOTE`；服务端处理后通过 `broadcastToRoom()` 按 session 上的 `roomId` 属性向全房间广播结果（如 `SKILL_SELECTION_PHASE`、`WEAPON_USED`、`SHIELD_USED`、`ROOM_SELECTED`）。
- **后端分层**：`controller`（对外 REST）与 `websocket`（实时入口）→ `service.GameService`（构造注入各 Repository，集中实现业务规则与阶段推进）→ `repository`（Spring Data JPA）→ `model`（实体）。跨域由 `CorsConfig` 统一放行。
- **前端状态**：`store/game.ts`（Pinia）保存房间、玩家、身份、阶段等全局状态；`types/game.ts` 定义强类型枚举；各 `views` 对应阶段页面，按当前 `GamePhase` 切换展示；`useWebSocket` 封装连接、5 次指数退避重连（`min(1000 * 2^n, 10000)` ms）与基于 `type` 的处理器注册。
- **持久化**：MySQL 存 7 张业务表（`schema.sql` + JPA `ddl-auto=update`），Redis 作为缓存；日志输出到 `logs/wolftrace.log`。

---

## 🎮 游戏玩法与规则

### 完整流程

1. **创建 / 加入房间**：玩家创建房间（房间名、昵称、人数 4–12）或用房间号加入，全员点击准备后开始。
2. **名字打乱与揭示**：系统打乱所有昵称再分配，逐一揭示并要求确认，让玩家无法凭昵称直接对号入座。
3. **座位与身份分配**：分配固定座位编号与身份（狼王、狼人、村长、村民），玩家确认后进入首轮。
4. **房间选择**：每轮选择进入天 / 地 / 人三个房间之一，不能连续进入同一房间；全员选完自动进入技能阶段。
5. **技能使用**：狼方用刀具攻击，好人方用护盾防御，或选择跳过；全员完成后自动结算。
6. **夜晚结算**：结算本轮攻击与护盾效果，公布出局与存活情况。
7. **讨论与投票**：公开讨论后全员投票，统计票型票出疑似狼王。
8. **胜负判定**：进入下一轮或结束游戏。

### 胜利条件

- **狼阵营胜**：狼人成功杀死村长，**或** 三轮过后村民仍未指认出狼王。
- **村民阵营胜**：在投票中成功指认（票出）狼王编号。

### 技能规则（依据 `GameService`）

- **普通刀**：任意轮可用；**必杀刀**：仅第 2、3 轮可用，用完即止。
- 只有狼人 / 狼王能使用武器；攻击目标为**与自己在同一房间的村长**：若村长仍有护盾则消耗一枚护盾、攻击失败，否则村长出局。
- **护盾**：仅当使用者与目标在同一房间时才生效，使用后消耗一枚。
- 所有存活玩家的技能决策完成后（`checkAllSkillsUsed`），阶段自动推进，无需等待超时。

> 游戏参数（`game.min-players=4`、`game.max-players=12`、`game.max-rounds=3`、`game.room-timeout=300000`）均可在后端配置中调整。

---

## ⚙️ 配置说明

后端配置位于 `backend/src/main/resources/application.yml`，关键项如下：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `server.port` | `8080` | 后端 HTTP 端口 |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/wolftrace` | MySQL 连接串 |
| `spring.jpa.hibernate.ddl-auto` | `update` | 自动同步表结构 |
| `spring.redis.host` / `port` / `database` | `localhost` / `6379` / `0` | Redis 连接 |
| `websocket.endpoint` | `/ws` | WebSocket 端点 |
| `websocket.allowed-origins` | `*` | 允许的来源（生产环境应收紧） |
| `game.min-players` / `max-players` | `4` / `12` | 房间人数范围 |
| `game.max-rounds` | `3` | 最大轮次 |
| `game.room-timeout` | `300000` | 房间超时（毫秒） |
| `logging.file.name` | `logs/wolftrace.log` | 日志文件 |

> 🔒 **安全提示**：原仓库配置中存在明文数据库密码。部署到任何非本地环境前，请务必把数据库账号密码改为**环境变量注入**（例如用 `${WOLFTRACE_DB_USER}` / `${WOLFTRACE_DB_PASSWORD}` 占位并在启动时传入），不要将真实凭据写进代码或配置文件；同时把 `websocket.allowed-origins` 从 `*` 收紧到实际域名。

---

## 📄 许可

本项目沿用原仓库的 **MIT License**（见根目录 `LICENSE`），可以自由使用、复制、修改、合并、发布、分发、再许可和/或出售副本，但须保留版权声明与许可声明。详见 [LICENSE](./LICENSE)。

> 说明：本项目由 Gitee 的 `li-tianyiTY/wolftrace` 迁移而来，MIT 许可随源码一并保留。

---

## 🙏 致谢

- 感谢原作者 [li-tianyiTY](https://gitee.com/li-tianyiTY) 开源狼影迷踪，并沉淀出「名字打乱 + 房间博弈 + 技能结算」这套有辨识度的玩法设计。
- 感谢 Spring Boot、Vue 3、Element Plus、Pinia、WebSocket 等开源生态提供的坚实基础。
- 感谢每一位参与内测、贡献规则建议的玩家——正是你们的反馈让身份推理的每一轮都更耐玩。
