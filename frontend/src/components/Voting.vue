<template>
  <el-dialog 
    v-model="visible" 
    title="投票" 
    width="500px"
    @close="handleClose"
  >
    <div class="voting">
      <p class="tip">请选择要投票出局的玩家</p>
      
      <div class="players">
        <div 
          v-for="player in votablePlayers" 
          :key="player.id"
          class="player-card"
          :class="{ selected: selectedTarget === player.id }"
          @click="selectTarget(player.id)"
        >
          <div class="player-info">
            <span class="seat">{{ player.seatNumber }}号</span>
            <span class="name">{{ player.name }}</span>
            <span class="status">{{ player.isAlive ? '存活' : '出局' }}</span>
          </div>
        </div>
      </div>

      <div class="vote-info">
        <el-progress 
          :percentage="voteProgress" 
          :status="voteStatus"
        />
        <p class="vote-count">已投票: {{ votedCount }} / {{ totalVotes }}</p>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button 
        type="primary" 
        @click="handleConfirm" 
        :disabled="!selectedTarget"
      >
        确认投票
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useGameStore } from '@/store/game'
import { useWebSocket } from '@/composables/useWebSocket'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const gameStore = useGameStore()
const { send } = useWebSocket()

const selectedTarget = ref<string | null>(null)
const votedCount = ref(0)
const totalVotes = computed(() => gameStore.currentRoom?.players?.filter(p => p.isAlive).length || 0)

const currentPlayer = computed(() => gameStore.currentPlayer)
const room = computed(() => gameStore.currentRoom)

const votablePlayers = computed(() => {
  if (!room.value || !currentPlayer.value) return []
  return room.value.players.filter(p => p.isAlive && p.id !== currentPlayer.value?.id)
})

const voteProgress = computed(() => {
  if (totalVotes.value === 0) return 0
  return Math.round((votedCount.value / totalVotes.value) * 100)
})

const voteStatus = computed(() => {
  if (voteProgress.value === 100) return 'success'
  if (voteProgress.value >= 50) return 'warning'
  return ''
})

const selectTarget = (playerId: string) => {
  selectedTarget.value = playerId
}

const handleConfirm = () => {
  if (!selectedTarget.value) {
    ElMessage.error('请选择投票目标')
    return
  }

  send('VOTE', { targetId: selectedTarget.value })
  votedCount.value++
  ElMessage.success('投票成功')
}

const handleClose = () => {
  visible.value = false
  selectedTarget.value = null
}
</script>

<style scoped lang="scss">
.voting {
  .tip {
    text-align: center;
    color: #666;
    margin-bottom: 20px;
    font-size: 0.95rem;
  }

  .players {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
    gap: 15px;
    margin-bottom: 30px;
  }

  .player-card {
    padding: 15px;
    background: #f5f5f5;
    border: 2px solid #e0e0e0;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s ease;

    &:hover {
      background: #e8e8e8;
      transform: translateY(-3px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }

    &.selected {
      border-color: #667eea;
      background: rgba(102, 126, 234, 0.1);
    }

    .player-info {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 5px;

      .seat {
        font-size: 0.9rem;
        color: #999;
      }

      .name {
        font-size: 1.1rem;
        font-weight: bold;
        color: #333;
      }

      .status {
        font-size: 0.85rem;
        color: #666;
      }
    }
  }

  .vote-info {
    background: #f9f9f9;
    padding: 20px;
    border-radius: 8px;
    margin-top: 20px;

    .vote-count {
      text-align: center;
      margin-top: 10px;
      font-size: 0.9rem;
      color: #666;
    }
  }
}
</style>
