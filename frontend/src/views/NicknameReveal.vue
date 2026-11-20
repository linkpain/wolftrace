<template>
  <div class="nickname-reveal-container">
    <el-card class="nickname-reveal-card">
      <template #header>
        <div class="card-header">
          <span>昵称分配</span>
          <el-tag type="warning">{{ countdown }}秒后公示身份</el-tag>
        </div>
      </template>
      
      <div class="reveal-content">
        <div class="reveal-info">
          <el-alert
            title="请记住你获得的昵称"
            type="warning"
            :closable="false"
            show-icon
          />
        </div>

        <div v-if="gameStore.currentPlayer" class="nickname-display">
          <div class="nickname-info">
            <div class="info-label">座位号:</div>
            <div class="seat-number">{{ gameStore.currentPlayer.seatNumber }}</div>
          </div>
          
          <div class="nickname-info">
            <div class="info-label">你获得的昵称:</div>
            <div class="nickname-value">{{ gameStore.currentPlayer.assignedName || '未知' }}</div>
          </div>
        </div>

        <div v-else class="loading-message">
          <el-alert
            title="正在加载玩家信息..."
            type="info"
            :closable="false"
            show-icon
          />
        </div>

        <div class="button-group">
          <el-button type="primary" @click="confirmReveal" :disabled="!gameStore.currentPlayer">
            我已记住，公示身份
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useGameStore } from '@/store/game'
import { useWebSocket } from '@/composables/useWebSocket'

const gameStore = useGameStore()
const { send } = useWebSocket()

const countdown = ref(5)
let timer: number | null = null

const roleType = computed(() => {
  const role = gameStore.currentPlayer?.role
  switch (role) {
    case 'WOLF_KING':
      return 'danger'
    case 'WOLF':
      return 'warning'
    case 'VILLAGE_HEAD':
      return 'success'
    case 'VILLAGER':
      return 'info'
    default:
      return 'info'
  }
})

const roleText = computed(() => {
  const role = gameStore.currentPlayer?.role
  switch (role) {
    case 'WOLF_KING':
      return '狼王'
    case 'WOLF':
      return '狼人'
    case 'VILLAGE_HEAD':
      return '村长'
    case 'VILLAGER':
      return '村民'
    default:
      return '未知'
  }
})

const confirmReveal = () => {
  console.log('confirmReveal被调用')
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  
  console.log('发送NICKNAME_REVEAL_CONFIRMED消息', {
    roomId: gameStore.currentRoom?.id,
    playerId: gameStore.currentPlayer?.id
  })
  
  send('NICKNAME_REVEAL_CONFIRMED', {
    roomId: gameStore.currentRoom?.id,
    playerId: gameStore.currentPlayer?.id
  })
}

onMounted(() => {
  timer = window.setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      confirmReveal()
    }
  }, 1000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})
</script>

<style scoped>
.nickname-reveal-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.nickname-reveal-card {
  width: 100%;
  max-width: 500px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: bold;
}

.reveal-content {
  padding: 20px 0;
}

.reveal-info {
  margin-bottom: 20px;
}

.nickname-display {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 20px;
}

.nickname-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: #f5f7fa;
  padding: 20px;
  border-radius: 8px;
}

.info-label {
  font-size: 14px;
  color: #909399;
  font-weight: bold;
}

.seat-number {
  font-size: 32px;
  font-weight: bold;
  color: #409EFF;
  text-align: center;
}

.nickname-value {
  font-size: 28px;
  font-weight: bold;
  color: #E6A23C;
  text-align: center;
}

.loading-message {
  margin-bottom: 20px;
}

.button-group {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 20px;
}

.button-group .el-button {
  min-width: 200px;
}
</style>
