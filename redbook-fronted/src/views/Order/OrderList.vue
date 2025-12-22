<template>
  <div class="order-list-container">
    <!-- 状态 Tab 页 -->
    <el-tabs v-model="activeTab" class="order-tabs">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="待付款" name="0" />
      <el-tab-pane label="待发货" name="1" />
      <el-tab-pane label="待收货" name="3" />
      <el-tab-pane label="已完成" name="4" />
      <el-tab-pane label="已取消" name="2" />
    </el-tabs>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchForm.keyword"
        placeholder="搜索商品名称"
        prefix-icon="Search"
        clearable
        @clear="handleSearch"
        @keyup.enter="handleSearch"
        style="width: 300px; margin-right: 10px;"
      />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <!-- 订单列表（卡片式） -->
    <div v-loading="loading" class="order-card-list">
      <el-empty v-if="!tableData.length && !loading" description="暂无订单" />
      
      <div v-for="order in tableData" :key="order.id" class="order-card">
        <div class="order-card-header">
          <div class="shop-info">
            <img :src="order.storeAvatar || 'https://sns-avatar-qc.xhscdn.com/avatar/63e6040851410e3093259166.jpg'" class="shop-avatar" />
            <span class="shop-name">{{ order.storeName || '小红书自营旗舰店' }}</span>
            <el-icon class="shop-arrow"><ArrowRight /></el-icon>
          </div>
          <div class="order-status" :class="getStatusColorClass(order.status)">
            {{ getStatusText(order.status) }}
          </div>
        </div>
        
        <div class="order-card-body" @click="handleDetail(order)">
          <el-image :src="getImageUrl(order.productImage)" class="product-image" fit="cover" />
          <div class="product-info">
            <div class="product-name">{{ order.productName }}</div>
            <div class="product-specs">规格: {{ formatSkuSpec(order.skuSpec) || '默认规格' }}</div>
          </div>
          <div class="price-info">
            <div class="price">¥{{ order.skuPrice || (order.finalPrice / order.quantity).toFixed(2) }}</div>
            <div class="quantity">x{{ order.quantity }}</div>
          </div>
        </div>
        
        <div class="order-card-footer">
          <div class="footer-left">
             <span class="order-time">{{ order.createTime }}</span>
             <transition name="countdown-fade">
               <span v-if="order.status === 0" class="order-countdown">{{ getCountdownText(order) }}</span>
             </transition>
          </div>
          <div class="action-buttons">
            <div class="total-price-inline">
                实付 <span class="price-num">¥{{ order.finalPrice }}</span>
            </div>
            <el-button 
              v-if="order.status === 0" 
              type="primary" 
              plain 
              size="small" 
              round
              :loading="payingOrderId === order.id"
              @click.stop="openPayDialog(order)"
            >去支付</el-button>
            <el-button 
              v-if="order.status === 0" 
              size="small" 
              round
              @click.stop="handleStatus(order, 2)"
            >取消订单</el-button>
            <el-button 
              v-if="order.status === 3" 
              type="primary" 
              plain 
              size="small" 
              round
              @click.stop="handleStatus(order, 4)"
            >确认收货</el-button>
            <el-button 
              v-if="order.status === 4 || order.status === 2" 
              size="small" 
              round
              @click.stop="handleDetail(order)"
            >查看详情</el-button>
             <el-button 
              v-if="order.status === 4" 
              size="small" 
              round
              @click.stop="handleReview(order)"
            >评价晒单</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="pagination-container">
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        layout="prev, pager, next"
        :total="total"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
      <div v-if="currentOrder">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单ID">{{ currentOrder.id }}</el-descriptions-item>
          <el-descriptions-item label="状态">
             <el-tag :type="getStatusType(currentOrder.status)">{{ getStatusText(currentOrder.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="商品名称" :span="2">{{ currentOrder.productName }}</el-descriptions-item>
          <el-descriptions-item label="商品规格" :span="2">{{ formatSkuSpec(currentOrder.skuSpec) || '默认规格' }}</el-descriptions-item>
          <el-descriptions-item label="商品图片" :span="2">
            <el-image :src="getImageUrl(currentOrder.productImage)" style="width: 100px; height: 100px" />
          </el-descriptions-item>
          <el-descriptions-item label="单价">¥{{ currentOrder.finalPrice / currentOrder.quantity }}</el-descriptions-item>
          <el-descriptions-item label="数量">{{ currentOrder.quantity }}</el-descriptions-item>
          <el-descriptions-item label="总价">¥{{ currentOrder.finalPrice }}</el-descriptions-item>
          <el-descriptions-item label="下单用户" :span="2">
            <div class="buyer-inline buyer-clickable" @click="goToBuyerProfile(currentOrder.userId)">
              <el-image
                :src="getImageUrl(currentOrder.buyerAvatar, defaultAvatar)"
                class="buyer-avatar"
              />
              <span class="buyer-name">{{ currentOrder.buyerName || ('用户' + currentOrder.userId) }}</span>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ currentOrder.createTime }}</el-descriptions-item>
          <el-descriptions-item label="支付时间" :span="2">{{ currentOrder.payTime || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <el-dialog
      v-model="payVisible"
      title="模拟支付"
      width="420px"
      :close-on-click-modal="false"
      :close-on-press-escape="!paying"
      :show-close="!paying"
      @closed="onPayDialogClosed"
    >
      <div v-if="payingOrder" v-loading="paying" element-loading-text="支付处理中...">
        <div style="display: flex; justify-content: space-between; margin-bottom: 12px;">
          <span>订单ID：{{ payingOrder.id }}</span>
          <span>支付金额：¥{{ payingOrder.finalPrice }}</span>
        </div>
        <div style="color: #666;">正在模拟支付，预计 3 秒完成...</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, computed, watch } from 'vue';
import { searchOrders, updateOrderStatus, getOrderDetail, getPaymentTimeoutSeconds, getServerTime, timeoutCancelOrder, type OrderVo, type OrderSearchDto } from '@/api/order';
import { ElMessage } from 'element-plus';
import { getImageUrl } from '@/utils/image';
import { useOrderStore } from '@/store/order';
import { useUserStore } from '@/store/user';
import { useRouter } from 'vue-router'; // Add router for buy again

const router = useRouter();
const loading = ref(false);
const tableData = ref<OrderVo[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const activeTab = ref('all');

const searchForm = reactive<Partial<OrderSearchDto>>({
  keyword: undefined,
  status: undefined
});

const detailVisible = ref(false);
const currentOrder = ref<OrderVo | null>(null);

const orderStore = useOrderStore();
const userStore = useUserStore();
const isMerchant = computed(() => (userStore.userInfo as any)?.role === 1);
// 默认头像（买家头像缺失时使用）
const defaultAvatar = 'https://via.placeholder.com/32x32/ff2442/ffffff?text=U';

// 模拟支付弹窗状态
const payVisible = ref(false);
const paying = ref(false);
const payingOrderId = ref<number | null>(null);
const payingOrder = ref<OrderVo | null>(null);
let payCloseTimer: ReturnType<typeof window.setTimeout> | null = null;

// 订单支付超时配置（秒，后端可配置，默认 120 秒）
const paymentTimeoutSeconds = ref<number>(120);
const serverTimeOffsetMs = ref<number>(0);
const remainingMsByOrderId = ref<Record<string, number>>({});
const online = ref<boolean>(navigator.onLine);
let countdownTimer: ReturnType<typeof window.setInterval> | null = null;
let timeoutSyncTimer: ReturnType<typeof window.setInterval> | null = null;
const cancelingByTimeout = ref<Record<string, boolean>>({});
let countdownTicking = false;
let timeoutSyncRunning = false;

const formatDateTime = (date: Date) => {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
};

const parseOrderCreateTime = (time?: string): Date | null => {
  if (!time) return null;
  try {
    const normalized = time.indexOf('T') === -1 ? time.replace(' ', 'T') : time;
    const date = new Date(normalized);
    if (!isNaN(date.getTime())) return date;
    const fallback = new Date(time.replace(/-/g, '/'));
    return isNaN(fallback.getTime()) ? null : fallback;
  } catch {
    return null;
  }
};

const formatRemaining = (ms: number) => {
  const totalSeconds = Math.max(0, Math.floor(ms / 1000));
  const hh = Math.floor(totalSeconds / 3600);
  const mm = Math.floor((totalSeconds % 3600) / 60);
  const ss = totalSeconds % 60;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(hh)}:${pad(mm)}:${pad(ss)}`;
};

const resolvedNowMs = () => Date.now() + serverTimeOffsetMs.value;

const computeRemainingMs = (order: OrderVo) => {
  if (!order.id) return 0;
  if (order.status !== 0) return 0;
  const createDate = parseOrderCreateTime(order.createTime);
  if (!createDate) return 0;
  const timeoutAtMs = createDate.getTime() + paymentTimeoutSeconds.value * 1000;
  return Math.max(0, timeoutAtMs - resolvedNowMs());
};

const refreshCountdownSnapshot = () => {
  const next: Record<string, number> = {};
  for (const order of tableData.value) {
    if (!order.id) continue;
    if (order.status !== 0) continue;
    next[String(order.id)] = computeRemainingMs(order);
  }
  remainingMsByOrderId.value = next;
};

const getCountdownText = (order: OrderVo) => {
  if (!order.id) return '剩余时间：--:--:--';
  const ms = remainingMsByOrderId.value[String(order.id)];
  if (typeof ms !== 'number') return '剩余时间：--:--:--';
  return `剩余时间：${formatRemaining(ms)}`;
};

const syncServerTime = async () => {
  try {
    const serverMs: any = await getServerTime();
    const parsed = Number(serverMs);
    if (Number.isFinite(parsed) && parsed > 0) {
      serverTimeOffsetMs.value = parsed - Date.now();
    }
  } catch {
    // 时间同步失败时继续使用本地时间
  }
};

const loadTimeoutConfig = async () => {
  try {
    const seconds: any = await getPaymentTimeoutSeconds();
    const parsed = Number(seconds);
    if (Number.isFinite(parsed) && parsed > 0) {
      paymentTimeoutSeconds.value = parsed;
    }
  } catch {
    // 获取配置失败时继续使用默认值
  }
};

const applyTimeoutCancelToUI = (orderId: number) => {
  const listItem = tableData.value.find(o => o.id === orderId);
  if (listItem) listItem.status = 2;
  if (currentOrder.value && currentOrder.value.id === orderId) currentOrder.value.status = 2;
};

const cancelByTimeoutIfNeeded = async (orderId: number, options?: { toast?: boolean }) => {
  const key = String(orderId);
  if (cancelingByTimeout.value[key]) return;
  cancelingByTimeout.value = { ...cancelingByTimeout.value, [key]: true };
  try {
    await timeoutCancelOrder(orderId);
    applyTimeoutCancelToUI(orderId);
    if (options?.toast) ElMessage.warning('订单已超时自动取消');
    await loadData();
  } catch {
    // 取消失败可能是未超时/状态已变化，交给后续刷新兜底
  } finally {
    const { [key]: _, ...rest } = cancelingByTimeout.value;
    cancelingByTimeout.value = rest;
  }
};

const cancelOverdueOrdersOnce = async (options?: { toast?: boolean }) => {
  if (!online.value) return;
  const pendingOrders = tableData.value.filter(o => o.status === 0 && !!o.id);
  if (pendingOrders.length === 0) return;
  for (const order of pendingOrders) {
    if (!order.id) continue;
    const remaining = computeRemainingMs(order);
    if (remaining <= 0) {
      await cancelByTimeoutIfNeeded(order.id, { toast: options?.toast ?? false });
    }
  }
};

const formatSkuSpec = (spec?: string) => {
  // 后端返回示例：color:幻影紫 / storage:12GB+512GB，前端展示仅保留值：幻影紫 / 12GB+512GB
  if (!spec) return '';
  return spec
    .split(' / ')
    .map(part => part.replace(/^[^:]+:\s*/, '').trim())
    .filter(Boolean)
    .join(' / ');
};

const applyLocalStateToOrder = (order: OrderVo): OrderVo => {
  const id = order.id;
  if (!id) return order;
  const overrideStatus = orderStore.getStatusOverride(id);
  const paidAt = orderStore.getPaidAt(id);
  const baseStatus = typeof order.status === 'number' ? order.status : Number(order.status);
  const effectiveStatus = overrideStatus == null
    ? baseStatus
    : (Number.isFinite(baseStatus) ? Math.max(baseStatus, overrideStatus) : overrideStatus);
  return {
    ...order,
    status: effectiveStatus,
    payTime: paidAt ?? order.payTime
  };
};

const goToBuyerProfile = (buyerId?: number) => {
  if (!buyerId) return;
  detailVisible.value = false;
  router.push(`/user/${buyerId}`);
};

const loadData = async () => {
  loading.value = true;
  try {
    const params: OrderSearchDto = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      ...searchForm
    };
    
    // 根据 Tab 映射状态
    if (activeTab.value !== 'all') {
      params.status = Number(activeTab.value);
    } else {
      params.status = undefined; // 显式设置为 undefined，确保不传 status 参数以搜索所有订单
    }

    // 确保 status 是有效的数字或 undefined
    if (params.status !== undefined && isNaN(params.status)) {
        params.status = undefined;
    }

    const res: any = await searchOrders(params);
    if (res.list) {
      tableData.value = res.list.map((o: OrderVo) => applyLocalStateToOrder(o));
      total.value = Number(res.total) || 0;
    } else if (Array.isArray(res)) {
      tableData.value = res.map((o: OrderVo) => applyLocalStateToOrder(o));
      total.value = res.length;
    }
    refreshCountdownSnapshot();
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
};

const handleOnline = async () => {
  online.value = true;
  await loadTimeoutConfig();
  await syncServerTime();
  refreshCountdownSnapshot();
  await cancelOverdueOrdersOnce({ toast: false });
};

const handleOffline = () => {
  online.value = false;
};

onMounted(async () => {
  window.addEventListener('online', handleOnline);
  window.addEventListener('offline', handleOffline);
  await loadTimeoutConfig();
  await syncServerTime();
  await loadData();
  if (!countdownTimer) {
    countdownTimer = window.setInterval(async () => {
      if (countdownTicking) return;
      countdownTicking = true;
      refreshCountdownSnapshot();
      try {
        for (const order of tableData.value) {
          if (!order.id) continue;
          if (order.status !== 0) continue;
          const ms = remainingMsByOrderId.value[String(order.id)];
          if (typeof ms === 'number' && ms <= 0) {
            await cancelByTimeoutIfNeeded(order.id, { toast: true });
          }
        }
      } finally {
        countdownTicking = false;
      }
    }, 1000);
  }
  if (!timeoutSyncTimer) {
    timeoutSyncTimer = window.setInterval(() => {
      if (!online.value) return;
      if (timeoutSyncRunning) return;
      timeoutSyncRunning = true;
      Promise.allSettled([loadTimeoutConfig(), syncServerTime()])
        .finally(() => {
          timeoutSyncRunning = false;
        });
    }, 15000);
  }
});

watch(activeTab, () => {
  currentPage.value = 1;
  loadData();
});

const handleSearch = () => {
  currentPage.value = 1;
  loadData();
};

const handleCurrentChange = (val: number) => {
  currentPage.value = val;
  loadData();
};

const handleDetail = async (row: OrderVo) => {
  try {
    const res = await getOrderDetail(row.id!);
    currentOrder.value = res ? applyLocalStateToOrder(res as any) : null;
    detailVisible.value = true;
  } catch (e) {
    console.error(e);
  }
};

const onPayDialogClosed = () => {
  paying.value = false;
  payingOrderId.value = null;
  payingOrder.value = null;
  if (payCloseTimer) {
    window.clearTimeout(payCloseTimer);
    payCloseTimer = null;
  }
};

const openPayDialog = async (row: OrderVo) => {
  if (!row.id) return;
  if (paying.value) return;
  if (!online.value) {
    ElMessage.warning('当前网络不可用，无法支付');
    return;
  }

  await syncServerTime();

  const localRemaining = remainingMsByOrderId.value[String(row.id)];
  if (typeof localRemaining === 'number' && localRemaining <= 0) {
    await cancelByTimeoutIfNeeded(row.id, { toast: true });
    return;
  }

  payingOrder.value = row;
  payVisible.value = true;
  paying.value = true;
  payingOrderId.value = row.id;

  const paidAt = formatDateTime(new Date());
  const delay3s = new Promise<void>((resolve) => {
    if (payCloseTimer) window.clearTimeout(payCloseTimer);
    payCloseTimer = window.setTimeout(() => resolve(), 3000);
  });

  try {
    // 调用后端支付接口
    await Promise.all([orderStore.payOrder(row.id, paidAt), delay3s]);

    const listItem = tableData.value.find(o => o.id === row.id);
    if (listItem) {
      listItem.status = 1;
      listItem.payTime = paidAt;
    }
    if (currentOrder.value && currentOrder.value.id === row.id) {
      currentOrder.value.status = 1;
      currentOrder.value.payTime = paidAt;
    }

    paying.value = false;
    ElMessage.success('支付成功');
    payVisible.value = false;
    loadData();
  } catch (e: any) {
    paying.value = false;
    payingOrderId.value = null;
    if (payCloseTimer) {
      window.clearTimeout(payCloseTimer);
      payCloseTimer = null;
    }
    if (!e?.isHandled) {
      ElMessage.error('支付失败');
    }
  }
};

const handleStatus = async (row: OrderVo, status: number) => {
  try {
    await updateOrderStatus(row.id!, status);
    ElMessage.success('操作成功');
    loadData();
  } catch (e) {
    ElMessage.error('操作失败');
  }
};

const handleReview = (order: OrderVo) => {
  if (!order?.id) return;
  ElMessage.info('评价功能开发中');
};

const buyAgain = (order: OrderVo) => {
    // 跳转到商品详情页
    if (order.productId) {
        router.push(`/product/${order.productId}`);
    } else {
        ElMessage.warning('商品信息缺失');
    }
}

const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '待付款',
    1: '待发货', // 修正：已付款在C端通常显示为待发货
    2: '已取消',
    3: '待收货', // 修正：已发货在C端通常显示为待收货
    4: '已完成'
  };
  return map[status] || '未知';
};

const getStatusType = (status: number) => {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'primary',
    2: 'info',
    3: 'success',
    4: 'success'
  };
  return map[status] || '';
};

const getStatusColorClass = (status: number) => {
   const map: Record<number, string> = {
    0: 'status-pending',
    1: 'status-paid',
    2: 'status-cancelled',
    3: 'status-shipped',
    4: 'status-completed'
  };
  return map[status] || '';
}

onBeforeUnmount(() => {
  if (payCloseTimer) window.clearTimeout(payCloseTimer);
  if (countdownTimer) {
    window.clearInterval(countdownTimer);
    countdownTimer = null;
  }
  if (timeoutSyncTimer) {
    window.clearInterval(timeoutSyncTimer);
    timeoutSyncTimer = null;
  }
  window.removeEventListener('online', handleOnline);
  window.removeEventListener('offline', handleOffline);
});
</script>

<style scoped>
.order-list-container {
  padding: 10px 20px;
  background-color: #f8f8f8;
  min-height: 100vh;
}

.order-tabs {
  background: #fff;
  padding: 0 20px;
  border-radius: 8px;
  margin-bottom: 15px;
}

.search-bar {
  margin-bottom: 15px;
  background: #fff;
  padding: 15px;
  border-radius: 8px;
  display: flex;
  align-items: center;
}

.order-card-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.order-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
  transition: all 0.2s;
}
.order-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.order-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 14px;
}
.shop-name {
  font-weight: 500;
  color: #333;
  margin-right: 4px;
  display: flex;
  align-items: center;
}
.shop-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  margin-right: 6px;
}
.shop-arrow {
  font-size: 12px;
  color: #999;
}
.order-time {
  color: #999;
  font-size: 12px;
}
.order-countdown {
  margin-left: 10px;
  color: #ff2442;
  font-size: 12px;
  font-weight: 500;
}
.countdown-fade-enter-active,
.countdown-fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.countdown-fade-enter-from,
.countdown-fade-leave-to {
  opacity: 0;
  transform: translateY(-2px);
}
.order-status {
  font-weight: 500;
}
.status-pending { color: #ff2442; }
.status-paid { color: #ff9a00; }
.status-shipped { color: #409eff; }
.status-completed { color: #333; }
.status-cancelled { color: #999; }

.order-card-body {
  display: flex;
  margin-bottom: 16px;
  cursor: pointer;
}
.product-image {
  width: 90px;
  height: 90px;
  border-radius: 8px;
  margin-right: 12px;
  background-color: #f5f5f5;
}
.product-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.product-name {
  font-size: 15px;
  color: #333;
  line-height: 1.4;
  margin-bottom: 6px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-weight: 500;
}
.product-specs {
  font-size: 12px;
  color: #666;
  background: #f7f7f7;
  padding: 2px 6px;
  border-radius: 4px;
  align-self: flex-start;
  margin-top: 4px;
}
.price-info {
  text-align: right;
  margin-left: 10px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}
.price {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}
.quantity {
  font-size: 12px;
  color: #999;
}

.order-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}
.footer-left {
  flex: 1;
}
.action-buttons {
    display: flex;
    align-items: center;
}
.total-price-inline {
  font-size: 13px;
  color: #333;
  margin-right: 12px;
}
.price-num {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}
.action-buttons .el-button {
  margin-left: 8px;
  font-weight: 500;
  border-radius: 20px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.buyer-inline {
  display: flex;
  align-items: center;
  gap: 10px;
}
.buyer-clickable {
  cursor: pointer;
}
.buyer-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  flex-shrink: 0;
}
.buyer-name {
  color: #333;
}
</style>
