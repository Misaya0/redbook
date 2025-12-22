import { defineStore } from 'pinia'
import { ref } from 'vue'
import { buyOrder } from '@/api/order'

type PersistedOrderState = {
  paidAtByOrderId?: Record<string, string>
  statusByOrderId?: Record<string, number>
}

// 模拟支付信息本地持久化（仅用于前端展示支付时间与即时状态回显）
const STORAGE_KEY = 'rb:order:simPay'

const safeParse = (raw: string | null): PersistedOrderState => {
  if (!raw) return {}
  try {
    return JSON.parse(raw) as PersistedOrderState
  } catch {
    return {}
  }
}

const readPersisted = (): PersistedOrderState => safeParse(localStorage.getItem(STORAGE_KEY))

const writePersisted = (state: PersistedOrderState) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
}

export const useOrderStore = defineStore('order', () => {
  const persisted = readPersisted()

  const paidAtByOrderId = ref<Record<string, string>>(persisted.paidAtByOrderId || {})
  const statusByOrderId = ref<Record<string, number>>(persisted.statusByOrderId || {})

  const getPaidAt = (orderId: number) => paidAtByOrderId.value[String(orderId)]
  const getStatusOverride = (orderId: number) => statusByOrderId.value[String(orderId)]

  const setPaidAt = (orderId: number, paidAt: string) => {
    paidAtByOrderId.value = { ...paidAtByOrderId.value, [String(orderId)]: paidAt }
    writePersisted({ paidAtByOrderId: paidAtByOrderId.value, statusByOrderId: statusByOrderId.value })
  }

  const setStatusOverride = (orderId: number, status: number) => {
    statusByOrderId.value = { ...statusByOrderId.value, [String(orderId)]: status }
    writePersisted({ paidAtByOrderId: paidAtByOrderId.value, statusByOrderId: statusByOrderId.value })
  }

  // 调用后端 buyOrder 完成状态流转，并记录本次模拟支付时间
  const payOrder = async (orderId: number, paidAt: string) => {
    await buyOrder(orderId)
    setStatusOverride(orderId, 1)
    setPaidAt(orderId, paidAt)
  }

  return {
    paidAtByOrderId,
    statusByOrderId,
    getPaidAt,
    getStatusOverride,
    setPaidAt,
    setStatusOverride,
    payOrder
  }
})
