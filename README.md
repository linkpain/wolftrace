# 狼影迷踪（Wolftrace）

一款基于Web的多人回合制身份推理游戏

## 📋 项目简介

狼影迷踪是一款创新的多人在线推理游戏，玩家通过房间选择、技能使用、公开信息与投票推理敌方身份。游戏支持4-12人同时在线，具有独特的名字打乱分配机制和座位系统。

## 🎮 游戏特色

- **多人在线**: 支持4-12人实时对战
- **身份推理**: 狼王、狼人、村长、村民四种身份
- **名字打乱**: 玩家输入的名字会被打乱分配，增加推理难度
- **座位系统**: 固定座位编号，方便讨论指代
- **实时通信**: 基于WebSocket的实时游戏同步
- **跨平台**: 支持PC和移动端浏览器

## 🏗️ 技术栈

### 后端
- **语言**: Java 1.8
- **框架**: Spring Boot 2.7.18
- **通信**: WebSocket
- **缓存**: Redis
- **构建工具**: Maven
- **包名**: com.linkpin.wolftrace

### 前端
- **框架**: Vue 3
- **语言**: TypeScript
- **构建工具**: Vite
- **UI库**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router

## 📁 项目结构

```
Wolftrace/
├── backend/                    # 后端项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/linkpin/wolftrace/
│   │   │   │       ├── controller/    # 控制器层
│   │   │   │       ├── service/       # 业务逻辑层
│   │   │   │       ├── model/         # 数据模型
│   │   │   │       ├── config/        # 配置类
│   │   │   │       ├── websocket/     # WebSocket处理
│   │   │   │       ├── util/          # 工具类
│   │   │   │       └── dto/           # 数据传输对象
│   │   │   └── resources/             # 配置文件
│   │   └── test/                     # 测试代码
│   └── pom.xml                       # Maven配置
│
└── frontend/                   # 前端项目
    ├── src/
    │   ├── assets/               # 静态资源
    │   │   ├── images/
    │   │   └── styles/
    │   ├── components/           # Vue组件
    │   ├── views/                # 页面视图
    │   ├── router/               # 路由配置
    │   ├── store/                # 状态管理
    │   ├── api/                  # API接口
    │   ├── utils/                # 工具函数
    │   ├── types/                # TypeScript类型
    │   ├── composables/          # 组合式函数
    │   ├── App.vue               # 根组件
    │   └── main.ts               # 入口文件
    ├── public/                   # 公共资源
    ├── index.html               # HTML模板
    ├── package.json             # 依赖配置
    ├── tsconfig.json            # TypeScript配置
    └── vite.config.ts           # Vite配置
```

## 🚀 快速开始

### 环境要求

- **JDK**: 1.8+
- **Maven**: 3.6+
- **Node.js**: 16+
- **Redis**: 5.0+

### 后端启动

1. 安装依赖并构建

```bash
cd backend
mvn clean install
```

2. 启动Redis服务

```bash
# Windows
redis-server

# Linux/Mac
redis-server
```

3. 运行应用

```bash
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动

### 前端启动

1. 安装依赖

```bash
cd frontend
npm install
```

2. 启动开发服务器

```bash
npm run dev
```

前端服务将在 `http://localhost:3000` 启动

## 📖 游戏规则

详见 [Wolftrace.md](./Wolftrace.md)

### 核心玩法

1. **身份分配**: 系统随机分配身份（狼王、狼人、村长、村民）
2. **名字打乱**: 玩家输入的名字会被打乱分配
3. **座位系统**: 固定座位编号，玩家随机入座
4. **房间选择**: 每轮选择进入天/地/人三个房间之一
5. **技能使用**: 使用刀具攻击或护盾防御
6. **夜晚结算**: 结算攻击和护盾效果
7. **讨论投票**: 玩家讨论并投票出局
8. **胜负判定**: 根据条件判断游戏胜负

### 胜利条件

- **狼阵营**: 杀死村长 或 三轮后村民未指认狼王
- **村民阵营**: 成功指认狼王编号

## 🔧 开发指南

### 后端开发

#### WebSocket消息格式

```json
{
  "type": "MESSAGE_TYPE",
  "data": {},
  "timestamp": 1234567890
}
```

#### 添加新的消息类型

1. 在 `dto` 包下创建消息DTO
2. 在 `websocket` 包下添加消息处理器
3. 在 `service` 包下实现业务逻辑

### 前端开发

#### 添加新页面

1. 在 `src/views` 下创建Vue组件
2. 在 `src/router/index.ts` 中配置路由
3. 在 `src/store` 中添加状态管理

#### WebSocket通信

使用 `useWebSocket` composable:

```typescript
import { useWebSocket } from '@/composables/useWebSocket'

const { isConnected, connect, send, onMessage } = useWebSocket()

connect()
onMessage('GAME_START', (data) => {
  console.log('Game started:', data)
})
```

## 📦 部署

### 后端部署

1. 构建JAR包

```bash
cd backend
mvn clean package
```

2. 上传到服务器并运行

```bash
java -jar target/wolftrace-1.0.0.jar
```

### 前端部署

1. 构建生产版本

```bash
cd frontend
npm run build
```

2. 将 `dist` 目录部署到Web服务器

### Nginx配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        root /path/to/frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:8080;
    }

    location /ws {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

## 🤝 贡献指南

欢迎提交Issue和Pull Request！

## 📄 许可证

MIT License

## 👥 联系方式

- 项目地址: https://github.com/yourusername/wolftrace
- 问题反馈: https://github.com/yourusername/wolftrace/issues

---

**包名**: com.linkpin.wolftrace
**版本**: 1.0.0
**最后更新**: 2026-01-19
