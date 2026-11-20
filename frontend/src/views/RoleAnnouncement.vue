<template>
  <div class="role-announcement-container">
    <el-card class="role-announcement-card">
      <template #header>
        <div class="card-header">
          <span>身份公告</span>
          <el-tag type="warning">{{ countdown }}秒后自动进入下一阶段</el-tag>
        </div>
      </template>
      
      <div class="announcement-content">
        <div class="announcement-info">
          <el-alert
            title="请记住所有玩家的身份"
            type="warning"
            :closable="false"
            show-icon
          />
        </div>

        <div class="players-list">
          <div
            v-for="player in sortedPlayers"
            :key="player.id"
            class="player-item"
          >
            <div class="player-info">
              <span class="player-name">{{ player.assignedName || player.realName }}</span>
              <el-tag :type="getRoleType(player.role)" size="large">
                {{ getRoleText(player.role) }}
              </el-tag>
            </div>
          </div>
        </div>

        <div class="button-group">
          <el-button type="primary" @click="confirmAnnouncement">
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

const countdown = ref(5)
let timer: number | null = null

const sortedPlayers = computed(() => {
  if (!gameStore.currentRoom?.players) return []
  return [...gameStore.currentRoom.players].sort((a, b) => a.seatNumber - b.seatNumber)
})

function getRoleType(role: string) {
  switch (role) {
    case 'WOLF_KING':
      return 'danger'
    case 'WOLF':
      return 'danger'
    case 'VILLAGE_HEAD':
      return 'success'
    case 'VILLAGER':
      return 'info'
    default:
      return 'info'
  }
}

function getRoleText(role: string) {
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
}

function confirmAnnouncement() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  send('ROLE_ANNOUNCEMENT_CONFIRMED', {
    roomId: gameStore.currentRoom?.id,
    playerId: gameStore.currentPlayer?.id
  })
}

onMounted(() => {
  timer = window.setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      confirmAnnouncement()
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
.role-announcement-container {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.role-announcement-card {
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

.announcement-content {
  .announcement-info {
    margin-bottom: 20px;
  }

  .players-list {
    max-height: 400px;
    overflow-y: auto;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    padding: 10px;
    margin-bottom: 20px;

    .player-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px;
      border-bottom: 1px solid #ebeef5;

      &:last-child {
        border-bottom: none;
      }

      .player-info {
        display: flex;
        align-items: center;
        gap: 15px;
        flex: 1;

        .player-name {
          font-size: 1.1rem;
          font-weight: 500;
          color: #303133;
        }
      }
    }
  }

  .button-group {
    display: flex;
    justify-content: center;
  }
}
</style>
