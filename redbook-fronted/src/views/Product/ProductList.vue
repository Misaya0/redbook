<template>
  <div class="product-search-page">
    <!-- 顶部搜索栏 -->
    <div class="search-header">
      <button class="back-btn" @click="router.back()">
        <span class="icon">←</span>
      </button>
      <div class="search-input-wrapper">
        <span class="search-icon">🔍</span>
        <input
            v-model="searchKeyword"
            placeholder="搜索商品"
            class="search-input"
            @keyup.enter="handleSearch"
        />
        <span class="clear-icon" v-if="searchKeyword" @click="searchKeyword = ''">×</span>
      </div>
      <button class="search-btn" @click="handleSearch">搜索</button>
    </div>

    <!-- 分类导航栏 -->
    <div class="category-nav-container">
      <!-- 一级分类 (横向滚动) -->
      <div class="category-scroll-view" ref="categoryScroll">
        <div
            v-for="item in categoryLevel1"
            :key="item.id"
            class="category-item"
            :class="{ active: selectedCategory1 === item.id }"
            @click="handleCategory1Click(item)"
        >
          {{ item.name }}
        </div>
      </div>

      <!-- 二级分类 (网格) -->
      <div v-if="categoryLevel2.length > 0" class="subcategory-panel">
        <div class="subcategory-grid">
          <div
              v-for="sub in categoryLevel2"
              :key="sub.id"
              class="subcategory-item"
              :class="{ active: selectedCategory2 === sub.id }"
              :style="selectedCategory2 === sub.id ? 'background: #fff1f2; color: #ff2442; border: 1px solid #ff2442; font-weight: 500;' : ''"
              @click="handleCategory2Click(sub)"
          >
            <div class="subcategory-text">{{ sub.name }} <span v-if="false">({{sub.id}})</span></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-item" :class="{ active: sortType === 'default' }" @click="sortType = 'default'">综合</div>
      <div class="filter-item" :class="{ active: sortType === 'sales' }" @click="sortType = 'sales'">销量</div>
      <div class="filter-item" :class="{ active: sortType === 'price' }" @click="togglePriceSort">
        价格
        <span class="sort-icon">{{ priceSortIcon }}</span>
      </div>
    </div>

    <!-- 商品列表 (瀑布流) -->
    <div class="product-list-content">
      <div v-if="loading && page === 1" class="loading-state">
        <div class="spinner"></div>
        <p>正在搜索...</p>
      </div>

      <div v-else-if="products.length === 0" class="empty-state">
        <div class="empty-icon">📦</div>
        <p>暂无相关商品</p>
      </div>

      <div v-else class="product-grid">
        <div
            v-for="product in products"
            :key="product.id"
            class="product-card"
            @click="navigateToDetail(product.id)"
        >
          <div class="image-wrapper">
            <img :src="getImageUrl(product.mainImage)" class="product-image" loading="lazy"/>
          </div>
          <div class="product-info">
            <h3 class="product-title">{{ product.name }}</h3>
            <div class="tags" v-if="product.tags">
              <span v-for="tag in product.tags" :key="tag" class="tag">{{ tag }}</span>
            </div>
            <div class="price-row">
              <span class="price">
                <span class="symbol">¥</span>{{ product.price }}
              </span>
              <span class="sales">{{ formatSales(product.sales) }}人付款</span>
            </div>
            <div class="shop-row" v-if="product.shopName">
              <span class="shop-name">{{ product.shopName }}</span>
              <span class="arrow">›</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载更多 -->
      <div v-if="products.length > 0" class="load-more">
        <span v-if="loading">加载中...</span>
        <span v-else-if="hasMore" @click="loadMore">点击加载更多</span>
        <span v-else>没有更多了</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, computed, onMounted, watch} from 'vue'
import {useRouter, useRoute} from 'vue-router'
import {searchProduct, getCategoryList} from '@/api/product'
import {getImageUrl} from '@/utils/image'
import {ElMessage} from 'element-plus'

const router = useRouter()
const route = useRoute()

// 状态
const searchKeyword = ref('')
const categoryId = ref(null)
const products = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const hasMore = ref(true)
const sortType = ref('default') // default, sales, price
const priceSortOrder = ref('asc') // asc, desc

// 分类相关状态
const categoryLevel1 = ref([])
const categoryLevel2 = ref([])
const selectedCategory1 = ref(null)
const selectedCategory2 = ref(null)
const categoryScroll = ref(null)

