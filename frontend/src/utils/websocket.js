class WebSocketClient {
  constructor() {
    this.ws = null
    this.reconnectTimer = null
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectDelay = 3000
    this.handlers = {}
    this.messageQueue = []
    this.isConnecting = false
  }

  connect(handlers = {}) {
    if (this.ws?.readyState === WebSocket.OPEN) return
    if (this.isConnecting) return

    this.handlers = handlers
    this.isConnecting = true

    const token = localStorage.getItem('token')
    if (!token) {
      this.isConnecting = false
      return
    }

    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
    const url = `${protocol}//${location.host}/ws/chat?token=${token}`

    try {
      this.ws = new WebSocket(url)

      this.ws.onopen = () => {
        this.isConnecting = false
        this.reconnectAttempts = 0
        this.handlers.onOpen?.()
        this.flushQueue()
      }

      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          this.handlers.onMessage?.(data)
        } catch (e) {
          console.error('WS message parse error:', e)
        }
      }

      this.ws.onerror = (err) => {
        this.isConnecting = false
        this.handlers.onError?.(err)
      }

      this.ws.onclose = (event) => {
        this.isConnecting = false
        this.handlers.onClose?.(event)
        if (!event.wasClean && this.reconnectAttempts < this.maxReconnectAttempts) {
          this.scheduleReconnect()
        }
      }
    } catch (e) {
      this.isConnecting = false
      console.error('WS connect error:', e)
    }
  }

  scheduleReconnect() {
    const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts)
    this.reconnectAttempts++
    this.reconnectTimer = setTimeout(() => {
      this.connect(this.handlers)
    }, Math.min(delay, 30000))
  }

  send(data) {
    const msg = JSON.stringify(data)
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(msg)
    } else {
      this.messageQueue.push(msg)
    }
  }

  flushQueue() {
    while (this.messageQueue.length > 0) {
      const msg = this.messageQueue.shift()
      if (this.ws?.readyState === WebSocket.OPEN) {
        this.ws.send(msg)
      }
    }
  }

  disconnect() {
    clearTimeout(this.reconnectTimer)
    this.reconnectAttempts = this.maxReconnectAttempts
    if (this.ws) {
      this.ws.close(1000, 'User logout')
      this.ws = null
    }
    this.messageQueue = []
    this.isConnecting = false
  }

  isConnected() {
    return this.ws?.readyState === WebSocket.OPEN
  }
}

export default new WebSocketClient()
