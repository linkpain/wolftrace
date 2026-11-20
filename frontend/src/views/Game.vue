<template>
  <div class="game-container">
    <NicknameReveal v-if="gameStore.isNicknameRevealPhase" />
    <RoleAnnouncement v-if="gameStore.isRoleAnnouncementPhase" />
    <RoomSelection v-if="gameStore.isRoomSelectionPhase" />
    <SkillSelection v-if="gameStore.isSkillSelectionPhase" />
    <NightResult v-if="gameStore.isNightResultPhase" />
    
    <div v-if="gameStore.isWaitingPhase || gameStore.isDiscussionPhase || gameStore.isVotingPhase || gameStore.isGameOverPhase" class="game-main">
      <div class="game-header">
        <div class="room-info">
          <h2>{{ gameStore.currentRoom?.name || '等待中...' }}</h2>
          <div class="room-id">
            <span>房间ID:</span>
            <el-tag type="info">{{ gameStore.currentRoom?.id || '-' }}</el-tag>
          </div>
          <div class="game-status">
            <el-tag :type="phaseType" size="large">{{ phaseText }}</el-tag>
            <el-tag v-if="gameStore.currentRound > 0" type="info" size="large">第 {{ gameStore.currentRound }} 轮</el-tag>
          </div>
        </div>
        
        <div v-if="gameStore.currentPlayer" class="player-info">
          <div class="info-item">
            <span>你的身份:</span>
            <el-tag :type="roleType" size="large">{{ roleText }}</el-tag>
          </div>
          <div class="info-item">
            <span>名字:</span>
            <el-tag type="primary">{{ gameStore.currentPlayer.assignedName || gameStore.currentPlayer.realName }}</el-tag>
          </div>
          <div v-if="gameStore.isWolf" class="info-item">
            <span>普通刀:</span>
            <el-tag type="danger">{{ gameStore.currentPlayer.normalWeapons || 0 }}</el-tag>
          </div>
          <div v-if="gameStore.isWolf && gameStore.hasFatalWeapon" class="info-item">
            <span>必杀刀:</span>
            <el-tag type="danger">{{ gameStore.currentPlayer.fatalWeapon || 0 }}</el-tag>
          </div>
          <div v-if="gameStore.isVillage" class="info-item">
            <span>护盾:</span>
            <el-tag type="success">{{ gameStore.currentPlayer.shields || 0 }}</el-tag>
          </div>
        </div>
      </div>

      <div class="game-content">
        <div class="seats-container">
          <div 
            v-for="player in gameStore.currentRoom?.players || []" 
            :key="player.id"
            class="seat"
            :class="{ 
              'is-me': player.id === gameStore.currentPlayer?.id,
              'is-dead': !player.isAlive,
              'is-wolf': gameStore.isWolf && (player.role === 'WOLF_KING' || player.role === 'WOLF'),
              'is-host': player.id === gameStore.currentRoom?.hostId
            }"
          >
            <div class="seat-number">{{ player.seatNumber }}号</div>
            <div class="player-name">
              {{ player.id === gameStore.currentPlayer?.id ? (player.assignedName || player.realName) : `${player.seatNumber}号玩家` }}
            </div>
            <div class="player-status">
              <el-tag v-if="!player.isAlive" type="danger" size="small">出局</el-tag>
              <el-tag v-else-if="player.isReady" type="success" size="small">已准备</el-tag>
              <el-tag v-else type="info" size="small">未准备</el-tag>
            </div>
            <div v-if="player.id === gameStore.currentRoom?.hostId" class="host-badge">房主</div>
          </div>
        </div>

        <div class="action-panel">
          <div style="margin-bottom: 10px; color: white; background: rgba(0,0,0,0.5); padding: 10px;">
            <div>isHost: {{ gameStore.isHost }}</div>
            <div>canStartGame: {{ gameStore.canStartGame }}</div>
            <div>canReady: {{ gameStore.canReady }}</div>
            <div>isWaitingPhase: {{ gameStore.isWaitingPhase }}</div>
            <div>gamePhase: {{ gameStore.currentRoom?.gamePhase }}</div>
            <div>currentPlayer: {{ gameStore.currentPlayer?.id }}</div>
            <div>roomHostId: {{ gameStore.currentRoom?.hostId }}</div>
          </div>
          
          <el-button 
            v-if="gameStore.isHost && gameStore.canStartGame" 
            type="primary" 
            size="large" 
            @click="handleStartGame"
            class="action-btn"
          >
            开始游戏
          </el-button>
          
          <el-button 
            v-if="!gameStore.isHost && gameStore.canReady" 
            :type="gameStore.currentPlayer?.isReady ? 'warning' : 'success'" 
            size="large" 
            @click="handleReady"
            class="action-btn"
          >
            {{ gameStore.currentPlayer?.isReady ? '取消准备' : '准备' }}
          </el-button>
          
          <el-button 
            v-if="gameStore.isVotingPhase && gameStore.canVote" 
            type="success" 
            size="large" 
            @click="showVoting"
            class="action-btn"
          >
            投票
          </el-button>
        </div>
      </div>
    </div>

    <Voting v-if="showVotingDialog" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useGameStore } from '@/store/game'
