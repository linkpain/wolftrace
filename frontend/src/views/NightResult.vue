<template>
  <div class="night-result-container">
    <el-card class="night-result-card">
      <template #header>
        <div class="card-header">
          <span>夜晚公告</span>
          <el-tag type="warning">第 {{ nightResult?.round }} 轮</el-tag>
        </div>
      </template>
      
      <div class="night-result-content">
        <div class="room-players">
          <h3>房间分布</h3>
          <div class="rooms">
            <div v-for="(players, roomType) in nightResult?.roomPlayers" :key="roomType" class="room">
              <div class="room-title">{{ getRoomName(roomType) }}</div>
              <div class="room-players-list">
                <el-tag
                  v-for="playerId in players"
                  :key="playerId"
                  :type="isPlayerDead(playerId) ? 'danger' : 'success'"
                  class="player-tag"
                >
                  {{ getPlayerName(playerId) }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>
        
        <el-divider />
        
        <div class="dead-players">
          <h3>
            <el-icon><Warning /></el-icon>
            死亡名单
          </h3>
          <div v-if="deadPlayersList.length > 0" class="dead-list">
            <el-tag
              v-for="playerId in deadPlayersList"
              :key="playerId"
              type="danger"
              size="large"
              class="dead-tag"
            >
              {{ getPlayerName(playerId) }} ({{ getSeatNumber(playerId) }}号座位)
            </el-tag>
          </div>
          <el-empty v-else description="本轮无人死亡" />
        </div>
        
        <div class="button-group">
          <el-button type="primary" @click="confirmNightResult">
            确认并继续
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Warning } from '@element-plus/icons-vue'
import { useGameStore } from '@/store/game'
import { useWebSocket } from '@/composables/useWebSocket'

const gameStore = useGameStore()
const { send } = useWebSocket()

const nightResult = computed(() => gameStore.nightResult)

const deadPlayersList = computed(() => {
  return nightResult.value?.deadPlayers || []
})

function getRoomName(roomType: string) {
  const roomNames: Record<string, string> = {
    '0': '天',
    '1': '地',
    '2': '人'
  }
  return roomNames[roomType] || roomType
}

function getPlayerName(playerId: string) {
  const player = gameStore.currentRoom?.players.find(p => p.id === playerId)
  return player?.assignedName || playerId
}

function getSeatNumber(playerId: string) {
  const player = gameStore.currentRoom?.players.find(p => p.id === playerId)
  return player?.seatNumber || 0
}

function isPlayerDead(playerId: string) {
  const player = gameStore.currentRoom?.players.find(p => p.id === playerId)
  return !player?.isAlive
}

function confirmNightResult() {
  send('NIGHT_RESULT_CONFIRMED', {})
  gameStore.clearNightResult()
  ElMessage.success('已确认，即将进入讨论阶段')
}
</script>

<style scoped>
.night-result-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.night-result-card {
  width: 800px;
  max-width: 90%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.night-result-content {
  padding: 20px 0;
}

.room-players {
  margin-bottom: 30px;
}

.room-players h3 {
  margin-bottom: 20px;
  color: #303133;
}

.rooms {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.room {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 20px;
}

.room-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 15px;
  text-align: center;
}

.room-players-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.player-tag {
  margin: 0;
}

.dead-players {
  margin-bottom: 30px;
}

.dead-players h3 {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  color: #f56c6c;
}

.dead-list {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
}

.dead-tag {
  font-size: 16px;
  padding: 10px 20px;
}

.button-group {
  display: flex;
  justify-content: center;
}
</style>
