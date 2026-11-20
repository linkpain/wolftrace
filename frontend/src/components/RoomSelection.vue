<template>
  <el-dialog 
    v-model="visible" 
    title="选择房间" 
    width="500px"
    @close="handleClose"
  >
    <div class="room-selection">
      <p class="tip">请选择你要进入的房间（不能进入上一轮所在的房间）</p>
      
      <div class="rooms">
        <div 
          v-for="(room, index) in rooms" 
          :key="index"
          class="room-card"
          :class="{ disabled: isDisabled(index) }"
          @click="selectRoom(index)"
        >
          <div class="room-icon">{{ room.icon }}</div>
          <div class="room-name">{{ room.name }}</div>
          <div class="room-players">{{ room.players || 0 }}人</div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useGameStore } from '@/store/game'
import { useWebSocket } from '@/composables/useWebSocket'
import { ElMessage } from 'element-plus'

interface RoomOption {
  icon: string
  name: string
  players: number
}

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

const rooms = ref<RoomOption[]>([
  { icon: '☀️', name: '天', players: 0 },
  { icon: '🌍', name: '地', players: 0 },
  { icon: '👥', name: '人', players: 0 }
])

const isDisabled = (index: number) => {
  const lastRoom = gameStore.currentPlayer?.lastRoom
  return lastRoom === index
}

const selectRoom = (index: number) => {
  if (isDisabled(index)) {
    ElMessage.warning('不能连续进入同一房间')
    return
  }

  send('SELECT_ROOM', { roomType: index })
  visible.value = false
  ElMessage.success(`已选择${rooms[index].name}房间`)
}

const handleClose = () => {
  visible.value = false
}
</script>

<style scoped lang="scss">
.room-selection {
  .tip {
    text-align: center;
    color: #666;
    margin-bottom: 20px;
    font-size: 0.95rem;
  }

  .rooms {
    display: flex;
    gap: 20px;
    justify-content: center;
  }

  .room-card {
    flex: 1;
    padding: 30px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 12px;
    text-align: center;
    cursor: pointer;
    transition: all 0.3s ease;
    border: 3px solid transparent;

    &:hover:not(.disabled) {
      transform: translateY(-5px);
      box-shadow: 0 8px 16px rgba(102, 126, 234, 0.4);
    }

    &.disabled {
      opacity: 0.4;
      cursor: not-allowed;
      background: #ccc;
    }

    .room-icon {
      font-size: 3rem;
      margin-bottom: 10px;
    }

    .room-name {
      font-size: 1.3rem;
      font-weight: bold;
      color: white;
      margin-bottom: 5px;
    }

    .room-players {
      font-size: 0.9rem;
      color: rgba(255, 255, 255, 0.9);
    }
  }
}
</style>
