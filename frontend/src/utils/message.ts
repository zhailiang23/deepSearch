/**
 * 消息提示工具
 */

export type MessageType = 'success' | 'error' | 'warning' | 'info'

interface MessageOptions {
  duration?: number
  showClose?: boolean
}

// 简单的消息提示实现
class MessageService {
  private container: HTMLElement | null = null

  private ensureContainer() {
    if (!this.container) {
      this.container = document.createElement('div')
      this.container.id = 'message-container'
      this.container.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        z-index: 9999;
        pointer-events: none;
      `
      document.body.appendChild(this.container)
    }
  }

  private show(message: string, type: MessageType, options: MessageOptions = {}) {
    this.ensureContainer()

    const { duration = 3000, showClose = true } = options

    const messageEl = document.createElement('div')
    messageEl.style.cssText = `
      margin-bottom: 12px;
      padding: 12px 16px;
      border-radius: 6px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      pointer-events: auto;
      transform: translateX(100%);
      transition: transform 0.3s ease;
      max-width: 300px;
      word-wrap: break-word;
    `

    // 设置不同类型的样式
    switch (type) {
      case 'success':
        messageEl.style.backgroundColor = '#f0f9ff'
        messageEl.style.color = '#059669'
        messageEl.style.border = '1px solid #10b981'
        break
      case 'error':
        messageEl.style.backgroundColor = '#fef2f2'
        messageEl.style.color = '#dc2626'
        messageEl.style.border = '1px solid #f87171'
        break
      case 'warning':
        messageEl.style.backgroundColor = '#fffbeb'
        messageEl.style.color = '#d97706'
        messageEl.style.border = '1px solid #fbbf24'
        break
      case 'info':
        messageEl.style.backgroundColor = '#eff6ff'
        messageEl.style.color = '#2563eb'
        messageEl.style.border = '1px solid #60a5fa'
        break
    }

    messageEl.innerHTML = `
      <div style="display: flex; align-items: center; justify-content: space-between;">
        <span>${message}</span>
        ${showClose ? '<button style="margin-left: 12px; background: none; border: none; font-size: 16px; cursor: pointer; opacity: 0.6; hover: opacity: 1;">&times;</button>' : ''}
      </div>
    `

    // 添加关闭按钮事件
    if (showClose) {
      const closeBtn = messageEl.querySelector('button')
      if (closeBtn) {
        closeBtn.onclick = () => this.remove(messageEl)
      }
    }

    this.container!.appendChild(messageEl)

    // 动画显示
    requestAnimationFrame(() => {
      messageEl.style.transform = 'translateX(0)'
    })

    // 自动关闭
    if (duration > 0) {
      setTimeout(() => {
        this.remove(messageEl)
      }, duration)
    }

    return messageEl
  }

  private remove(messageEl: HTMLElement) {
    messageEl.style.transform = 'translateX(100%)'
    setTimeout(() => {
      if (messageEl.parentNode) {
        messageEl.parentNode.removeChild(messageEl)
      }
    }, 300)
  }

  success(message: string, options?: MessageOptions) {
    return this.show(message, 'success', options)
  }

  error(message: string, options?: MessageOptions) {
    return this.show(message, 'error', options)
  }

  warning(message: string, options?: MessageOptions) {
    return this.show(message, 'warning', options)
  }

  info(message: string, options?: MessageOptions) {
    return this.show(message, 'info', options)
  }
}

export const message = new MessageService()