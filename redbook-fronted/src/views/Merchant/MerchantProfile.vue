<template>
  <div class="merchant-profile">
    <div class="header-bg"></div>

    <div class="content-container">
      <!-- 顶部用户信息 -->
      <div class="user-card">
        <div class="avatar-wrapper">
          <img :src="getImageUrl(userStore.userInfo?.image)" class="avatar"/>
        </div>
        <div class="user-info">
          <h2 class="nickname">
            {{ userStore.userInfo?.nickname }}
            <span class="merchant-badge">商家</span>
          </h2>
          <p class="user-id">ID: {{ userStore.userInfo?.number }}</p>

          <!-- 社交数据统计 -->
          <div class="stats-row">
            <div class="stat-item">
              <span class="count">{{ stats.attentionCount || 0 }}</span>
              <span class="label">关注</span>
            </div>
            <div class="stat-item">
              <span class="count">{{ stats.fansCount || 0 }}</span>
              <span class="label">粉丝</span>
            </div>
            <div class="stat-item">
              <span class="count">{{ stats.likeCollectCount || 0 }}</span>
              <span class="label">获赞与收藏</span>
            </div>
          </div>

          <!-- 社交互动按钮 -->
          <div class="social-actions" v-if="isSelf">
            <el-button round size="small">编辑资料</el-button>
            <el-button round size="small" :icon="Setting">设置</el-button>
          </div>
          <div class="social-actions" v-else>
            <el-button
                type="primary"
                round
                size="small"
                :class="{ 'followed': isFollowed }"
                @click="handleFollow"
                :loading="followLoading"
            >
              {{ isFollowed ? '已关注' : '关注' }}
            </el-button>
            <el-button round size="small" @click="handleChat">私信</el-button>
          </div>
        </div>
      </div>

      <!-- 标签页切换 -->
      <div class="tabs-container" v-if="shop">
        <div
            class="tab-item"
            :class="{ active: currentTab === 'dashboard' }"
            @click="currentTab = 'dashboard'"
            v-if="isSelf"
        >
          店铺经营
        </div>
        <div
            class="tab-item"
            :class="{ active: currentTab === 'products' }"
            @click="currentTab = 'products'"
            v-if="!isSelf"
        >
          商品
        </div>
        <div
            class="tab-item"
            :class="{ active: currentTab === 'notes' }"
            @click="currentTab = 'notes'"
        >
          笔记
        </div>
        <div
            class="tab-item"
            :class="{ active: currentTab === 'collection' }"
            @click="currentTab = 'collection'"
        >
          收藏
        </div>
      </div>

      <!-- 内容展示区域 -->
      <div class="shop-section">
        <div v-if="loading" class="loading-state">
          <div class="spinner"></div>
          <p>加载店铺信息...</p>
        </div>

        <!-- Tab 1: 店铺经营 (原内容) -->
        <div v-else-if="shop && currentTab === 'dashboard'" class="shop-dashboard">
          <div class="shop-header">
            <div class="shop-basic">
              <img :src="getImageUrl(shop.image)" class="shop-logo"/>
              <div class="shop-text">
                <h3 class="shop-name">{{ shop.name }}</h3>
                <p class="shop-desc">成立时间: {{ shop.time }}</p>
              </div>
            </div>
            <el-button type="primary" plain size="small" @click="$router.push('/merchant/products')">进入商品管理
            </el-button>
          </div>

          <!-- 数据仪表盘 -->
          <div class="dashboard-grid">
            <div class="data-card">
              <div class="data-label">商品总数</div>
              <div class="data-value">{{ dashboard.totalProducts || 0 }}</div>
            </div>
            <div class="data-card">
              <div class="data-label">今日浏览</div>
              <div class="data-value highlight">{{ dashboard.todayViews || 0 }}</div>
            </div>
            <div class="data-card">
              <div class="data-label">今日订单</div>
              <div class="data-value highlight">{{ dashboard.todayOrders || 0 }}</div>
            </div>
            <div class="data-card">
              <div class="data-label">本月成交</div>
              <div class="data-value">{{ dashboard.monthOrders || 0 }}</div>
            </div>
          </div>

          <!-- 快捷入口 -->
          <div class="quick-actions">
            <div class="action-item" @click="$router.push('/merchant/products')">
              <span class="icon">📦</span>
              <span>商品管理</span>
            </div>
            <div class="action-item" @click="$router.push('/orders')">
              <span class="icon">📄</span>
              <span>订单管理</span>
            </div>
            <div class="action-item">
              <span class="icon">💰</span>
              <span>促销活动</span>
            </div>
            <div class="action-item">
              <span class="icon">⚙️</span>
              <span>店铺设置</span>
            </div>
          </div>
        </div>

        <!-- Tab 2: 笔记列表 -->
        <div v-else-if="currentTab === 'notes'" class="notes-list-container">
          <div v-if="notesLoading" class="loading-state">
            <div class="spinner"></div>
          </div>
          <div v-else-if="notesList.length === 0" class="empty-state">
            <div class="empty-icon">📝</div>
            <p>暂无笔记</p>
          </div>
          <div v-else class="note-grid">
            <PostCard
                v-for="note in notesList"
                :key="note.id"
                :post="note"
                @click="handleNoteClick(note)"
            />
          </div>
        </div>

        <!-- Tab 3: 收藏列表 -->
        <div v-else-if="currentTab === 'collection'" class="notes-list-container">
          <div v-if="collectionLoading" class="loading-state">
            <div class="spinner"></div>
          </div>
          <div v-else-if="collectionList.length === 0" class="empty-state">
            <div class="empty-icon">⭐</div>
            <p>暂无收藏</p>
          </div>
          <div v-else class="note-grid">
            <PostCard
                v-for="note in collectionList"
                :key="note.id"
                :post="note"
                @click="handleNoteClick(note)"
            />
          </div>
        </div>

        <!-- Tab 4: 商品列表 (访客视角) -->
        <div v-else-if="currentTab === 'products'" class="products-list-container">
          <div v-if="productsLoading" class="loading-state">
            <div class="spinner"></div>
          </div>
          <div v-else-if="productsList.length === 0" class="empty-state">
            <div class="empty-icon">📦</div>
            <p>暂无在售商品</p>
          </div>
          <div v-else class="product-grid">
            <div
                v-for="product in productsList"
                :key="product.id"
                class="product-card"
                @click="$router.push(`/product/${product.id}`)"
            >
              <div class="image-wrapper">
                <img :src="getImageUrl(product.mainImage)" class="product-image" loading="lazy"/>
              </div>
              <div class="product-info">
                <h3 class="product-title">{{ product.name }}</h3>
                <div class="price-row">
                  <span class="price">
                    <span class="symbol">¥</span>{{ product.price }}
                  </span>
                  <span class="sales">{{ product.sales }}人付款</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 场景 B: 未创建店铺 (且不在 Dashboard Tab 时不显示，或保持引导) -->
        <div v-else-if="!shop" class="create-shop-guide">
          <div class="guide-icon">🏪</div>
          <h2>开启您的电商之旅</h2>
          <p class="guide-text">创建店铺，发布商品，让更多人发现您的好物</p>

          <div class="benefits-list">
            <div class="benefit-item">
              <span class="check">✓</span> 专属店铺主页
            </div>
            <div class="benefit-item">
              <span class="check">✓</span> 商品多规格管理
            </div>
            <div class="benefit-item">
              <span class="check">✓</span> 经营数据分析
            </div>
          </div>

          <el-button type="primary" size="large" class="create-btn" @click="showCreateModal = true">
            立即创建店铺
          </el-button>
        </div>
      </div>
    </div>

    <!-- 创建店铺弹窗 -->
    <el-dialog v-model="showCreateModal" title="创建店铺" width="500px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="店铺名称">
          <el-input v-model="createForm.name" placeholder="请输入店铺名称"/>
        </el-form-item>
        <el-form-item label="店铺Logo">
          <el-input v-model="createForm.image" placeholder="Logo URL"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showCreateModal = false">取消</el-button>
          <el-button type="primary" @click="handleCreateShop" :loading="creating">确定创建</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {ref, onMounted, reactive, computed} from 'vue'
