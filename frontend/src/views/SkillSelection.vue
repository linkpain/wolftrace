<template>
  <div class="skill-selection-container">
    <el-card class="skill-selection-card">
      <template #header>
        <div class="card-header">
          <span>技能选择</span>
          <el-tag type="info">第 {{ currentRound }} 轮</el-tag>
        </div>
      </template>
      
      <div class="skill-selection-content">
        <div v-if="isWolf" class="wolf-skills">
          <h3>狼阵营技能</h3>
          <div class="weapons">
            <div class="weapon-section">
              <h4>普通刀 (剩余: {{ currentPlayer?.normalWeapons }})</h4>
              <div class="action-buttons">
                <el-button
                  type="danger"
                  @click="useNormalWeapon"
                  :disabled="!hasNormalWeapons || !canUseSkill"
                >
                  使用
                </el-button>
                <el-button
                  @click="skipSkill"
                  :disabled="!canUseSkill"
                >
                  不使用
                </el-button>
              </div>
            </div>
            
            <div v-if="hasFatalWeapon" class="weapon-section">
              <h4>必杀刀 (剩余: {{ currentPlayer?.fatalWeapon }})</h4>
              <el-alert
                v-if="!canUseFatalWeapon"
                title="必杀刀只能在第2或第3轮使用"
                type="warning"
                :closable="false"
                show-icon
                style="margin-bottom: 10px"
              />
              <div class="action-buttons">
                <el-button
                  type="danger"
                  @click="useFatalWeapon"
                  :disabled="!canUseFatalWeapon || !canUseSkill"
                >
                  使用
                </el-button>
                <el-button
                  @click="skipSkill"
                  :disabled="!canUseSkill"
                >
                  不使用
                </el-button>
              </div>
            </div>
          </div>
        </div>
        
        <div v-if="isVillage" class="village-skills">
          <h3>村民阵营技能</h3>
          <div class="shields">
            <div class="shield-section">
              <h4>护盾 (剩余: {{ currentPlayer?.shields }})</h4>
              <div class="target-selection">
                <el-select
                  v-model="shieldTarget"
                  placeholder="选择保护目标"
                  style="width: 100%"
                >
                  <el-option
                    v-for="player in allPlayers"
                    :key="player.id"
                    :label="`${player.seatNumber}号座位`"
                    :value="player.id"
                  />
                </el-select>
              </div>
              <div class="action-buttons">
                <el-button
                  type="primary"
                  @click="useShield"
                  :disabled="!shieldTarget || !hasShields || !canUseSkill"
                >
                  使用护盾
                </el-button>
                <el-button
                  @click="skipSkill"
                  :disabled="!canUseSkill"
                >
                  不使用
                </el-button>
              </div>
            </div>
          </div>
        </div>
        
        <div class="info">
          <el-alert
            title="技能说明"
            type="info"
            :closable="false"
            show-icon
          >
            <template #default>
              <p v-if="isWolf">
                <strong>普通刀：</strong>攻击房间内的村长，可被护盾抵消<br>
                <strong>必杀刀：</strong>必定命中，但只能在第2或第3轮使用
              </p>
              <p v-if="isVillage">
                <strong>护盾：</strong>保护自己或同房间的玩家，抵消一次刀具攻击<br>
                <strong>注意：</strong>不同房间使用护盾不会生效，但护盾次数仍会被消耗
              </p>
            </template>
          </el-alert>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useGameStore } from '@/store/game'
import { useWebSocket } from '@/composables/useWebSocket'
import { Weapon } from '@/types/game'

const gameStore = useGameStore()
const { send } = useWebSocket()

const shieldTarget = ref<string>('')

const currentRound = computed(() => gameStore.currentRound)
const currentPlayer = computed(() => gameStore.currentPlayer)
const isWolf = computed(() => gameStore.isWolf)
const isVillage = computed(() => gameStore.isVillage)
const hasNormalWeapons = computed(() => gameStore.hasNormalWeapons)
const hasFatalWeapon = computed(() => gameStore.hasFatalWeapon)
const hasShields = computed(() => gameStore.hasShields)
const canUseFatalWeapon = computed(() => gameStore.canUseFatalWeapon)
const canUseSkill = computed(() => !currentPlayer.value?.hasMadeSkillChoice)

const allPlayers = computed(() => {
  return gameStore.currentRoom?.players || []
})

function useNormalWeapon() {
  if (!hasNormalWeapons.value) {
    ElMessage.warning('普通刀不足')
    return
  }

  send('WEAPON_USE', {
    roomId: gameStore.currentRoom?.id,
    playerId: gameStore.currentPlayer?.id,
    weaponType: Weapon.NORMAL
  })

  ElMessage.success('普通刀已使用')
}

function useFatalWeapon() {
  if (!canUseFatalWeapon.value) {
    ElMessage.warning('必杀刀只能在第2或第3轮使用')
    return
  }

  send('WEAPON_USE', {
    roomId: gameStore.currentRoom?.id,
    playerId: gameStore.currentPlayer?.id,
    weaponType: Weapon.FATAL
  })

  ElMessage.success('必杀刀已使用')
}

function useShield() {
  if (!shieldTarget.value || !hasShields.value) {
    ElMessage.warning('请选择保护目标或护盾不足')
    return
  }

  send('SHIELD_USE', {
    roomId: gameStore.currentRoom?.id,
    playerId: gameStore.currentPlayer?.id,
    targetId: shieldTarget.value
  })

  shieldTarget.value = ''
  ElMessage.success('护盾已使用')
}

function skipSkill() {
  send('SKIP_SKILL', {
    roomId: gameStore.currentRoom?.id,
    playerId: gameStore.currentPlayer?.id
  })
  ElMessage.success('已选择不使用技能')
}
</script>

<style scoped>
.skill-selection-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.skill-selection-card {
  width: 700px;
  max-width: 90%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.skill-selection-content {
  padding: 20px 0;
}

.wolf-skills,
.village-skills {
  margin-bottom: 30px;
}

.wolf-skills h3,
.village-skills h3 {
  margin-bottom: 20px;
  color: #303133;
}

.weapons,
.shields {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.weapon-section,
.shield-section {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 20px;
}

.weapon-section h4,
.shield-section h4 {
  margin-bottom: 15px;
  color: #606266;
}

.action-buttons {
  display: flex;
  gap: 10px;
  margin-top: 15px;
}

.action-buttons .el-button {
  flex: 1;
}

.target-selection {
  margin-bottom: 15px;
}

.target-selection .el-select {
  width: 100%;
}

.info {
  margin-top: 30px;
}

.info p {
  margin: 5px 0;
  line-height: 1.6;
}
</style>
