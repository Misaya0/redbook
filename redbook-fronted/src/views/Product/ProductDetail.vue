<template>
  <div class="product-detail-container">
    <div class="product-detail">
      <!-- 顶部导航 -->
      <div class="nav-header">
      <button type="button" class="nav-icon-btn" @click="router.back()" aria-label="返回">
        <span class="nav-icon nav-icon--back" aria-hidden="true"></span>
      </button>
      <span class="title">商品详情</span>
      <button type="button" class="nav-icon-btn" @click="handleShare" aria-label="分享">
        <span class="nav-icon nav-icon--share" aria-hidden="true"></span>
      </button>
    </div>

    <div v-if="loading" class="loading-container">
      <div class="spinner"></div>
      <p>加载商品中...</p>
    </div>

    <div v-else-if="error" class="error-container">
      <p>{{ error }}</p>
      <button class="retry-btn" @click="fetchProduct">重试</button>
    </div>

    <div v-else class="content-scroll">
      <!-- 主图轮播 -->
      <div class="product-gallery">
        <img :src="currentImage" class="main-image" @click="previewImage(currentImage)"/>
        <div class="gallery-dots" v-if="product.detailImages">
          <!-- 简化版：仅展示主图，实际项目可做轮播 -->
        </div>
      </div>

      <!-- 商品信息 -->
      <div class="product-info-card">
        <div class="price-row">
          <span class="currency">¥</span>
          <span class="price">{{ currentPrice }}</span>
          <span class="sales">已售 {{ product.sales || 0 }}</span>
        </div>
        <h1 class="product-name">{{ product.name }}</h1>
        <p class="product-desc">{{ product.title }}</p>
      </div>

      <!-- 规格选择 -->
      <div class="sku-selector card" v-if="product.skus && product.skus.length > 0">
        <div class="section-title">规格选择</div>
        <SpecSelector :skus="product.skus" :spec-groups="specGroups" v-model:skuId="selectedSkuId" />
      </div>

      <!-- 店铺信息 -->
      <div class="shop-card card" v-if="product.shop">
        <div class="shop-header">
          <img :src="getImageUrl(product.shop.image)" class="shop-avatar"/>
          <div class="shop-info">
            <div class="shop-name">{{ product.shop.name }}</div>
            <div class="shop-stats">粉丝 {{ product.shop.fans || 0 }}</div>
          </div>
          <button class="shop-btn" @click="goToShop">进店</button>
        </div>
      </div>

      <!-- 关联笔记 (种草秀) -->
      <div class="related-notes card" v-if="product.relatedNotes && product.relatedNotes.length > 0">
        <div class="section-title">商品种草秀</div>
        <div class="note-list">
          <div v-for="note in product.relatedNotes" :key="note.id" class="note-item" @click="router.push(`/note/${note.id}`)">
            <img :src="getImageUrl(note.image)" class="note-cover"/>
            <div class="note-title">{{ note.title }}</div>
            <div class="note-user">
              <img :src="getImageUrl(note.user?.image)" class="user-avatar-small"/>
              <span>{{ note.user?.nickname || '小红薯' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 商品详情图 -->
      <div class="detail-images card">
        <div class="section-title">商品详情</div>
        <div class="image-list">
           <!-- 简化处理，直接展示详情描述 -->
           <div class="empty-desc" v-if="!product.detailImages">暂无详情图</div>
        </div>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div class="bottom-bar">
      <div class="icon-btn" @click="goToShop">
        <span class="icon">🏠</span>
        <span class="text">店铺</span>
      </div>
      <div class="icon-btn">
        <span class="icon">💬</span>
        <span class="text">客服</span>
      </div>
      <div class="icon-btn">
        <span class="icon">⭐</span>
        <span class="text">收藏</span>
      </div>
      <div class="action-btns">
        <button class="add-cart">加入购物车</button>
        <button class="buy-now" @click="handleBuy">立即购买</button>
      </div>
    </div>
  </div>
</div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductDetail, getProductSpecs } from '@/api/product'
import { saveOrder } from '@/api/order'
import { getImageUrl } from '@/utils/image'
import { useModal } from '@/utils/modal'
import SpecSelector from './components/SpecSelector.vue'

const route = useRoute()
const router = useRouter()
const { showToast } = useModal()

const productId = route.params.id
const loading = ref(true)
const error = ref(null)
const product = ref({})
const selectedSkuId = ref(null)
const specGroups = ref([])

// 计算属性
const currentPrice = computed(() => {
  if (selectedSkuId.value) {
    const sku = product.value.skus.find(s => s.id === selectedSkuId.value)
    return sku ? sku.price : product.value.price
  }
  return product.value.price
})

const currentImage = computed(() => {
  if (selectedSkuId.value) {
    const sku = product.value.skus.find(s => s.id === selectedSkuId.value)
    if (sku && sku.image) return getImageUrl(sku.image)
  }
  return getImageUrl(product.value.mainImage)
})

// 方法
const fetchProduct = async () => {
  loading.value = true
  error.value = null
  try {
    const res = await getProductDetail(productId)
    product.value = res
    try {
      const specRes = await getProductSpecs(productId, { skipErrorHandler: true })
      specGroups.value = specRes?.specGroups || []
    } catch (e) {
      specGroups.value = []
    }
    // 默认选中第一个有库存的 SKU
    if (res.skus && res.skus.length > 0) {
      const validSku = res.skus.find(s => s.stock > 0)
      if (validSku) selectedSkuId.value = validSku.id
    }
  } catch (err) {
    console.error(err)
    error.value = '加载商品失败'
  } finally {
    loading.value = false
  }
}

const goToShop = () => {
  if (product.value.shop?.id) {
    router.push(`/shop/${product.value.shop.id}`)
  } else if (product.value.shopId) {
    router.push(`/shop/${product.value.shopId}`)
  }
}

const handleBuy = async () => {
  if (!selectedSkuId.value && product.value.skus && product.value.skus.length > 0) {
    showToast('请选择规格')
    return
  }
  try {
    await saveOrder({
      skuId: selectedSkuId.value,
      quantity: 1
    })
    showToast('下单成功')
    router.push('/orders')
  } catch (e) {
    showToast('下单失败')
  }
}

const handleShare = async () => {
  const url = window.location.href
  try {
    if (navigator.share) {
      await navigator.share({
        title: product.value?.name || '商品详情',
        url
      })
      return
    }
  } catch (e) {
  }

  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(url)
      showToast('链接已复制')
      return
    }
  } catch (e) {
  }

  showToast('当前环境不支持分享')
}

