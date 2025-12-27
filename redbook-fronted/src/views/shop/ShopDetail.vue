<template>
  <div class="shop-detail-page">
    <!-- 顶部 Banner -->
    <div class="shop-banner" :style="{ backgroundImage: `url(${shopInfo.banner || 'https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=1600&q=80'})` }">
      <div class="banner-mask"></div>
    </div>

    <div class="main-container">
      <!-- 店铺头部信息卡片 -->
      <div class="shop-header-card">
        <div class="header-content">
          <div class="left-info">
            <div class="avatar-wrapper">
              <img :src="getImageUrl(shopInfo.image)" class="shop-avatar" />
            </div>
            <div class="text-info">
              <h1 class="shop-name">
                {{ shopInfo.name }}
                <span class="owner-suffix" v-if="ownerNickname">({{ ownerNickname }}的店铺)</span>
              </h1>
              <div class="shop-stats">
                <span class="stat-item">粉丝 {{ formatNumber(fansCount) }}</span>
                <span class="divider">|</span>
                <span class="stat-item">已售 {{ formatNumber(shopInfo.salesCount) }}</span>
              </div>
              <div class="shop-tags" v-if="shopInfo.tags">
                <el-tag v-for="tag in shopInfo.tags" :key="tag" size="small" effect="plain" class="shop-tag">
                  {{ tag }}
                </el-tag>
              </div>
            </div>
          </div>
          <div class="right-actions">
            <el-button 
              type="primary" 
              class="follow-btn" 
              :class="{ 'is-followed': isFollowed }"
              :round="true"
              :loading="followLoading"
              @click="handleFollow"
            >
              {{ isFollowed ? '已关注' : '关注店铺' }}
            </el-button>
            <el-button class="chat-btn" :round="true" @click="handleChat">私信商家</el-button>
          </div>
        </div>
      </div>

      <!-- 筛选工具栏 -->
      <div class="filter-bar-wrapper" :class="{ 'is-sticky': isSticky }">
        <div class="filter-bar">
          <div 
            class="filter-item" 
            :class="{ active: sortType === 'default' }" 
            @click="handleSort('default')"
          >
            综合
          </div>
          <div 
            class="filter-item" 
            :class="{ active: sortType === 'sales' }" 
            @click="handleSort('sales')"
          >
            销量
          </div>
          <div 
            class="filter-item" 
            :class="{ active: sortType === 'price' }" 
            @click="handleSort('price')"
          >
            价格
            <span class="price-icons">
              <el-icon :class="{ active: sortType === 'price' && priceOrder === 'asc' }"><CaretTop /></el-icon>
              <el-icon :class="{ active: sortType === 'price' && priceOrder === 'desc' }"><CaretBottom /></el-icon>
            </span>
          </div>
        </div>
      </div>

      <!-- 商品展示区 -->
      <div class="product-grid-container">
        <el-row :gutter="20">
          <el-col 
            v-for="product in productList" 
            :key="product.id" 
            :xs="12" :sm="8" :md="6" :lg="6" :xl="6"
            class="product-col"
          >
            <div class="product-card" @click="goToDetail(product.id)">
              <div class="image-wrapper">
                <img :src="getImageUrl(product.mainImage || product.image)" class="product-image" loading="lazy" />
                <div class="image-hover-mask">
                  <span class="view-detail">查看详情</span>
                </div>
              </div>
              <div class="product-info">
                <h3 class="product-title" v-html="product.name"></h3>
                <div class="product-footer">
                  <div class="price-box">
                    <span class="symbol">¥</span>
                    <span class="price">{{ product.price }}</span>
                  </div>
                  <span class="sales">已售 {{ product.sales || 0 }}</span>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>

        <!-- 加载状态 -->
        <div class="loading-more" v-if="loading">
          <el-skeleton :rows="3" animated />
        </div>
        <div class="no-more" v-else-if="!hasMore">
          <span class="line"></span>
          <span class="text">没有更多商品了</span>
          <span class="line"></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CaretTop, CaretBottom } from '@element-plus/icons-vue'
