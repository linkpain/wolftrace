<template>
  <el-dialog
    v-model="dialogVisible"
    title="投票阶段"
    width="600px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    @close="handleClose"
  >
    <div class="voting-container">
      <div class="voting-info">
        <el-alert
          title="请选择你要投票的玩家"
          type="info"
          :closable="false"
          show-icon
        />
      </div>

      <div class="player-list">
        <div
          v-for="player in availablePlayers"
          :key="player.id"
          class="player-item"
          :class="{ selected: selectedPlayerId === player.id }"
          @click="selectPlayer(player.id)"
        >
          <div class="player-info">
            <span class="seat-number">{{ player.seatNumber }}号座位</span>
            <span class="player-name">{{ player.assignedName || player.realName }}</span>
            <el-tag v-if="!player.isAlive" type="danger" size="small">已死亡</el-tag>
          </div>
          <el-radio v-model="selectedPlayerId" :label="player.id" />
        </div>
      </div>

      <div class="vote-stats" v-if="voteStats.length > 0">
        <h4>当前投票情况：</h4>
        <div class="stats-list">
          <div v-for="stat in voteStats" :key="stat.playerId" class="stat-item">
            <span>{{ getPlayerName(stat.playerId) }}: {{ stat.votes }}票</span>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button
        type="primary"
        @click="submitVote"
        :disabled="!selectedPlayerId || hasVoted"
      >
        {{ hasVoted ? '已投票' : '确认投票' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useGameStore } from '@/store/game'
import { useWebSocket } from '@/composables/useWebSocket'
import { ElMessage } from 'element-plus'

const gameStore = useGameStore()
const { send, onMessage } = useWebSocket()

const dialogVisible = ref(true)
const selectedPlayerId = ref('')
const hasVoted = ref(false)
const voteStats = ref<Array<{ playerId: string; votes: number }>>([])

const room = computed(() => gameStore.currentRoom)
const currentPlayer = computed(() => gameStore.currentPlayer)

const availablePlayers = computed(() => {
  if (!room.value) return []
  return room.value.players.filter(player => player.isAlive && player.id !== currentPlayer.value?.id)
})

const getPlayerName = (playerId: string) => {
  const player = room.value?.players.find(p => p.id === playerId)
  return player ? `${player.seatNumber}号座位 (${player.assignedName || player.realName})` : '未知'
}

const selectPlayer = (playerId: string) => {
  if (!hasVoted.value) {
    selectedPlayerId.value = playerId
  }
}

const submitVote = () => {
  if (!selectedPlayerId.value) {
    ElMessage.warning('请选择要投票的玩家')
    return
  }

  if (hasVoted.value) {
    ElMessage.warning('你已经投过票了')
    return
  }

  send('VOTE', {
    roomId: room.value?.id,
    playerId: currentPlayer.value?.id,
    targetId: selectedPlayerId.value
  })

  hasVoted.value = true
}

const handleClose = () => {
  dialogVisible.value = false
}

onMessage('VOTE_UPDATE', (data) => {
  voteStats.value = data.voteStats || []
})

onMessage('VOTE_RESULT', (data) => {
  ElMessage.success(`投票结果：${data.eliminatedPlayer ? getPlayerName(data.eliminatedPlayer) + ' 被淘汰' : '无人被淘汰'}`)
  dialogVisible.value = false
})

watch(dialogVisible, (newVal) => {
  if (!newVal) {
    hasVoted.value = false
    selectedPlayerId.value = ''
  }
})
</script>

<style scoped lang="scss">
.voting-container {
  .voting-info {
    margin-bottom: 20px;
  }

  .player-list {
    max-height: 400px;
    overflow-y: auto;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    padding: 10px;

    .player-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px;
      border-bottom: 1px solid #ebeef5;
      cursor: pointer;
      transition: background-color 0.3s;

      &:hover {
        background-color: #f5f7fa;
      }

      &.selected {
        background-color: #ecf5ff;
      }

      &:last-child {
        border-bottom: none;
      }

      .player-info {
        display: flex;
        align-items: center;
        gap: 10px;

        .seat-number {
          font-weight: bold;
          color: #409eff;
        }

        .player-name {
          color: #303133;
        }
      }
    }
  }

  .vote-stats {
    margin-top: 20px;
    padding: 15px;
    background-color: #f5f7fa;
    border-radius: 4px;

    h4 {
      margin: 0 0 10px 0;
      color: #606266;
    }

    .stats-list {
      .stat-item {
        padding: 5px 0;
        color: #303133;
      }
    }
  }
}
</style>