// 计算属性
const priceSortIcon = computed(() => {
  if (sortType.value !== 'price') return '↕'
  return priceSortOrder.value === 'asc' ? '↑' : '↓'
})

// 方法
const handleSearch = () => {
  page.value = 1
  products.value = []
  hasMore.value = true
  fetchProducts()
}

// 加载一级分类
const fetchCategories = async () => {
  try {
    // 获取 level=1 的分类
    const res = await getCategoryList(null, 1)
    
    // 添加"全部"选项
    const allOption = { id: -1, name: '全部' }
    categoryLevel1.value = [allOption, ...(res || [])]

    // 默认选中"全部"
    handleCategory1Click(allOption)
  } catch (err) {
    console.error('Failed to load categories', err)
  }
}

// 点击一级分类
const handleCategory1Click = async (item) => {
  selectedCategory1.value = item.id
  selectedCategory2.value = null // 切换一级分类时重置二级分类选中
  
  if (item.id === -1) {
    // 点击全部
    categoryId.value = null
    categoryLevel2.value = []
    handleSearch() // 重新搜索全部商品
    return
  }

  // 不是全部，正常加载子分类
  // categoryId.value = null // 这里不应该立即重置搜索，除非点击一级分类也触发搜索
  // 按照通常交互，点击一级分类展示其子分类，点击子分类才触发筛选
  // 如果产品要求点击一级分类也筛选，则这里赋值 item.id，但目前代码逻辑是点击子分类才赋值
  
  categoryLevel2.value = [] // 先清空

  try {
    // 获取 level=2 且 parentId=item.id 的分类
    const res = await getCategoryList(item.id, 2)
    categoryLevel2.value = res || []
  } catch (err) {
    console.error('Failed to load subcategories', err)
  }
}

// 点击二级分类
const handleCategory2Click = (item) => {
  console.log('Triggered handleCategory2Click')
  console.log('Item:', item)
  console.log('Current selectedCategory2:', selectedCategory2.value)
  
  // 如果点击已选中的，则不做处理
  if (selectedCategory2.value === item.id) {
    console.log('Already selected category:', item.id)
    return
  }
  
  // 选中新分类
  selectedCategory2.value = item.id
  console.log('Set selectedCategory2 to:', selectedCategory2.value)
  categoryId.value = item.id
  // 选中分类时清空搜索词，体验更好
  searchKeyword.value = '' 
  
  console.log('Searching with categoryId:', categoryId.value, 'keyword:', searchKeyword.value)
  handleSearch()
}

const togglePriceSort = () => {
  if (sortType.value === 'price') {
    priceSortOrder.value = priceSortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortType.value = 'price'
    priceSortOrder.value = 'asc'
  }
  handleSearch() // 重新搜索
}

const fetchProducts = async () => {
  if (loading.value || !hasMore.value) return

  loading.value = true
  try {
    const searchDto = {
      keyword: searchKeyword.value,
      categoryId: categoryId.value,
      pageNum: page.value,
      pageSize: pageSize,
      // 这里可以根据 sortType 扩展后端排序参数
      // sort: sortType.value,
      // order: priceSortOrder.value
    }

    const res = await searchProduct(searchDto)
    // 兼容多种返回格式：
    // 1. 拦截器处理后的分页对象: { list: [], total: ... }
    // 2. 直接的数组: []
    // 3. 原始的分页对象: { records: [], ... }
    const newProducts = Array.isArray(res) 
      ? res 
      : (res.list || res.records || res.data || [])
    
    if (newProducts.length < pageSize) {
      hasMore.value = false
    }

    products.value = [...products.value, ...newProducts]
    page.value++
  } catch (err) {
    console.error('Search failed', err)
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  fetchProducts()
}

const navigateToDetail = (id) => {
  router.push(`/product/${id}`)
}

const formatSales = (num) => {
  if (!num) return '0'
  if (num > 10000) return (num / 10000).toFixed(1) + '万+'
  return num
}

// 初始化
onMounted(() => {
  // 如果路由带有查询参数
  if (route.query.keyword) {
    searchKeyword.value = route.query.keyword
    handleSearch()
  } else {
    // 默认加载推荐列表
    fetchProducts()
  }

  // 加载分类
  fetchCategories()
})
</script>

<style scoped>
/* 分类导航样式 */
.category-nav-container {
  background: white;
  margin-bottom: 8px;
}

.category-scroll-view {
  display: flex;
  overflow-x: auto;
  white-space: nowrap;
  padding: 0 12px;
  border-bottom: 1px solid #f5f5f5;
  scrollbar-width: none; /* Firefox */
}

.category-scroll-view::-webkit-scrollbar {
  display: none; /* Chrome/Safari */
}

.category-item {
  padding: 12px 16px;
  font-size: 14px;
  color: #666;
  position: relative;
  cursor: pointer;
  flex-shrink: 0;
}

.category-item.active {
  color: #333;
  font-weight: 600;
  font-size: 15px;
}

.category-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 3px;
  background: #ff2442;
  border-radius: 2px;
}