import {useRouter, useRoute} from 'vue-router'
import {useUserStore} from '@/store/user'
import {useModal} from '@/utils/modal'
import {getMyShop, createShop, getShopDashboard, getShopByUserId} from '@/api/shop'
import {getAttentionList, getFansList, isAttention, toggleAttention} from '@/api/user'
import {getNoteListByUserId, getNoteListByCollectionUserId} from '@/api/note'
import {searchProduct, getProductsByShop} from '@/api/product'
import {getImageUrl} from '@/utils/image'
import {ElMessage} from 'element-plus'
import {Setting} from '@element-plus/icons-vue'
import PostCard from '@/components/PostCard.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const {showConfirm} = useModal()

const loading = ref(true)
const shop = ref(null)
const dashboard = ref({})
const stats = reactive({
  attentionCount: 0,
  fansCount: 0,
  likeCollectCount: 0
})

// Tab 切换
const currentTab = ref('dashboard')
const notesList = ref([])
const notesLoading = ref(false)
const collectionList = ref([])
const collectionLoading = ref(false)
const productsList = ref([])
const productsLoading = ref(false)

// 关注状态
const isFollowed = ref(false)
const followLoading = ref(false)

// 判断是否是本人
const isSelf = computed(() => {
  const currentUserId = userStore.userInfo?.id
  // 如果是从 /merchant/profile 进来，默认是本人
  if (route.path === '/merchant/profile') return true
  // 如果是从 /user/:id 进来，需要判断 ID
  const targetId = route.params.id
  return !targetId || String(targetId) === String(currentUserId)
})

