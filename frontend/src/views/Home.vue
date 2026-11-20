<template>
  <div class="home-container">
    <div class="home-content">
      <div class="game-title">
        <h1>狼影迷踪</h1>
        <p class="subtitle">Wolftrace - 多人身份推理游戏</p>
      </div>

      <div class="menu-buttons">
        <el-button 
          type="primary" 
          size="large" 
          @click="showCreateRoomDialog"
          class="menu-btn"
        >
          创建房间
        </el-button>
        
        <el-button 
          type="success" 
          size="large" 
          @click="showJoinRoomDialog"
          class="menu-btn"
        >
          加入房间
        </el-button>
      </div>

      <div class="game-info">
        <el-card class="info-card">
          <template #header>
            <div class="card-header">
              <span>游戏规则</span>
            </div>
          </template>
          <div class="info-content">
            <p>🎮 支持4-12人在线对战</p>
            <p>🐺 狼王、狼人、村长、村民四种身份</p>
            <p>🎲 名字打乱分配，增加推理难度</p>
            <p>🏠 三个房间选择，不能连续进入同一房间</p>
            <p>⚔️ 使用刀具攻击或护盾防御</p>
          </div>
        </el-card>
      </div>
    </div>

    <el-dialog 
      v-model="createRoomVisible" 
      title="创建房间" 
      width="400px"
    >
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="房间名称">
          <el-input v-model="createForm.roomName" placeholder="请输入房间名称" />
        </el-form-item>
        
        <el-form-item label="玩家昵称">
          <el-input v-model="createForm.playerName" placeholder="请输入你的昵称" />
        </el-form-item>
        
        <el-form-item label="最大人数">
          <el-select v-model="createForm.maxPlayers" placeholder="请选择">
            <el-option
              v-for="num in [4, 5, 6, 7, 8, 9, 10, 11, 12]"
              :key="num"
              :label="num + '人'"
              :value="num"
            />
          </el-select>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="createRoomVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateRoom">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog 
      v-model="joinRoomVisible" 
      title="加入房间" 
      width="400px"
    >
      <el-form :model="joinForm" label-width="80px">
        <el-form-item label="房间ID">
          <el-input v-model="joinForm.roomId" placeholder="请输入房间ID" />
        </el-form-item>
        
        <el-form-item label="玩家昵称">
          <el-input v-model="joinForm.playerName" placeholder="请输入你的昵称" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="joinRoomVisible = false">取消</el-button>
        <el-button type="primary" @click="handleJoinRoom">加入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useGameStore } from '@/store/game'
import { useWebSocket } from '@/composables/useWebSocket'
import { ElMessage } from 'element-plus'

const router = useRouter()
const gameStore = useGameStore()
const { isConnected, connect, send, onMessage } = useWebSocket()

const createRoomVisible = ref(false)
const joinRoomVisible = ref(false)

const createForm = ref({
  roomName: '',
  playerName: '',
  maxPlayers: 8
})

const joinForm = ref({
  roomId: '',
  playerName: ''
})

const showCreateRoomDialog = () => {
  createRoomVisible.value = true
}

const showJoinRoomDialog = () => {
  joinRoomVisible.value = true
}

const handleCreateRoom = () => {
  if (!createForm.value.roomName || !createForm.value.playerName) {
    ElMessage.error('请填写完整信息')
    return
  }

  if (!isConnected.value) {
    console.log('WebSocket未连接，开始连接...')
    connect()
    let attempts = 0
    const maxAttempts = 50
    const checkConnection = setInterval(() => {
      attempts++
      console.log(`检查连接状态 (${attempts}/${maxAttempts}): isConnected=${isConnected.value}`)
      if (isConnected.value) {
        clearInterval(checkConnection)
        console.log('WebSocket已连接，发送CREATE_ROOM消息')
        send('CREATE_ROOM', {
          roomName: createForm.value.roomName,
          playerName: createForm.value.playerName,
          maxPlayers: createForm.value.maxPlayers
        })
      } else if (attempts >= maxAttempts) {
        clearInterval(checkConnection)
        ElMessage.error('连接超时，请重试')
      }
    }, 100)
  } else {
    console.log('WebSocket已连接，直接发送CREATE_ROOM消息')
    send('CREATE_ROOM', {
      roomName: createForm.value.roomName,
      playerName: createForm.value.playerName,
      maxPlayers: createForm.value.maxPlayers
    })
  }

  createRoomVisible.value = false
  ElMessage.success('正在创建房间...')
}

