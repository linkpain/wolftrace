<template>
  <div class="name-assignment-container">
    <el-card class="name-assignment-card">
      <template #header>
        <div class="card-header">
          <span>名字分配</span>
          <el-tag type="success">系统已随机分配名字</el-tag>
        </div>
      </template>
      
      <div class="name-assignment-content">
        <div class="my-assignment">
          <h3>你的名字</h3>
          <el-tag size="large" type="primary" class="my-name">
            {{ currentPlayer?.assignedName }}
          </el-tag>
          <p class="hint">请记住你的名字，游戏中使用这个名字发言</p>
        </div>
        
        <el-divider />
        
        <div class="all-assignments">
          <h3>所有玩家名字分配</h3>
          <el-table :data="playerAssignments" style="width: 100%">
            <el-table-column prop="seatNumber" label="座位" width="80" />
            <el-table-column prop="assignedName" label="分配名字" />
          </el-table>
        </div>
        
        <div class="button-group">
          <el-button type="primary" @click="confirmAssignment">
            确认并继续
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useGameStore } from '@/store/game'
import { useWebSocket } from '@/composables/useWebSocket'
import { useRouter } from 'vue-router'

const gameStore = useGameStore()
const router = useRouter()
const { send } = useWebSocket()

const currentPlayer = computed(() => gameStore.currentPlayer)

const playerAssignments = computed(() => {
  return gameStore.currentRoom?.players.map(p => ({
    seatNumber: p.seatNumber,
    assignedName: p.assignedName
  })) || []
})

function confirmAssignment() {
  send('NAME_ASSIGNMENT_CONFIRMED', {})
  ElMessage.success('已确认，即将进入房间选择阶段')
}
</script>

<style scoped>
.name-assignment-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.name-assignment-card {
  width: 600px;
  max-width: 90%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.name-assignment-content {
  padding: 20px 0;
}

.my-assignment {
  text-align: center;
  padding: 30px 0;
}

.my-assignment h3 {
  margin-bottom: 20px;
  color: #303133;
}

.my-name {
  font-size: 24px;
  padding: 15px 30px;
  margin-bottom: 15px;
}

.hint {
  color: #909399;
  font-size: 14px;
  margin: 0;
}

.all-assignments {
  margin-top: 30px;
}

.all-assignments h3 {
  margin-bottom: 20px;
  color: #303133;
}

.button-group {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}
</style>