import { getImageUrl } from '@/utils/image'
import { getShopById } from '@/api/shop'
import { searchProduct } from '@/api/product'
import { getUserById, isAttention, toggleAttention, getFansList } from '@/api/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

// 状态
const shopId = ref(route.params.id)
const sortType = ref('default') // default, sales, price
const priceOrder = ref('asc') // asc, desc
const isSticky = ref(false)
const loading = ref(false)
const listLoading = ref(false)
const page = ref(1)
const pageSize = 12
const hasMore = ref(true)

// 数据
const shopInfo = ref({})
const productList = ref([])
const isFollowed = ref(false)
const followLoading = ref(false)
const fansCount = ref(0)
const ownerNickname = ref('')

// 获取店铺信息
const fetchShopInfo = async () => {
  loading.value = true
  try {
    const res = await getShopById(shopId.value)
    if (res) {
      shopInfo.value = res
      // 获取到店铺信息后，拉取店主的社交数据和基本信息
      if (res.userId) {
        checkFollowStatus(res.userId)
        fetchFansCount(res.userId)
        fetchOwnerInfo(res.userId)
      }
    }
  } catch (err) {
    console.error('Fetch shop info failed:', err)
    ElMessage.error('获取店铺信息失败')
  } finally {
    loading.value = false
  }
}

// 获取店主信息
const fetchOwnerInfo = async (userId) => {
  try {
    const res = await getUserById(userId)
    if (res) {
      ownerNickname.value = res.nickname || '用户' + userId
    }
  } catch (err) {
    console.error('Fetch owner info failed:', err)
  }
}

// 检查关注状态
const checkFollowStatus = async (userId) => {
  try {
    const res = await isAttention(userId)
    isFollowed.value = res === 1
  } catch (err) {
    console.error('Check follow status failed:', err)
  }
}

// 获取粉丝数量
const fetchFansCount = async (userId) => {
  try {
    const res = await getFansList(userId)
    fansCount.value = res ? res.length : 0
  } catch (err) {
    console.error('Fetch fans count failed:', err)
  }
}

// 处理关注/取消关注
const handleFollow = async () => {
  if (!shopInfo.value.userId) return
  
  followLoading.value = true
  try {
    const res = await toggleAttention(shopInfo.value.userId)
    // 后端返回 1 表示关注成功，2 表示取消关注成功
    isFollowed.value = res === 1
    // 更新粉丝数
    fetchFansCount(shopInfo.value.userId)
    ElMessage.success(isFollowed.value ? '关注成功' : '已取消关注')
  } catch (err) {
    console.error('Toggle follow failed:', err)
    ElMessage.error('操作失败，请重试')
  } finally {
    followLoading.value = false
  }
}

// 获取商品列表
const fetchProductList = async (isLoadMore = false) => {
  if (listLoading.value) return
  if (!isLoadMore) {
    page.value = 1
    productList.value = []
    hasMore.value = true
  }

  listLoading.value = true
  try {
    const searchDto = {
      shopId: parseInt(shopId.value),
      sort: sortType.value,
      order: sortType.value === 'price' ? priceOrder.value : null,
      pageNum: page.value,
      pageSize: pageSize
    }

    const res = await searchProduct(searchDto)
    // 兼容多种返回格式
    const newProducts = Array.isArray(res) 
      ? res 
      : (res.list || res.records || res.data || [])
    
    if (newProducts.length < pageSize) {
      hasMore.value = false
    }

    if (isLoadMore) {
      productList.value = [...productList.value, ...newProducts]
    } else {
      productList.value = newProducts
    }
    
    page.value++
  } catch (err) {
    console.error('Fetch product list failed:', err)
  } finally {
    listLoading.value = false
  }
}