const handleJoinRoom = () => {
  if (!joinForm.value.roomId || !joinForm.value.playerName) {
    ElMessage.error('请填写完整信息')
    return
  }

  if (!isConnected.value) {
    console.log('WebSocket未连接，开始连接...')
    connect()
    let attempts = 0
    const maxAttempts = 50
    const checkConnection = setInterval(() => {
      attempts++
      console.log(`检查连接状态 (${attempts}/${maxAttempts}): isConnected=${isConnected.value}`)
      if (isConnected.value) {
        clearInterval(checkConnection)
        console.log('WebSocket已连接，发送JOIN_ROOM消息')
        send('JOIN_ROOM', {
          roomId: joinForm.value.roomId,
          playerName: joinForm.value.playerName
        })
      } else if (attempts >= maxAttempts) {
        clearInterval(checkConnection)
        ElMessage.error('连接超时，请重试')
      }
    }, 100)
  } else {
    console.log('WebSocket已连接，直接发送JOIN_ROOM消息')
    send('JOIN_ROOM', {
      roomId: joinForm.value.roomId,
      playerName: joinForm.value.playerName
    })
  }

  joinRoomVisible.value = false
  ElMessage.success('正在加入房间...')
}

onMessage('ROOM_CREATED', (data) => {
  console.log('收到 ROOM_CREATED 消息:', data)
  console.log('ROOM_CREATED - 房间hostId:', data.room.hostId)
  console.log('ROOM_CREATED - 房间players:', data.room.players)
  console.log('ROOM_CREATED - 第一个玩家的id:', data.room.players[0].id)
  console.log('ROOM_CREATED - 房间id:', data.roomId)
  console.log('ROOM_CREATED - 房间对象:', data.room)
  gameStore.setRoom(data.room)
  gameStore.setCurrentPlayer(data.room.players[0])
  sessionStorage.setItem('roomId', data.roomId)
  sessionStorage.setItem('playerId', data.room.players[0].id)
  console.log('保存到 sessionStorage - roomId:', data.roomId, 'playerId:', data.room.players[0].id)
  ElMessage.success('房间创建成功')
  router.push('/game')
})

onMessage('PLAYER_JOINED', (data) => {
  gameStore.setRoom(data.room)
  const currentPlayer = data.room.players.find((p: any) => p.name === joinForm.value.playerName)
  if (currentPlayer) {
    gameStore.setCurrentPlayer(currentPlayer)
    sessionStorage.setItem('roomId', data.room.id)
    sessionStorage.setItem('playerId', currentPlayer.id)
  }
  ElMessage.success('成功加入房间')
  router.push('/game')
})

onMessage('ERROR', (data) => {
  ElMessage.error(data.message)
})
</script>

<style scoped lang="scss">
.home-container {
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.home-content {
  text-align: center;
  color: white;
}

.game-title {
  margin-bottom: 60px;

  h1 {
    font-size: 4rem;
    margin-bottom: 10px;
    text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
  }

  .subtitle {
    font-size: 1.5rem;
    opacity: 0.9;
  }
}

.menu-buttons {
  display: flex;
  gap: 20px;
  justify-content: center;
  margin-bottom: 60px;

  .menu-btn {
    min-width: 180px;
    height: 60px;
    font-size: 1.2rem;
    border-radius: 8px;
  }
}

.game-info {
  max-width: 500px;
  margin: 0 auto;
}

.info-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);

  .card-header {
    font-size: 1.2rem;
    font-weight: bold;
    color: #333;
  }

  .info-content {
    p {
      margin: 12px 0;
      font-size: 1rem;
      color: #555;
      line-height: 1.6;
    }
  }
}
</style>
