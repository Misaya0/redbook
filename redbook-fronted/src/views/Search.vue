<template>
  <div class="search-page">
    <!-- Tabs -->
    <div class="search-tabs">
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 0 }"
        @click="handleTabChange(0)"
      >
        笔记
      </div>
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 1 }"
        @click="handleTabChange(1)"
      >
        用户
      </div>
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 2 }"
        @click="handleTabChange(2)"
      >
        商品
      </div>
    </div>

    <!-- Content -->
    <div class="search-content" v-loading="loading">
      <div v-if="loading" class="loading-container">
        <div class="spinner"></div>
        <p>搜索中...</p>
      </div>

      <div v-else-if="results.length === 0" class="empty-state">
        <div class="empty-icon">🔍</div>
        <p>未找到相关内容</p>
      </div>

      <div v-else>
        <!-- Note Results -->
        <div v-if="activeTab === 0" class="note-grid">
          <PostCard 
            v-for="note in formattedNotes" 
            :key="note.id" 
            :post="note"
            @click="handleNoteClick"
          />
          <NoteDetailModal
            v-model:visible="showNoteDetail"
            :note-id="currentNoteId"
          />
        </div>

        <!-- User Results -->
        <div v-if="activeTab === 1" class="user-list">
          <div v-for="user in results" :key="user.id" class="user-item" @click="navigateToUser(user.id)">
            <img :src="user.image || defaultAvatar" class="user-avatar" />
            <div class="user-info">
              <div class="user-name" v-html="highlight(user.nickname)"></div>
              <div class="user-id">ID: {{ user.number }}</div>
            </div>
            <button class="follow-btn">关注</button>
          </div>
        </div>

        <!-- Product Results -->
        <div v-if="activeTab === 2" class="product-grid">
          <div v-for="product in results" :key="product.id" class="product-item">
            <img :src="product.image" class="product-image" />
            <div class="product-info">
              <div class="product-name" v-html="highlight(product.name)"></div>
              <div class="product-price">¥{{ product.price }}</div>
              <div class="product-shop">{{ product.shopName || '未知店铺' }}</div>
            </div>
          </div>
        </div>

        <!-- Pagination -->
        <div class="pagination" v-if="results.length > 0">
          <button :disabled="page <= 1" @click="handlePageChange(page - 1)">上一页</button>
          <span>第 {{ page }} 页</span>
          <button :disabled="results.length < size" @click="handlePageChange(page + 1)">下一页</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchApi } from '@/api/search'
import PostCard from '@/components/PostCard.vue'
import NoteDetailModal from '@/components/NoteDetailModal.vue'

const route = useRoute()
const router = useRouter()

const activeTab = ref(0)
const loading = ref(false)
const results = ref([])
const page = ref(1)
const size = ref(20)
const defaultAvatar = 'https://via.placeholder.com/50'
const showNoteDetail = ref(false)
const currentNoteId = ref(null)

const keyword = computed(() => route.query.keyword || '')

// Watch for keyword or tab changes
watch(
  [() => route.query.keyword, activeTab],
  () => {
    page.value = 1
    fetchResults()
  }
)

const formattedNotes = computed(() => {
  if (activeTab.value !== 0) return []
  return results.value.map(note => ({
    id: note.id,
    title: note.title, // Contains HTML for highlight
    image: note.image?.split(',')[0] || '',
    likes: note.like || 0,
    author: {
      name: note.user?.nickname || '未知用户',
      avatar: note.user?.image || defaultAvatar,
      id: note.user?.id || note.userId
    }
  }))
})

const fetchResults = async () => {
  if (!keyword.value) return
  
  loading.value = true
  try {
    const res = await searchApi.searchAll({
      keyword: keyword.value,
      type: activeTab.value,
      page: page.value,
      size: size.value
    })
    results.value = Array.isArray(res) ? res : []
  } catch (error) {
    console.error('Search failed:', error)
    results.value = []
  } finally {
    loading.value = false
  }
}

const handleTabChange = (tab) => {
  activeTab.value = tab
}

const handlePageChange = (newPage) => {
  page.value = newPage
  fetchResults()
  window.scrollTo(0, 0)
}

const handleNoteClick = (note) => {
  // 打开帖子详情弹窗
  currentNoteId.value = note.id
  showNoteDetail.value = true
}

const navigateToUser = (userId) => {
  if (!userId) return
  router.push(`/user/${userId}`)
}

const highlight = (text) => {
  if (!text) return ''
  if (!keyword.value) return text
  // 转义正则表达式特殊字符
  const escapedKeyword = keyword.value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  try {
    const reg = new RegExp(escapedKeyword, 'gi')
    return text.replace(reg, match => `<span style="color: red">${match}</span>`)
  } catch (e) {
    return text
  }
}

onMounted(() => {
  if (keyword.value) {
    fetchResults()
  }
})
</script>

<style lang="scss" scoped>
.search-page {
  padding-top: 80px;
  max-width: 1200px;
  margin: 0 auto;
  min-height: 100vh;
}

.search-tabs {
  display: flex;
  gap: 30px;
  margin-bottom: 20px;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
  
  .tab-item {
    cursor: pointer;
    font-size: 16px;
    color: #666;
    padding: 5px 10px;
    
    &.active {
      color: #333;
      font-weight: bold;
      border-bottom: 2px solid #ff2442;
    }
  }
}

.loading-container, .empty-state {
  text-align: center;
  padding: 50px 0;
  color: #999;
  
  .spinner {
    border: 4px solid #f3f3f3;
    border-top: 4px solid #ff2442;
    border-radius: 50%;
    width: 30px;
    height: 30px;
    animation: spin 1s linear infinite;
    margin: 0 auto 10px;
  }
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.note-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
}

.user-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
  
  .user-item {
    display: flex;
    align-items: center;
    padding: 15px;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0,0,0,0.05);
    cursor: pointer;
    
    .user-avatar {
      width: 50px;
      height: 50px;
      border-radius: 50%;
      margin-right: 15px;
    }
    
    .user-info {
      flex: 1;
      
      .user-name {
        font-weight: bold;
        margin-bottom: 5px;
      }
      
      .user-id {
        font-size: 12px;
        color: #999;
      }
    }
    
    .follow-btn {
      padding: 6px 15px;
      border-radius: 20px;
      border: 1px solid #ff2442;
      color: #ff2442;
      background: #fff;
      cursor: pointer;
      
      &:hover {
        background: #fff5f5;
      }
    }
  }
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
  
  .product-item {
    background: #fff;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    transition: transform 0.2s;
    
    &:hover {
      transform: translateY(-2px);
    }
    
    .product-image {
      width: 100%;
      aspect-ratio: 1;
      object-fit: cover;
    }
    
    .product-info {
      padding: 10px;
      
      .product-name {
        font-size: 14px;
        margin-bottom: 5px;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }
      
      .product-price {
        color: #ff2442;
        font-weight: bold;
        font-size: 16px;
      }
      
      .product-shop {
        font-size: 12px;
        color: #999;
        margin-top: 5px;
      }
    }
  }
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 30px;
  gap: 15px;
  
  button {
    padding: 8px 16px;
    border: 1px solid #ddd;
    background: #fff;
    border-radius: 4px;
    cursor: pointer;
    
    &:disabled {
      color: #ccc;
      cursor: not-allowed;
    }
    
    &:not(:disabled):hover {
      border-color: #ff2442;
      color: #ff2442;
    }
  }
}
</style>
