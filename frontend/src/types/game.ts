export interface Player {
  id: string
  realName: string
  assignedName: string
  name: string
  seatNumber: number
  role: Role
  isAlive: boolean
  isReady: boolean
  normalWeapons: number
  fatalWeapon: number
  shields: number
  hasMadeSkillChoice: boolean
  lastRoom: number
  currentRoom: number
}

export interface Room {
  id: string
  name: string
  hostId: string
  players: Player[]
  maxPlayers: number
  currentRound: number
  gamePhase: GamePhase
  isGameOver: boolean
  winner?: string
}

export enum Role {
  WOLF_KING = 'WOLF_KING',
  WOLF = 'WOLF',
  VILLAGE_HEAD = 'VILLAGE_HEAD',
  VILLAGER = 'VILLAGER'
}

export enum Weapon {
  NORMAL = 'NORMAL',
  FATAL = 'FATAL'
}

export enum GamePhase {
  WAITING = 'WAITING',
  NICKNAME_REVEAL = 'NICKNAME_REVEAL',
  ROLE_ANNOUNCEMENT = 'ROLE_ANNOUNCEMENT',
  ROOM_SELECTION = 'ROOM_SELECTION',
  SKILL_SELECTION = 'SKILL_SELECTION',
  NIGHT_RESULT = 'NIGHT_RESULT',
  DISCUSSION = 'DISCUSSION',
  VOTING = 'VOTING',
  GAME_OVER = 'GAME_OVER'
}

export enum RoomType {
  HEAVEN = '0',
  EARTH = '1',
  HUMAN = '2'
}

export interface WeaponAction {
  targetId: string
  weaponType: Weapon
}

export interface ShieldAction {
  targetId: string
}

export interface VoteAction {
  targetId: string
}

export interface GameMessage {
  type: string
  data: any
  timestamp: number
}

export interface NightResult {
  room: Room
  deadPlayers: string[]
  roomPlayers: Record<string, string[]>
  round: number
}

export interface GameOverData {
  room: Room
  eliminatedId?: string
  voteCount?: Record<string, number>
  winner: string
}