import { useWebSocket } from '@/composables/useWebSocket'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import RoleAnnouncement from '@/views/RoleAnnouncement.vue'
import NicknameReveal from '@/views/NicknameReveal.vue'
import RoomSelection from '@/views/RoomSelection.vue'
import SkillSelection from '@/views/SkillSelection.vue'
import NightResult from '@/views/NightResult.vue'
import Voting from '@/views/Voting.vue'

const gameStore = useGameStore()
const { send, onMessage, connect } = useWebSocket()
const router = useRouter()

const showVotingDialog = ref(false)
let pollingTimer: number | null = null

const fetchRoomData = async () => {
  const savedRoomId = sessionStorage.getItem('roomId')
  if (!savedRoomId) return
  
  try {
    const response = await fetch(`http://localhost:8080/rooms/${savedRoomId}`)
    const result = await response.json()
    
    if (result.success && result.data) {
      gameStore.setRoom(result.data)
      
      const savedPlayerId = sessionStorage.getItem('playerId')
      if (savedPlayerId) {
        const player = result.data.players?.find((p: any) => p.id === savedPlayerId)
        if (player) {
          gameStore.setCurrentPlayer(player)
        }
      }
    }
  } catch (error) {
    console.error('获取房间数据失败:', error)
  }
}

onMounted(async () => {
  connect()
  
  const savedRoomId = sessionStorage.getItem('roomId')
  const savedPlayerId = sessionStorage.getItem('playerId')
  
  console.log('sessionStorage数据:', { savedRoomId, savedPlayerId })
  
  try {
    console.log('初始状态:', {
      currentRoom: gameStore.currentRoom,
      currentPlayer: gameStore.currentPlayer,
      gamePhase: gameStore.gamePhase,
      isWaitingPhase: gameStore.isWaitingPhase
    })
  } catch (error) {
    console.error('打印初始状态时出错:', error)
  }
  
  if (savedRoomId && savedPlayerId) {
    try {
      const response = await fetch(`http://localhost:8080/rooms/${savedRoomId}`)
      const result = await response.json()
      
      if (result.success && result.data) {
        console.log('恢复房间数据:', result.data)
        console.log('房间gamePhase:', result.data.gamePhase)
        gameStore.setRoom(result.data)
        
        const player = result.data.players?.find((p: any) => p.id === savedPlayerId)
        if (player) {
          console.log('恢复玩家数据:', player)
          gameStore.setCurrentPlayer(player)
        }
      } else {
        sessionStorage.removeItem('roomId')
        sessionStorage.removeItem('playerId')
        router.push('/')
      }
    } catch (error) {
      console.error('恢复游戏状态失败:', error)
      sessionStorage.removeItem('roomId')
      sessionStorage.removeItem('playerId')
      router.push('/')
    }
  }
  
  pollingTimer = window.setInterval(fetchRoomData, 3000)
})

onUnmounted(() => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
})

const phaseText = computed(() => {
  const phase = gameStore.gamePhase
  const phaseMap: Record<string, string> = {
    'WAITING': '等待中',
    'ROLE_ANNOUNCEMENT': '身份公告',
    'NICKNAME_ASSIGNMENT': '昵称分配',
    'ROOM_SELECTION': '房间选择',
    'SKILL_SELECTION': '技能选择',
    'NIGHT_RESULT': '夜晚结算',
    'DISCUSSION': '讨论阶段',
    'VOTING': '投票阶段',
    'GAME_OVER': '游戏结束'
  }
  return phaseMap[phase || 'WAITING']
})

