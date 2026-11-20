<template>
  <div class="room-selection-container">
    <el-card class="room-selection-card">
      <template #header>
        <div class="card-header">
          <span>房间选择</span>
          <el-tag type="info">第 {{ currentRound }} 轮</el-tag>
        </div>
      </template>
      
      <div class="room-selection-content">
        <div class="info">
          <el-alert
            title="选择规则"
            type="info"
            :closable="false"
            show-icon
          >
            <template #default>
              <p>1. 每轮选择一个房间进入</p>
              <p>2. 不能连续进入同一房间</p>
              <p>3. 第一轮无限制</p>
            </template>
          </el-alert>
        </div>
        
        <div class="rooms">
          <div
            v-for="room in rooms"
            :key="room.type"
            class="room-card"
            :class="{ 'disabled': room.disabled, 'selected': selectedRoom === room.type }"
            @click="selectRoom(room)"
          >
            <div class="room-icon">{{ room.icon }}</div>
            <div class="room-name">{{ room.name }}</div>
            <div class="room-desc">{{ room.desc }}</div>
            <el-tag v-if="room.disabled" type="warning">不可选</el-tag>
            <el-tag v-else-if="selectedRoom === room.type" type="success">已选择</el-tag>
          </div>
        </div>
        
        <div class="button-group">
          <el-button
            type="primary"
            @click="confirmSelection"
            :disabled="!selectedRoom"
          >
            确认选择
          </el-button>
        </div>
        
        <div class="player-status">
          <h4>玩家选择状态</h4>
          <div class="players">
            <div
              v-for="player in players"
              :key="player.id"
              class="player-item"
            >
              <el-tag :type="player.currentRoom !== -1 ? 'success' : 'info'">
                {{ player.seatNumber }}号座位
              </el-tag>
              <span>{{ player.currentRoom !== -1 ? '已选择' : '未选择' }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useGameStore } from '@/store/game'
import { useWebSocket } from '@/composables/useWebSocket'
import { RoomType } from '@/types/game'

const gameStore = useGameStore()
const { send, onMessage } = useWebSocket()

const selectedRoom = ref<string | null>(null)

const currentRound = computed(() => gameStore.currentRound)
const currentPlayer = computed(() => gameStore.currentPlayer)
const players = computed(() => gameStore.currentRoom?.players || [])

const rooms = computed(() => {
  const lastRoom = currentPlayer.value?.lastRoom || -1
  return [
    {
      type: RoomType.HEAVEN,
      name: '天',
      icon: '☀️',
      desc: '天空之境',
      disabled: lastRoom === parseInt(RoomType.HEAVEN)
    },
    {
      type: RoomType.EARTH,
      name: '地',
      icon: '🌍',
      desc: '大地之母',
      disabled: lastRoom === parseInt(RoomType.EARTH)
    },
    {
      type: RoomType.HUMAN,
      name: '人',
      icon: '👥',
      desc: '人间烟火',
      disabled: lastRoom === parseInt(RoomType.HUMAN)
    }
  ]
})

function selectRoom(room: any) {
  if (room.disabled) {
    ElMessage.warning('不能连续进入同一房间')
    return
  }
  selectedRoom.value = room.type
}

function confirmSelection() {
  if (!selectedRoom.value) {
    ElMessage.warning('请选择一个房间')
    return
  }

  send('ROOM_SELECTION', {
    roomId: gameStore.currentRoom?.id,
    playerId: gameStore.currentPlayer?.id,
    roomType: selectedRoom.value
  })

  ElMessage.success('房间选择已提交')
  selectedRoom.value = null
}

onMounted(() => {
  onMessage('ROOM_SELECTED', (data) => {
    console.log('收到ROOM_SELECTED消息:', data)
    gameStore.setRoom(data.room)
    const savedPlayerId = sessionStorage.getItem('playerId')
    if (savedPlayerId) {
      const player = data.room.players?.find((p: any) => p.id === savedPlayerId)
      if (player) {
        gameStore.setCurrentPlayer(player)
      }
    }
  })
})

onUnmounted(() => {
  // 清理消息监听
})
</script>

<style scoped>
.room-selection-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.room-selection-card {
  width: 800px;
  max-width: 90%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.room-selection-content {
  padding: 20px 0;
}

.info {
  margin-bottom: 30px;
}

.rooms {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.room-card {
  background: #f5f7fa;
  border: 2px solid #dcdfe6;
  border-radius: 8px;
  padding: 30px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.room-card:hover:not(.disabled) {
  border-color: #409eff;
  background: #ecf5ff;
  transform: translateY(-2px);
}

.room-card.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.room-card.selected {
  border-color: #67c23a;
  background: #f0f9ff;
}

.room-icon {
  font-size: 48px;
  margin-bottom: 15px;
}

.room-name {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 10px;
}

.room-desc {
  color: #909399;
  margin-bottom: 15px;
}

.button-group {
  display: flex;
  justify-content: center;
  margin-bottom: 30px;
}

.player-status {
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.player-status h4 {
  margin-bottom: 15px;
  color: #303133;
}

.players {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 10px;
}

.player-item {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>
