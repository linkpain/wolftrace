import { ref, onUnmounted } from 'vue'
import type { GameMessage } from '@/types/game'

const WS_URL = import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws'

const ws = ref<WebSocket | null>(null)
const isConnected = ref(false)
const messageHandlers = ref<Map<string, (data: any) => void>>(new Map())
let reconnectTimer: number | null = null
let reconnectAttempts = 0
const MAX_RECONNECT_ATTEMPTS = 5

export function useWebSocket() {
  function connect() {
    if (ws.value && (ws.value.readyState === WebSocket.OPEN || ws.value.readyState === WebSocket.CONNECTING)) {
      console.log('WebSocket已连接或正在连接')
      return
    }

    console.log('开始连接WebSocket...')
    ws.value = new WebSocket(WS_URL)

    ws.value.onopen = () => {
      console.log('WebSocket连接成功')
      isConnected.value = true
      reconnectAttempts = 0
      if (reconnectTimer) {
        clearTimeout(reconnectTimer)
        reconnectTimer = null
      }
    }

    ws.value.onmessage = (event) => {
      try {
        const message: GameMessage = JSON.parse(event.data)
        console.log('收到消息:', message.type, message.data)
        const handler = messageHandlers.value.get(message.type)
        if (handler) {
          handler(message.data)
        }
      } catch (error) {
        console.error('解析消息失败:', error)
      }
    }

    ws.value.onerror = (error) => {
      console.error('WebSocket错误:', error)
      isConnected.value = false
    }

    ws.value.onclose = () => {
      console.log('WebSocket连接关闭')
      isConnected.value = false
      ws.value = null
      
      if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
        reconnectAttempts++
        const delay = Math.min(1000 * Math.pow(2, reconnectAttempts), 10000)
        console.log(`尝试重连WebSocket (${reconnectAttempts}/${MAX_RECONNECT_ATTEMPTS})，延迟${delay}ms`)
        reconnectTimer = window.setTimeout(() => {
          connect()
        }, delay)
      } else {
        console.error('WebSocket重连失败，已达到最大重连次数')
      }
    }
  }

  function disconnect() {
    if (ws.value) {
      ws.value.close()
      ws.value = null
      isConnected.value = false
    }
  }

  function send(type: string, data: any) {
    console.log('send方法被调用 - type:', type, 'isConnected:', isConnected.value, 'ws:', ws.value)
    if (ws.value && isConnected.value) {
      const message: GameMessage = {
        type,
        data,
        timestamp: Date.now()
      }
      ws.value.send(JSON.stringify(message))
      console.log('发送消息:', type, data)
    } else {
      console.error('WebSocket未连接 - ws:', ws.value, 'isConnected:', isConnected.value, 'readyState:', ws.value?.readyState)
    }
  }

  function onMessage(type: string, handler: (data: any) => void) {
    messageHandlers.value.set(type, handler)
  }

  function offMessage(type: string) {
    messageHandlers.value.delete(type)
  }

  onUnmounted(() => {
    disconnect()
  })

  return {
    isConnected,
    connect,
    disconnect,
    send,
    onMessage,
    offMessage
  }
}
