<template>
  <transition name="modal-fade">
    <div v-if="state.visible" class="modal-overlay" @click="handleOverlayClick">
      <div class="modal-content" @click.stop>
        <div class="modal-header" v-if="state.title">
          <h3>{{ state.title }}</h3>
          <button class="close-btn" @click="handleCancel">×</button>
        </div>
        <div class="modal-body">
          <p>{{ state.message }}</p>
        </div>
        <div class="modal-footer">
          <button 
            v-if="state.type === 'confirm'" 
            class="cancel-btn" 
            @click="handleCancel"
          >
            {{ state.cancelText }}
          </button>
          <button 
            class="confirm-btn" 
            @click="handleConfirm"
          >
            {{ state.confirmText }}
          </button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { useModal } from '@/utils/modal'

const { state, hideModal } = useModal()

const handleConfirm = () => {
  if (state.onConfirm) {
    state.onConfirm()
  } else {
    hideModal()
  }
}

const handleCancel = () => {
  if (state.onCancel) {
    state.onCancel()
  } else {
    hideModal()
  }
}

const handleOverlayClick = () => {
  if (state.type === 'alert') {
    handleConfirm()
  } else {
    handleCancel()
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.modal-content {
  background: white;
  border-radius: 16px;
  width: 85%;
  max-width: 320px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  animation: modal-zoom 0.3s cubic-bezier(0.18, 0.89, 0.32, 1.28);
}

.modal-header {
  padding: 20px 20px 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

.close-btn:hover {
  color: #666;
}

.modal-body {
  padding: 10px 20px 20px;
  font-size: 15px;
  color: #666;
  line-height: 1.5;
  text-align: center;
}

.modal-footer {
  padding: 0 20px 20px;
  display: flex;
  justify-content: center;
  gap: 12px;
}

.confirm-btn, .cancel-btn {
  flex: 1;
  padding: 10px 0;
  border-radius: 22px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.confirm-btn {
  background: #ff2442;
  color: white;
}

.confirm-btn:hover {
  background: #e01e3a;
}

.cancel-btn {
  background: #f5f5f5;
  color: #666;
}

.cancel-btn:hover {
  background: #e5e5e5;
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.3s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

@keyframes modal-zoom {
  0% {
    transform: scale(0.8);
    opacity: 0;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}
</style>