const previewImage = (url) => {
  // 图片预览逻辑
}

onMounted(() => {
  fetchProduct()
})
</script>

<style scoped>
.product-detail-container {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  justify-content: center;
}

.product-detail {
  width: 100%;
  max-width: 800px;
  background: #ffffff;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.05);
  --nav-icon-color: #333;
  --nav-icon-bg-hover: rgba(0, 0, 0, 0.05);
  --nav-icon-bg-active: rgba(0, 0, 0, 0.08);
  --nav-icon-color-active: #ff2442;
  --nav-bg: #ffffff;
}

.nav-header {
  height: 44px;
  background: var(--nav-bg);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-icon-btn {
  width: 44px;
  height: 44px;
  padding: 0;
  border: none;
  background: transparent;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--nav-icon-color);
  cursor: pointer;
  transition: transform 0.12s ease, background 0.12s ease, color 0.12s ease;
  -webkit-tap-highlight-color: transparent;
}

@media (hover: hover) {
  .nav-icon-btn:hover {
    background: var(--nav-icon-bg-hover);
  }
}

.nav-icon-btn:active {
  transform: scale(0.96);
  background: var(--nav-icon-bg-active);
  color: var(--nav-icon-color-active);
}

.nav-icon {
  width: 24px;
  height: 24px;
  display: block;
  background-color: currentColor;
  mask-repeat: no-repeat;
  mask-position: center;
  mask-size: contain;
  -webkit-mask-repeat: no-repeat;
  -webkit-mask-position: center;
  -webkit-mask-size: contain;
}

.nav-icon--back {
  mask-image: url('@/assets/icons/nav-back@1x.svg');
  -webkit-mask-image: url('@/assets/icons/nav-back@1x.svg');
}

