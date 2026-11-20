<template>
  <el-dialog 
    v-model="visible" 
    title="使用技能" 
    width="500px"
    @close="handleClose"
  >
    <div class="skill-selection">
      <div class="skill-type">
        <el-button 
          type="primary" 
          size="large" 
          @click="selectAttack"
          :disabled="!canAttack"
          class="skill-btn"
        >
          <div class="btn-content">
            <span class="icon">🗡️</span>
            <span>使用刀具</span>
          </div>
        </el-button>
        
        <el-button 
          type="success" 
          size="large" 
          @click="selectShield"
          :disabled="!canShield"
          class="skill-btn"
        >
          <div class="btn-content">
            <span class="icon">🛡️</span>
            <span>使用护盾</span>
          </div>
        </el-button>
      </div>

      <div v-if="selectedSkill === 'ATTACK'" class="target-selection">
        <p class="tip">请选择要攻击的目标（仅限同房间玩家）</p>
        <div class="players">
          <div 
            v-for="player in targetablePlayers" 
            :key="player.id"
            class="player-card"
            :class="{ selected: selectedTarget === player.id }"
            @click="selectTarget(player.id)"
          >
            <div class="player-info">
              <span class="seat">{{ player.seatNumber }}号</span>
              <span class="name">{{ player.name }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="selectedSkill === 'SHIELD'" class="target-selection">
        <p class="tip">请选择要保护的目标（可以是自己或同房间玩家）</p>
        <div class="players">
          <div 
            v-for="player in targetablePlayers" 
            :key="player.id"
            class="player-card"
            :class="{ selected: selectedTarget === player.id }"
            @click="selectTarget(player.id)"
          >
            <div class="player-info">
              <span class="seat">{{ player.seatNumber }}号</span>
              <span class="name">{{ player.name }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button 
        type="primary" 
        @click="handleConfirm" 
        :disabled="!selectedSkill || !selectedTarget"
      >
        确认
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

const selectedSkill = ref<'ATTACK' | 'SHIELD' | null>(null)
const selectedTarget = ref<string | null>(null)

const currentPlayer = computed(() => gameStore.currentPlayer)
const room = computed(() => gameStore.currentRoom)

const canAttack = computed(() => {
  return currentPlayer.value?.weapons && currentPlayer.value.weapons > 0
})

const canShield = computed(() => {
  return currentPlayer.value?.shields && currentPlayer.value.shields > 0
})

const targetablePlayers = computed(() => {
  if (!room.value || !currentPlayer.value) return []
  return room.value.players.filter(p => p.isAlive && p.id !== currentPlayer.value?.id)
})

const selectAttack = () => {
  selectedSkill.value = 'ATTACK'
  selectedTarget.value = null
}

const selectShield = () => {
  selectedSkill.value = 'SHIELD'
  selectedTarget.value = null
}

const selectTarget = (playerId: string) => {
  selectedTarget.value = playerId
}

const handleConfirm = () => {
  if (!selectedSkill.value || !selectedTarget.value) {
    ElMessage.error('请选择技能和目标')
    return
  }

  send('USE_SKILL', {
    skillType: selectedSkill.value,
    targetId: selectedTarget.value
  })

  visible.value = false
  ElMessage.success('技能使用成功')
}

const handleClose = () => {
  visible.value = false
  selectedSkill.value = null
  selectedTarget.value = null
}
</script>

<style scoped lang="scss">
.skill-selection {
  .tip {
    text-align: center;
    color: #666;
    margin-bottom: 20px;
    font-size: 0.95rem;
  }

  .skill-type {
    display: flex;
    gap: 20px;
    justify-content: center;
    margin-bottom: 30px;
  }

  .skill-btn {
    flex: 1;
    height: 80px;
    font-size: 1.1rem;
    border-radius: 12px;

    .btn-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;

      .icon {
        font-size: 2rem;
      }
    }
  }

  .target-selection {
    .players {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
      gap: 15px;
      margin-top: 20px;
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
      }
    }
  }
}
</style>
