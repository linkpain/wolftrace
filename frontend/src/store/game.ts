import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Player, Room, GamePhase, GameMessage, NightResult, GameOverData } from '@/types/game'

export const useGameStore = defineStore('game', () => {
  const currentRoom = ref<Room | null>(null)
  const currentPlayer = ref<Player | null>(null)
  const isConnected = ref(false)
  const messages = ref<GameMessage[]>([])
  const nightResult = ref<NightResult | null>(null)
  const gameOverData = ref<GameOverData | null>(null)
  const inputName = ref('')

  const gamePhase = computed(() => {
    const phase = currentRoom.value?.gamePhase || 'WAITING' as GamePhase
    console.log('gamePhase computed:', phase)
    return phase
  })
  const isGameStarted = computed(() => gamePhase.value !== 'WAITING')
  const isWaitingPhase = computed(() => gamePhase.value === 'WAITING')
  const isGameOver = computed(() => currentRoom.value?.isGameOver || false)
  const winner = computed(() => currentRoom.value?.winner || null)
  const currentRound = computed(() => currentRoom.value?.currentRound || 0)
  const isHost = computed(() => {
    if (!currentPlayer.value || !currentRoom.value) {
      return false
    }
    const result = String(currentPlayer.value.id) === String(currentRoom.value.hostId)
    console.log('isHost computed:', {
      currentPlayerId: currentPlayer.value?.id,
      currentPlayerIdType: typeof currentPlayer.value?.id,
      roomHostId: currentRoom.value?.hostId,
      roomHostIdType: typeof currentRoom.value?.hostId,
      result
    })
    return result
  })
  const canReady = computed(() => {
    if (!currentPlayer.value || !currentRoom.value) {
      return false
    }
    const result = !isHost.value && !isGameStarted.value
    console.log('canReady computed:', {
      isHost: isHost.value,
      isGameStarted: isGameStarted.value,
      result
    })
    return result
  })
  const canStartGame = computed(() => {
    if (!currentPlayer.value || !currentRoom.value) {
      return false
    }
    const result = isHost.value && !isGameStarted.value && currentRoom.value.players.length >= 4
    console.log('canStartGame computed:', {
      isHost: isHost.value,
      isGameStarted: isGameStarted.value,
      playersLength: currentRoom.value?.players.length,
      result
    })
    return result
  })
  const canVote = computed(() => {
    if (!currentPlayer.value || !currentRoom.value) {
      return false
    }
    const result = gamePhase.value === 'VOTING' && currentPlayer.value.isAlive
    console.log('canVote computed:', {
      gamePhase: gamePhase.value,
      isAlive: currentPlayer.value?.isAlive,
      result
    })
    return result
  })
  const isNicknameRevealPhase = computed(() => gamePhase.value === 'NICKNAME_REVEAL')
  const isRoleAnnouncementPhase = computed(() => gamePhase.value === 'ROLE_ANNOUNCEMENT')
  const isRoomSelectionPhase = computed(() => gamePhase.value === 'ROOM_SELECTION')
  const isSkillSelectionPhase = computed(() => gamePhase.value === 'SKILL_SELECTION')
  const isNightResultPhase = computed(() => gamePhase.value === 'NIGHT_RESULT')
  const isDiscussionPhase = computed(() => gamePhase.value === 'DISCUSSION')
  const isVotingPhase = computed(() => gamePhase.value === 'VOTING')
  const isGameOverPhase = computed(() => gamePhase.value === 'GAME_OVER')

  const isWolf = computed(() => currentPlayer.value?.role === 'WOLF_KING' || currentPlayer.value?.role === 'WOLF')
  const isVillage = computed(() => currentPlayer.value?.role === 'VILLAGE_HEAD' || currentPlayer.value?.role === 'VILLAGER')
  const hasNormalWeapons = computed(() => (currentPlayer.value?.normalWeapons || 0) > 0)
  const hasFatalWeapon = computed(() => (currentPlayer.value?.fatalWeapon || 0) > 0)
  const hasShields = computed(() => (currentPlayer.value?.shields || 0) > 0)
  const canUseFatalWeapon = computed(() => hasFatalWeapon.value && (currentRound.value === 2 || currentRound.value === 3))

  function setRoom(room: Room) {
    console.log('setRoom被调用:', room)
    currentRoom.value = room
  }

  function setCurrentPlayer(player: Player) {
    console.log('setCurrentPlayer被调用:', player)
    currentPlayer.value = player
  }

  function setConnected(connected: boolean) {
    isConnected.value = connected
  }

  function addMessage(message: GameMessage) {
    messages.value.push(message)
  }

  function clearMessages() {
    messages.value = []
  }

  function setNightResult(result: NightResult) {
    nightResult.value = result
  }

  function setGameOverData(data: GameOverData) {
    gameOverData.value = data
  }

  function setInputName(name: string) {
    inputName.value = name
  }

  function clearNightResult() {
    nightResult.value = null
  }

  function clearGameOverData() {
    gameOverData.value = null
  }

  function clearInputName() {
    inputName.value = ''
  }

  function reset() {
    currentRoom.value = null
    currentPlayer.value = null
    isConnected.value = false
    messages.value = []
    nightResult.value = null
    gameOverData.value = null
    inputName.value = ''
  }

  return {
    currentRoom,
    currentPlayer,
    isConnected,
    messages,
    nightResult,
    gameOverData,
    inputName,
    gamePhase,
    isGameStarted,
    isWaitingPhase,
    isGameOver,
    winner,
    currentRound,
    isHost,
    canReady,
    canStartGame,
    canVote,
    isNicknameRevealPhase,
    isRoleAnnouncementPhase,
    isRoomSelectionPhase,
    isSkillSelectionPhase,
    isNightResultPhase,
    isDiscussionPhase,
    isVotingPhase,
    isGameOverPhase,
    isWolf,
    isVillage,
    hasNormalWeapons,
    hasFatalWeapon,
    hasShields,
    canUseFatalWeapon,
    setRoom,
    setCurrentPlayer,
    setConnected,
    addMessage,
    clearMessages,
    setNightResult,
    setGameOverData,
    setInputName,
    clearNightResult,
    clearGameOverData,
    clearInputName,
    reset
  }
})
