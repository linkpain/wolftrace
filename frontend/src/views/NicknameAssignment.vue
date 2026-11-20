<template>
  <div class="nickname-assignment-container">
    <el-card class="nickname-assignment-card">
      <template #header>
        <div class="card-header">
          <span>昵称分配</span>
          <el-tag type="warning">{{ countdown }}秒后自动进入下一阶段</el-tag>
        </div>
      </template>
      
      <div class="assignment-content">
        <div class="assignment-info">
          <el-alert
            title="请记住分配给你的昵称"
            type="warning"
            :closable="false"
            show-icon
          />
        </div>

        <div v-if="gameStore.currentPlayer" class="nickname-display">
          <div class="player-info">
            <div class="info-label">你的身份:</div>
            <el-tag :type="roleType" size="large">{{ roleText }}</el-tag>
          </div>
          
          <div class="nickname-info">
            <div class="info-label">分配给你的昵称:</div>
            <div class="nickname-value">{{ gameStore.currentPlayer.assignedName || gameStore.currentPlayer.realName || '未知' }}</div>
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
          <el-button type="primary" @click="confirmAssignment" :disabled="!gameStore.currentPlayer">
            我已记住，进入下一阶段
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

console.log('NicknameAssignment - gameStore.currentPlayer:', gameStore.currentPlayer)
console.log('NicknameAssignment - gameStore.currentPlayer?.assignedName:', gameStore.currentPlayer?.assignedName)

const countdown = ref(10)
let timer: number | null = null

const roleText = computed(() => {
  const role = gameStore.currentPlayer?.role
  const roleMap: Record<string, string> = {
    'WOLF_KING': '狼王',
    'WOLF': '狼人',
    'VILLAGE_HEAD': '村长',
    'VILLAGER': '村民'
  }
  return roleMap[role || '未知']
})

const roleType = computed(() => {
  const role = gameStore.currentPlayer?.role
  if (role === 'WOLF_KING' || role === 'WOLF') {
    return 'danger'
  } else if (role === 'VILLAGE_HEAD') {
    return 'success'
  }
  return 'info'
})

function confirmAssignment() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  send('NICKNAME_ASSIGNMENT_CONFIRMED', {
    roomId: gameStore.currentRoom?.id,
    playerId: gameStore.currentPlayer?.id
  })
}

onMounted(() => {
  console.log('NicknameAssignment onMounted - gameStore.currentPlayer:', gameStore.currentPlayer)
  console.log('NicknameAssignment onMounted - gameStore.currentPlayer?.assignedName:', gameStore.currentPlayer?.assignedName)
  timer = window.setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      confirmAssignment()
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

<style scoped lang="scss">
.nickname-assignment-container {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.nickname-assignment-card {
  width: 90%;
  max-width: 600px;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 1.2rem;
    font-weight: bold;
  }
}

.assignment-content {
  .assignment-info {
    margin-bottom: 20px;
  }

  .nickname-display {
    display: flex;
    flex-direction: column;
    gap: 20px;
    padding: 20px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    margin-bottom: 20px;

    .player-info,
    .nickname-info {
      display: flex;
      align-items: center;
      gap: 15px;

      .info-label {
        font-size: 1.1rem;
        font-weight: 500;
        color: #303133;
        min-width: 120px;
      }
    }

    .nickname-value {
      font-size: 1.5rem;
      font-weight: bold;
      color: #409eff;
      padding: 10px 20px;
      background: #ecf5ff;
      border-radius: 4px;
      border: 2px solid #409eff;
    }
  }

  .button-group {
    display: flex;
    justify-content: center;
  }
}
</style>