// 监听路由参数变化
watch(() => route.params.id, (newId) => {
  if (newId) {
    shopId.value = newId
    initData()
  }
})

// 初始化数据
const initData = () => {
  fetchShopInfo()
  fetchProductList()
}

// 方法
const handleSort = (type) => {
  if (type === 'price') {
    if (sortType.value === 'price') {
      priceOrder.value = priceOrder.value === 'asc' ? 'desc' : 'asc'
    } else {
      sortType.value = 'price'
      priceOrder.value = 'asc'
    }
  } else {
    if (sortType.value === type) return // 避免重复点击
    sortType.value = type
  }
  fetchProductList()
}

const loadMore = () => {
  if (hasMore.value && !listLoading.value) {
    fetchProductList(true)
  }
}

const formatNumber = (num) => {
  if (!num) return 0
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  return num
}

const goToDetail = (id) => {
  router.push(`/product/${id}`)
}

const handleChat = () => {
  if (shopInfo.value.userId) {
    router.push(`/message/chat/${shopInfo.value.userId}`)
  } else {
    ElMessage.warning('暂无法联系商家')
  }
}

// 监听滚动实现工具栏吸顶
const handleScroll = () => {
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop
  isSticky.value = scrollTop > 350 // 大约 Banner + Header 的高度
  
  // 触底加载更多
  const scrollHeight = document.documentElement.scrollHeight
  const clientHeight = document.documentElement.clientHeight
  if (scrollTop + clientHeight >= scrollHeight - 100) {
    loadMore()
  }
}

