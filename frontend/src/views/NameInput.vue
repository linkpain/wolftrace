<template>
  <div class="name-input-container">
    <el-card class="name-input-card">
      <template #header>
        <div class="card-header">
          <span>名字输入</span>
          <el-tag type="info">请输入你的昵称</el-tag>
        </div>
      </template>
      
      <div class="name-input-content">
        <el-form :model="form" label-width="80px">
          <el-form-item label="昵称">
            <el-input
              v-model="form.name"
              placeholder="请输入昵称"
              maxlength="20"
              show-word-limit
              @keyup.enter="submitName"
            />
          </el-form-item>
        </el-form>
        
        <div class="button-group">
          <el-button type="primary" @click="submitName" :disabled="!form.name">
            提交名字
          </el-button>
        </div>
        
        <div class="player-list">
          <h4>已输入名字的玩家：</h4>
          <el-tag
            v-for="player in playersWithNames"
            :key="player.id"
            class="player-tag"
            type="success"
          >
            {{ player.realName }}
          </el-tag>
          <el-tag
            v-for="player in playersWithoutNames"
            :key="player.id"
            class="player-tag"
            type="info"
          >
            等待输入...
          </el-tag>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useGameStore } from '@/store/game'
import { useWebSocket } from '@/composables/useWebSocket'

const gameStore = useGameStore()
const { send } = useWebSocket()

const form = ref({
  name: ''
})

const playersWithNames = computed(() => {
  return gameStore.currentRoom?.players.filter(p => p.realName) || []
})

const playersWithoutNames = computed(() => {
  return gameStore.currentRoom?.players.filter(p => !p.realName) || []
})

function submitName() {
  if (!form.value.name) {
    ElMessage.warning('请输入昵称')
    return
  }

  send('NAME_INPUT', {
    name: form.value.name
  })

  gameStore.setInputName(form.value.name)
}
</script>

<style scoped>
.name-input-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.name-input-card {
  width: 500px;
  max-width: 90%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.name-input-content {
  padding: 20px 0;
}

.button-group {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.player-list {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.player-list h4 {
  margin-bottom: 15px;
  color: #303133;
}

.player-tag {
  margin-right: 10px;
  margin-bottom: 10px;
}
</style>