const phaseType = computed(() => {
  const phase = gameStore.gamePhase
  if (phase === 'WAITING' || phase === 'ROLE_ANNOUNCEMENT' || phase === 'NICKNAME_ASSIGNMENT') {
    return 'info'
  } else if (phase === 'ROOM_SELECTION' || phase === 'SKILL_SELECTION') {
    return 'warning'
  } else if (phase === 'NIGHT_RESULT') {
    return 'danger'
  } else if (phase === 'DISCUSSION' || phase === 'VOTING') {
    return 'success'
  }
  return 'info'
})

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
    return 'warning'
  }
  return 'success'
})

const handleStartGame = () => {
  send('START_GAME', {
    roomId: gameStore.currentRoom?.id,
    playerId: gameStore.currentPlayer?.id
  })
  ElMessage.success('游戏开始')
}

const handleReady = () => {
  send('READY', {
    roomId: gameStore.currentRoom?.id,
    playerId: gameStore.currentPlayer?.id
  })
}

const showVoting = () => {
  showVotingDialog.value = true
}

onMessage('PLAYER_JOINED', (data) => {
  gameStore.setRoom(data.room)
  const savedPlayerId = sessionStorage.getItem('playerId')
  if (savedPlayerId) {
    const player = data.room.players?.find((p: any) => p.id === savedPlayerId)
    if (player) {
      gameStore.setCurrentPlayer(player)
    }
  }
  ElMessage.success('有新玩家加入房间')
})

onMessage('PLAYER_READY', (data) => {
  gameStore.setRoom(data.room)
  const savedPlayerId = sessionStorage.getItem('playerId')
  if (savedPlayerId) {
    const player = data.room.players?.find((p: any) => p.id === savedPlayerId)
    if (player) {
      gameStore.setCurrentPlayer(player)
    }
  }
})

onMessage('GAME_STARTED', (data) => {
  gameStore.setRoom(data.room)
  const savedPlayerId = sessionStorage.getItem('playerId')
  if (savedPlayerId) {
    const player = data.room.players?.find((p: any) => p.id === savedPlayerId)
    if (player) {
      gameStore.setCurrentPlayer(player)
    }
  }
  ElMessage.success('游戏开始！')
})

onMessage('ROLE_ANNOUNCEMENT_PHASE', (data) => {
  gameStore.setRoom(data.room)
  const savedPlayerId = sessionStorage.getItem('playerId')
  if (savedPlayerId) {
    const player = data.room.players?.find((p: any) => p.id === savedPlayerId)
    if (player) {
      gameStore.setCurrentPlayer(player)
    }
  }
})

onMessage('ROOM_SELECTION_PHASE', (data) => {
  gameStore.setRoom(data.room)
  const savedPlayerId = sessionStorage.getItem('playerId')
  if (savedPlayerId) {
    const player = data.room.players?.find((p: any) => p.id === savedPlayerId)
    if (player) {
      gameStore.setCurrentPlayer(player)
    }
  }
})

onMessage('SKILL_SELECTION_PHASE', (data) => {
  gameStore.setRoom(data.room)
  const savedPlayerId = sessionStorage.getItem('playerId')
  if (savedPlayerId) {
    const player = data.room.players?.find((p: any) => p.id === savedPlayerId)
    if (player) {
      gameStore.setCurrentPlayer(player)
    }
  }
})

onMessage('WEAPON_USED', (data) => {
  gameStore.setRoom(data.room)
  const savedPlayerId = sessionStorage.getItem('playerId')
  if (savedPlayerId) {
    const player = data.room.players?.find((p: any) => p.id === savedPlayerId)
    if (player) {
      gameStore.setCurrentPlayer(player)
    }
  }
})

onMessage('SHIELD_USED', (data) => {
  gameStore.setRoom(data.room)
  const savedPlayerId = sessionStorage.getItem('playerId')
  if (savedPlayerId) {
    const player = data.room.players?.find((p: any) => p.id === savedPlayerId)
    if (player) {
      gameStore.setCurrentPlayer(player)
    }
  }
})

