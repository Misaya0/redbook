import { reactive } from 'vue'

const state = reactive({
  visible: false,
  type: 'alert', // 'alert' or 'confirm'
  title: '',
  message: '',
  confirmText: '确定',
  cancelText: '取消',
  onConfirm: null,
  onCancel: null
})

export const useModal = () => {
  const showModal = ({ type = 'alert', title = '', message = '', confirmText = '确定', cancelText = '取消', onConfirm, onCancel }) => {
    state.type = type
    state.title = title
    state.message = message
    state.confirmText = confirmText
    state.cancelText = cancelText
    state.onConfirm = onConfirm
    state.onCancel = onCancel
    state.visible = true
  }

  const hideModal = () => {
    state.visible = false
    state.onConfirm = null
    state.onCancel = null
  }

  const showAlert = (message, title = '提示') => {
    return new Promise((resolve) => {
      showModal({
        type: 'alert',
        title,
        message,
        onConfirm: () => {
          hideModal()
          resolve()
        }
      })
    })
  }

  const showConfirm = (message, title = '确认') => {
    return new Promise((resolve, reject) => {
      showModal({
        type: 'confirm',
        title,
        message,
        onConfirm: () => {
          hideModal()
          resolve(true)
        },
        onCancel: () => {
          hideModal()
          resolve(false)
        }
      })
    })
  }

  return {
    state,
    showModal,
    hideModal,
    showAlert,
    showConfirm
  }
}