.subcategory-panel {
  padding: 12px;
  background: #fff;
}

.subcategory-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.subcategory-item {
  background: #f8f8f8;
  border-radius: 4px;
  padding: 8px 4px;
  text-align: center;
  font-size: 12px;
  color: #333;
  cursor: pointer;
}

.subcategory-item.active {
  background: #fff1f2;
  color: #ff2442;
  font-weight: 500;
  border: 1px solid #ff2442;
}

.product-search-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.search-header {
  height: auto;
  min-height: 50px;
  background: white;
  display: flex;
  align-items: center;
  /* justify-content: center;  移除居中，改为默认的 flex-start 或 space-between */
  padding: 10px 12px 10px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.back-btn {
  width: 40px;
  height: 40px;
  border: none;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  margin-right: 15px;
  transition: opacity 0.2s, transform 0.1s;
  padding: 0;
  flex-shrink: 0; /* 防止压缩 */
}

/* ... back-btn styles unchanged ... */
.back-btn .icon {
  font-size: 24px;
  color: #333333;
}

.back-btn:hover {
  opacity: 0.8;
}

.back-btn:active {
  transform: scale(0.95);
}

.search-input-wrapper {
  flex: 1; /* 改为自适应宽度 */
  /* width: 300px;  移除固定宽度 */
  height: 40px;
  background: #FFFFFF;
  border: 1px solid #E0E0E0;
  border-radius: 20px;
  display: flex;
  align-items: center;
  padding: 0;
  transition: border-color 0.2s;
  box-sizing: border-box;
  margin-right: 15px; /* 右侧留出间距给搜索按钮 */
}

.search-input-wrapper:focus-within {
  border-color: #FF6700;
}

.search-icon {
  font-size: 16px;
  color: #999999;
  margin-left: 15px;
  margin-right: 8px;
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  font-size: 14px;
  color: #333;
  outline: none;
  height: 100%;
}

.search-input::placeholder {
  color: #999999;
}

.clear-icon {
  font-size: 16px;
  color: #ccc;
  padding: 4px;
  margin-right: 10px;
  cursor: pointer;
}

.search-btn {
  display: block; /* 显示搜索按钮 */
  font-size: 16px;
  color: #333;
  font-weight: 500;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0 5px;
  white-space: nowrap; /* 防止换行 */
}

.filter-bar {
  height: 40px;
  background: white;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #f0f0f0;
}

.filter-item {
  flex: 1;
  text-align: center;
  font-size: 14px;
  color: #666;
}

.filter-item.active {
  color: #333;
  font-weight: bold;
}

.sort-icon {
  font-size: 12px;
  margin-left: 2px;
}

.product-list-content {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.product-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.product-card {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.image-wrapper {
  width: 100%;
  aspect-ratio: 1;
  background: #f9f9f9;
}

.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-info {
  padding: 8px;
}

.product-title {
  font-size: 14px;
  color: #333;
  line-height: 1.4;
  height: 40px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

@media (max-width: 375px) {
  .search-input-wrapper {
    width: 240px; /* 小屏幕适配 */
  }
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 6px;
}

.tag {
  font-size: 10px;
  color: #ff2442;
  border: 1px solid rgba(255, 36, 66, 0.2);
  padding: 0 4px;
  border-radius: 2px;
}

.price-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 4px;
}

.price {
  color: #ff2442;
  font-size: 16px;
  font-weight: bold;
}

.symbol {
  font-size: 12px;
}

.sales {
  font-size: 11px;
  color: #999;
}

.shop-row {
  display: flex;
  align-items: center;
  font-size: 11px;
  color: #999;
}

.shop-name {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 100px;
  color: #999;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 10px;
}

.spinner {
  width: 24px;
  height: 24px;
  border: 2px solid #eee;
  border-top: 2px solid #999;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 10px;
}

.load-more {
  text-align: center;
  padding: 15px 0;
  font-size: 12px;
  color: #999;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>