onMessage('SKILL_SKIPPED', (data) => {
  gameStore.setRoom(data.room)
  const savedPlayerId = sessionStorage.getItem('playerId')
  if (savedPlayerId) {
    const player = data.room.players?.find((p: any) => p.id === savedPlayerId)
    if (player) {
      gameStore.setCurrentPlayer(player)
    }
  }
})

onMessage('NIGHT_RESULT', (data) => {
  gameStore.setRoom(data.room)
  gameStore.setNightResult(data)
  const savedPlayerId = sessionStorage.getItem('playerId')
  if (savedPlayerId) {
    const player = data.room.players?.find((p: any) => p.id === savedPlayerId)
    if (player) {
      gameStore.setCurrentPlayer(player)
    }
  }
})

onMessage('DISCUSSION_PHASE', (data) => {
  gameStore.setRoom(data.room)
  const savedPlayerId = sessionStorage.getItem('playerId')
  if (savedPlayerId) {
    const player = data.room.players?.find((p: any) => p.id === savedPlayerId)
    if (player) {
      gameStore.setCurrentPlayer(player)
    }
  }
  ElMessage.success('进入讨论阶段')
})

onMessage('VOTE_CAST', (data) => {
  ElMessage.success('投票成功')
  showVotingDialog.value = false
})

onMessage('GAME_OVER', (data) => {
  gameStore.setRoom(data.room)
  gameStore.setGameOverData(data)
  const savedPlayerId = sessionStorage.getItem('playerId')
  if (savedPlayerId) {
    const player = data.room.players?.find((p: any) => p.id === savedPlayerId)
    if (player) {
      gameStore.setCurrentPlayer(player)
    }
  }
  ElMessage.success(`游戏结束！${data.winner === 'WOLF' ? '狼阵营胜利' : '村民阵营胜利'}`)
})
</script>

<style scoped lang="scss">
.game-container {
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.game-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.game-header {
  padding: 20px;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom:1px solid rgba(255, 255, 255, 0.1);
}

.room-info {
  h2 {
    color: white;
    margin: 0 0 10px 0;
    font-size: 1.5rem;
  }

  .room-id {
    color: white;
    margin-bottom: 10px;
    font-size: 0.9rem;

    span {
      margin-right: 5px;
      opacity: 0.8;
    }
  }

  .game-status {
    display: flex;
    gap: 10px;
  }
}

.player-info {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;

  .info-item {
    color: white;
    font-size: 0.9rem;
    
    span {
      margin-right: 5px;
      opacity: 0.8;
    }
  }
}

.game-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 20px;
  overflow: hidden;
}

.seats-container {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 20px;
  padding: 20px;
  overflow-y: auto;
}

.seat {
  background: rgba(255, 255, 255, 0.1);
  border: 2px solid rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  padding: 15px;
  text-align: center;
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;

  &:hover {
    background: rgba(255, 255, 255, 0.2);
    transform: translateY(-5px);
    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3);
  }

  &.is-me {
    border-color: #667eea;
    background: rgba(102, 126, 234, 0.2);
  }

  &.is-dead {
    opacity: 0.5;
    filter: grayscale(100%);
  }

  &.is-wolf {
    border-color: #f56c6c;
  }

  &.is-host {
    border-color: #e6a23c;
    background: rgba(230, 162, 60, 0.15);
  }

  .seat-number {
    font-size: 1.5rem;
    font-weight: bold;
    color: #667eea;
    margin-bottom: 10px;
  }

  .player-name {
    font-size: 1.2rem;
    color: white;
    margin-bottom: 8px;
  }

  .player-status {
    display: flex;
    justify-content: center;
  }

  .host-badge {
    position: absolute;
    top: -10px;
    right: -10px;
    background: #e6a23c;
    color: white;
    padding: 4px 8px;
    border-radius: 10px;
    font-size: 0.75rem;
    font-weight: bold;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  }
}

.action-panel {
  display: flex;
  justify-content: center;
  gap: 15px;
  padding: 20px;
  background: rgba(0, 0, 0, 0.3);
  border-top: 1px solid rgba(255, 255, 255, 0.1);

  .action-btn {
    min-width: 150px;
    height: 50px;
    font-size: 1.1rem;
    border-radius: 8px;
  }
}
</style>