// 创建店铺相关
const showCreateModal = ref(false)
const creating = ref(false)
const createForm = reactive({
  name: '',
  image: ''
})

const loadShopProducts = async (shopId) => {
  console.log('[loadShopProducts] called with shopId=', shopId)
  productsLoading.value = true
  try {
    // 调用 getProductsByShop 接口
    const res = await getProductsByShop(shopId)

    // 兼容后端返回格式
    let list = []
    if (res && res.list) {
      list = res.list
    } else if (res && res.records) {
      list = res.records
    } else if (Array.isArray(res)) {
      list = res
    }

    productsList.value = list
  } catch (err) {
    console.error('Failed to load products', err)
  } finally {
    productsLoading.value = false
  }
}

const fetchShopInfo = async () => {
  loading.value = true
  try {
    // 1. 获取店铺信息
    let res
    if (isSelf.value) {
      res = await getMyShop()
    } else {
      const targetId = route.params.id
      if (targetId) {
        res = await getShopByUserId(targetId)
      }
    }

    if (res) {
      shop.value = res
      // 如果有店铺，加载经营数据
      if (isSelf.value) {
        getShopDashboard().then(data => {
          dashboard.value = data
        }).catch(e => console.error(e))
      }

      // 如果是访客，立即加载店铺商品
      if (!isSelf.value) {
        currentTab.value = 'products'
        loadShopProducts(shop.value.id)
      }
    }

    console.log('[fetchShopInfo] route.path=', route.path, 'route.params.id=', route.params.id)
    console.log('[fetchShopInfo] currentUserId=', userStore.userInfo?.id, 'isSelf=', isSelf.value)
    console.log('[fetchShopInfo] shop=', res)
    // —— 强制兜底：进入页面后如果当前是商品Tab且有店铺，就拉商品 ——
    // 这样无论 watch 是否触发，都不会漏请求
    if (shop.value?.id && currentTab.value === 'products' && productsList.value.length === 0) {
      await loadShopProducts(shop.value.id)
    }


    // 2. 获取社交数据 (关注/粉丝)
    // 如果是本人，用当前登录ID；如果是访客，用路由参数ID
    const currentUserId = userStore.userInfo?.id
    const targetId = route.params.id
    // 优先使用路由参数ID (访客模式)，如果没有则使用当前登录ID (本人模式)
    const userId = targetId || currentUserId

    if (userId) {
      // 使用 Promise.allSettled 防止个别接口失败中断流程
      Promise.allSettled([
        getAttentionList(userId),
        getFansList(userId)
      ]).then(results => {
        const attentions = results[0].status === 'fulfilled' ? results[0].value : []
        const fans = results[1].status === 'fulfilled' ? results[1].value : []

        stats.attentionCount = attentions ? attentions.length : 0
        stats.fansCount = fans ? fans.length : 0
        stats.likeCollectCount = 0
      })

      // 加载笔记和收藏 (预加载或切换时加载，这里为了简化直接加载)
      loadUserNotes(userId)

      // 检查关注状态 (如果是访客)
      if (!isSelf.value) {
        currentTab.value = 'products'
        await loadShopProducts(shop.value.id)
      }
    }
  } catch (err) {
    // 允许 404 或业务错误表示无店铺
    console.log('未查询到店铺信息或社交数据', err)
  } finally {
    loading.value = false
  }
}

const handleNoteClick = (note) => {
  router.push(`/note/${note.id}`)
}