onMounted(() => {
  initData()
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped lang="scss">
.shop-detail-page {
  background-color: #f5f7fa;
  min-height: 100vh;
  padding-bottom: 50px;

  .shop-banner {
    height: 300px;
    background-size: cover;
    background-position: center;
    position: relative;
    
    .banner-mask {
      position: absolute;
      inset: 0;
      background: linear-gradient(180deg, rgba(0,0,0,0.2) 0%, rgba(0,0,0,0.4) 100%);
    }
  }

  .main-container {
    max-width: 1000px;
    margin: -60px auto 0;
    position: relative;
    padding: 0 20px;
    z-index: 10;
  }

  .shop-header-card {
    background: #fff;
    border-radius: 16px;
    padding: 30px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
    margin-bottom: 24px;

    .header-content {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;

      .left-info {
        display: flex;
        gap: 24px;

        .avatar-wrapper {
          width: 100px;
          height: 100px;
          border-radius: 50%;
          border: 4px solid #fff;
          box-shadow: 0 2px 8px rgba(0,0,0,0.1);
          overflow: hidden;
          flex-shrink: 0;
          margin-top: -50px;
          background: #fff;

          .shop-avatar {
            width: 100%;
            height: 100%;
            object-fit: cover;
          }
        }

        .text-info {
          .shop-name {
            font-size: 24px;
            font-weight: 700;
            color: #333;
            margin: 0 0 8px 0;
            display: flex;
            align-items: baseline;
            gap: 8px;

            .owner-suffix {
              font-size: 14px;
              color: #999;
              font-weight: normal;
            }
          }

          .shop-stats {
            display: flex;
            align-items: center;
            gap: 12px;
            margin-bottom: 12px;
            color: #666;
            font-size: 14px;

            .divider {
              color: #eee;
            }

            .rating {
              margin-left: 4px;
            }
          }

          .shop-desc {
            font-size: 14px;
            color: #999;
            margin-bottom: 12px;
            line-height: 1.5;
            max-width: 600px;
          }

          .shop-tags {
            display: flex;
            gap: 8px;
            
            .shop-tag {
              border-radius: 4px;
            }
          }
        }
      }

      .right-actions {
        display: flex;
        gap: 12px;

        .follow-btn {
          padding: 8px 24px;
          background-color: #ff2442;
          border-color: #ff2442;
          font-weight: 600;

          &.is-followed {
            background-color: #f5f5f5;
            border-color: #eee;
            color: #999;
          }

          &:hover {
            opacity: 0.9;
          }
        }

        .chat-btn {
          padding: 0 24px;
          height: 36px;
          color: #666;
          
          &:hover {
            color: #ff2442;
            border-color: #ff2442;
          }
        }
      }
    }
  }

  .filter-bar-wrapper {
    background: #fff;
    border-radius: 8px;
    margin-bottom: 20px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.02);
    transition: all 0.3s;

    &.is-sticky {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      z-index: 100;
      border-radius: 0;
      margin-bottom: 0;
      padding: 0 calc((100vw - 1000px) / 2 + 20px);
      box-shadow: 0 2px 12px rgba(0,0,0,0.08);
    }

    .filter-bar {
      display: flex;
      height: 50px;
      align-items: center;
      gap: 40px;
      padding: 0 24px;

      .filter-item {
        font-size: 15px;
        color: #666;
        cursor: pointer;
        position: relative;
        height: 100%;
        display: flex;
        align-items: center;
        transition: all 0.2s;

        &:hover {
          color: #333;
        }

        &.active {
          color: #333;
          font-weight: 700;

          &::after {
            content: '';
            position: absolute;
            bottom: 0;
            left: 0;
            right: 0;
            height: 3px;
            background-color: #ff2442;
            border-radius: 2px;
          }
        }

        .price-icons {
          display: flex;
          flex-direction: column;
          margin-left: 4px;
          font-size: 10px;
          line-height: 1;

          .el-icon {
            height: 6px;
            color: #ccc;
            
            &.active {
              color: #ff2442;
            }
          }
        }
      }
    }
  }

  .product-grid-container {
    .product-col {
      margin-bottom: 20px;
    }

    .product-card {
      background: #fff;
      border-radius: 12px;
      overflow: hidden;
      cursor: pointer;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      border: 1px solid #f0f0f0;

      &:hover {
        transform: translateY(-5px);
        box-shadow: 0 10px 20px rgba(0,0,0,0.08);

        .image-hover-mask {
          opacity: 1;
        }
      }

      .image-wrapper {
        position: relative;
        padding-top: 100%; // 1:1 Aspect Ratio
        overflow: hidden;

        .product-image {
          position: absolute;
          top: 0;
          left: 0;
          width: 100%;
          height: 100%;
          object-fit: cover;
        }

        .image-hover-mask {
          position: absolute;
          inset: 0;
          background: rgba(0, 0, 0, 0.2);
          display: flex;
          align-items: center;
          justify-content: center;
          opacity: 0;
          transition: opacity 0.3s;

          .view-detail {
            color: #fff;
            padding: 6px 16px;
            border: 1px solid #fff;
            border-radius: 20px;
            font-size: 12px;
            background: rgba(255, 255, 255, 0.1);
            backdrop-filter: blur(4px);
          }
        }
      }

      .product-info {
        padding: 12px;

        .product-title {
          font-size: 14px;
          color: #333;
          font-weight: 500;
          margin: 0 0 10px 0;
          line-height: 1.4;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
          height: 40px;
        }

        .product-footer {
          display: flex;
          justify-content: space-between;
          align-items: center;

          .price-box {
            color: #ff2442;
            
            .symbol {
              font-size: 12px;
              margin-right: 2px;
            }
            .price {
              font-size: 18px;
              font-weight: 700;
            }
          }

          .sales {
            font-size: 12px;
            color: #999;
          }
        }
      }
    }

    .no-more {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 15px;
      margin-top: 40px;
      color: #ccc;
      
      .line {
        height: 1px;
        width: 60px;
        background: #eee;
      }
      .text {
        font-size: 13px;
      }
    }
  }
}

// 适配 PC 端显示 (一行 4 个)
:deep(.el-col-lg-6) {
  flex: 0 0 25%;
  max-width: 25%;
}
</style>