.nav-icon--share {
  mask-image: url('@/assets/icons/nav-share@1x.svg');
  -webkit-mask-image: url('@/assets/icons/nav-share@1x.svg');
}

@media (min-resolution: 2dppx) {
  .nav-icon--back {
    mask-image: url('@/assets/icons/nav-back@2x.svg');
    -webkit-mask-image: url('@/assets/icons/nav-back@2x.svg');
  }
  .nav-icon--share {
    mask-image: url('@/assets/icons/nav-share@2x.svg');
    -webkit-mask-image: url('@/assets/icons/nav-share@2x.svg');
  }
}

@media (min-resolution: 3dppx) {
  .nav-icon--back {
    mask-image: url('@/assets/icons/nav-back@3x.svg');
    -webkit-mask-image: url('@/assets/icons/nav-back@3x.svg');
  }
  .nav-icon--share {
    mask-image: url('@/assets/icons/nav-share@3x.svg');
    -webkit-mask-image: url('@/assets/icons/nav-share@3x.svg');
  }
}

@media (prefers-color-scheme: dark) {
  .product-detail {
    --nav-icon-color: rgba(255, 255, 255, 0.92);
    --nav-icon-bg-hover: rgba(255, 255, 255, 0.1);
    --nav-icon-bg-active: rgba(255, 255, 255, 0.14);
    --nav-bg: #111111;
  }
}

.content-scroll {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 60px;
}

.product-gallery {
  background: white;
  aspect-ratio: 1;
}

.main-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-info-card {
  background: white;
  padding: 16px;
  margin-bottom: 10px;
}

.price-row {
  color: #ff2442;
  margin-bottom: 8px;
  display: flex;
  align-items: baseline;
}

.currency {
  font-size: 14px;
  font-weight: bold;
}

.price {
  font-size: 24px;
  font-weight: bold;
  margin-left: 2px;
}

.sales {
  margin-left: auto;
  font-size: 12px;
  color: #999;
}

.product-name {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
  line-height: 1.4;
}

.product-desc {
  font-size: 13px;
  color: #666;
  line-height: 1.4;
}

.card {
  background: white;
  margin-bottom: 10px;
  padding: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 12px;
  color: #333;
}

.shop-header {
  display: flex;
  align-items: center;
}

.shop-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 10px;
}

.shop-info {
  flex: 1;
}

.shop-name {
  font-size: 14px;
  font-weight: bold;
}

.shop-stats {
  font-size: 12px;
  color: #999;
}

.shop-btn {
  padding: 4px 12px;
  border: 1px solid #ff2442;
  color: #ff2442;
  border-radius: 14px;
  font-size: 12px;
  background: white;
}

.note-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.note-item {
  background: #fff;
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.note-cover {
  width: 100%;
  aspect-ratio: 3/4;
  object-fit: cover;
}

.note-title {
  font-size: 12px;
  padding: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-user {
  padding: 0 8px 8px;
  display: flex;
  align-items: center;
  font-size: 11px;
  color: #666;
}

.user-avatar-small {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  margin-right: 4px;
}

.bottom-bar {
  height: 60px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  padding: 0 16px;
  position: sticky;
  bottom: 0;
  z-index: 100;
  max-width: 800px;
  width: 100%;
}

.action-btns {
  flex: 1;
  display: flex;
  gap: 8px;
  margin-left: 16px;
}

.add-cart, .buy-now {
  flex: 1;
  height: 40px;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.add-cart {
  background: #fff1f2;
  color: #ff2442;
  border: 1px solid #ff2442;
}

.buy-now {
  background: #ff2442;
  color: #fff;
}

.add-cart:hover, .buy-now:hover {
  opacity: 0.9;
}

.icon-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-right: 16px;
  font-size: 10px;
  color: #666;
}

.icon-btn .icon {
  font-size: 18px;
  margin-bottom: 2px;
}

.action-btn {
  flex: 1;
  height: 36px;
  border-radius: 18px;
  font-size: 14px;
  font-weight: bold;
  color: white;
  border: none;
}

.cart-btn {
  background: #ffb800;
  margin-right: 10px;
}

.buy-btn {
  background: #ff2442;
}

.loading-container, .error-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
}

.spinner {
  width: 30px;
  height: 30px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #ff2442;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 10px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>