const handleFollow = async () => {
  if (followLoading.value) return

  if (isFollowed.value) {
    const confirmed = await showConfirm('确定要取消关注吗？', '取消关注')
    if (!confirmed) return
  }

  followLoading.value = true
  try {
    // const targetId = ...
    // await toggleAttention(targetId)
    isFollowed.value = !isFollowed.value
    if (isFollowed.value) {
      stats.fansCount++
    } else {
      stats.fansCount = Math.max(0, stats.fansCount - 1)
    }
  } catch (err) {
    ElMessage.error('操作失败')
  } finally {
    followLoading.value = false
  }
}

const handleChat = () => {
  // router.push(`/message/chat/${targetId}`)
  ElMessage.info('私信功能开发中')
}

const handleCreateShop = async () => {
  if (!createForm.name) {
    ElMessage.warning('请输入店铺名称')
    return
  }

  creating.value = true
  try {
    await createShop(createForm)
    ElMessage.success('店铺创建成功！')
    showCreateModal.value = false
    fetchShopInfo() // 刷新状态
  } catch (err) {
    ElMessage.error(err.message || '创建失败')
  } finally {
    creating.value = false
  }
}

// 监听Tab切换
import {watch} from 'vue'

watch(currentTab, (newTab) => {
  if (newTab === 'products' && shop.value) {
    // 避免重复加载
    if (productsList.value.length === 0) {
      loadShopProducts(shop.value.id)
    }
  }
})

onMounted(() => {
  fetchShopInfo()
})
</script>

<style scoped>
.merchant-profile {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 40px;
}

.header-bg {
  height: 120px;
  background: linear-gradient(135deg, #2c3e50, #4ca1af);
}

.content-container {
  max-width: 800px;
  margin: -60px auto 0;
  padding: 0 20px;
}

.user-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: flex-end;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
}

.avatar-wrapper {
  margin-right: 20px;
  position: relative;
}

.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 4px solid white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.user-info {
  flex: 1;
  padding-bottom: 5px;
}

.nickname {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
}

.merchant-badge {
  background: #e6a23c;
  color: white;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  margin-left: 8px;
}

.user-id {
  font-size: 12px;
  color: #999;
  margin-bottom: 12px;
}

.stats-row {
  display: flex;
  gap: 20px;
  margin-bottom: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  cursor: pointer;
}

.stat-item .count {
  font-weight: bold;
  color: #333;
}

.stat-item .label {
  color: #666;
}

.social-actions {
  display: flex;
  gap: 10px;
}

.tabs-container {
  display: flex;
  background: white;
  border-bottom: 1px solid #eee;
  margin-bottom: 1px;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 16px 0;
  font-size: 16px;
  color: #666;
  cursor: pointer;
  position: relative;
  font-weight: 500;
}

.tab-item.active {
  color: #333;
  font-weight: bold;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 3px;
  background: #ff2442;
  border-radius: 2px;
}

.shop-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  min-height: 400px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
}

.notes-list-container {
  min-height: 300px;
}

.note-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
  color: #999;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.shop-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.shop-basic {
  display: flex;
  align-items: center;
}

.shop-logo {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  margin-right: 12px;
  border: 1px solid #eee;
}

.shop-name {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 4px;
}

.shop-desc {
  font-size: 12px;
  color: #999;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 15px;
  margin-bottom: 30px;
}

.data-card {
  background: #f9f9f9;
  padding: 15px;
  border-radius: 8px;
  text-align: center;
}

.data-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
}

.data-value {
  font-size: 20px;
  font-weight: bold;
  color: #333;
}

.data-value.highlight {
  color: #ff2442;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  padding: 15px;
  border-radius: 8px;
  transition: background 0.2s;
}

.action-item:hover {
  background: #f0f0f0;
}

.action-item .icon {
  font-size: 24px;
  margin-bottom: 8px;
}

/* 未开店引导样式 */
.create-shop-guide {
  text-align: center;
  padding: 40px 0;
}

.guide-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.guide-text {
  color: #666;
  margin-bottom: 30px;
}

.benefits-list {
  display: inline-block;
  text-align: left;
  margin-bottom: 40px;
  background: #f9f9f9;
  padding: 20px 40px;
  border-radius: 8px;
}

.benefit-item {
  margin-bottom: 10px;
  color: #333;
}

.check {
  color: #67c23a;
  margin-right: 8px;
  font-weight: bold;
}

.create-btn {
  padding: 12px 40px;
  font-size: 16px;
}

.loading-state {
  text-align: center;
  padding: 60px 0;
  color: #999;
}
</style>
