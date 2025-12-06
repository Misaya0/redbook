<template>
  <div class="explore-container">
    <div class="explore-header">
      <h2 class="explore-title">探索精彩内容</h2>
      <div class="filter-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          :class="['tab-btn', { active: currentTab === tab.value }]"
          @click="switchTab(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>
    </div>

    <div v-if="error" class="error-message">
      <p>{{ error }}</p>
      <button @click="retryLoad" class="retry-btn">重试</button>
    </div>

    <div v-else class="posts-grid">
      <PostCard
        v-for="post in posts"
        :key="post.id"
        :post="post"
        @click="handlePostClick"
      />
    </div>

    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>正在加载更多内容...</p>
    </div>

    <div v-if="!loading && posts.length === 0 && !error" class="empty-state">
      <div class="empty-icon">📭</div>
      <p class="empty-title">{{ getEmptyMessage() }}</p>
    </div>

    <!-- 笔记详情模态框 -->
    <NoteDetailModal
      v-model:visible="showNoteDetail"
      :note-id="currentNoteId"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import PostCard from '@/components/PostCard.vue'
import NoteDetailModal from '@/components/NoteDetailModal.vue'
import { getNoteList } from '@/api/note'

const router = useRouter()
const userStore = useUserStore()

const posts = ref([])
const loading = ref(false)
const error = ref('')
const currentPage = ref(1)
const pageSize = ref(12)
const hasMore = ref(true)
const currentTab = ref('recommend')

// 笔记详情控制
const showNoteDetail = ref(false)
const currentNoteId = ref(null)

const tabs = [
  { label: '推荐', value: 'recommend' },
  { label: '情感', value: '情感' },
  { label: '体育', value: '体育' },
  { label: '穿搭', value: '穿搭' },
  { label: '美食', value: '美食' },
  { label: '旅行', value: '旅行' },
  { label: '搞笑', value: '搞笑' },
  { label: '音乐', value: '音乐' },
  { label: '职场', value: '职场' },
  { label: '其他', value: '其他' }
]

// 转换后端数据为前端需要的格式
const transformNoteData = (noteVo) => {
  return {
    id: noteVo.id,
    title: noteVo.title || '无标题',
    image: noteVo.image || 'https://via.placeholder.com/300x400/f0f0f0/999999?text=小红书',
    likes: noteVo.like || 0,
    comments: noteVo.comment || 0,
    collects: noteVo.collection || 0,
    author: {
      id: noteVo.user?.id,
      name: noteVo.user?.nickname || '匿名用户',
      avatar: noteVo.user?.image || 'https://via.placeholder.com/32x32/ff2442/ffffff?text=U'
    },
    content: noteVo.content,
    time: noteVo.dealTime || noteVo.time,
    distance: noteVo.distance,
    address: noteVo.address
  }
}

// 加载笔记列表
const loadPosts = async (isRefresh = false) => {
  if (loading.value || (!hasMore.value && !isRefresh)) return

  loading.value = true
  error.value = ''

  try {
    let response
    const type = currentTab.value === 'recommend' ? null : currentTab.value
    response = await getNoteList(currentPage.value, pageSize.value, type)

    console.log('笔记列表响应:', response)

    if (response && Array.isArray(response)) {
      const transformedPosts = response.map(transformNoteData)

      if (isRefresh) {
        posts.value = transformedPosts
      } else {
        posts.value.push(...transformedPosts)
      }

      // 判断是否还有更多数据
      if (response.length < pageSize.value) {
        hasMore.value = false
      } else {
        currentPage.value++
      }
    } else {
      // 如果没有数据，显示空状态
      if (isRefresh) {
        posts.value = []
      }
      hasMore.value = false
    }
  } catch (err) {
    console.error('加载笔记列表失败:', err)
    error.value = err.message || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

// 切换标签
const switchTab = (tab) => {
  if (currentTab.value === tab) return

  currentTab.value = tab
  currentPage.value = 1
  hasMore.value = true
  posts.value = []
  loadPosts(true)
}

// 重试加载
const retryLoad = () => {
  error.value = ''
  loadPosts(true)
}

// 点击帖子
const handlePostClick = (post) => {
  console.log('点击帖子:', post)
  // 打开帖子详情弹窗
  currentNoteId.value = post.id
  showNoteDetail.value = true
}

// 获取空状态提示信息
const getEmptyMessage = () => {
  return '暂无内容'
}

// 滚动加载
const handleScroll = () => {
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop
  const windowHeight = window.innerHeight
  const documentHeight = document.documentElement.scrollHeight

  if (scrollTop + windowHeight >= documentHeight - 200 && !loading.value && hasMore.value) {
    loadPosts()
  }
}

onMounted(() => {
  loadPosts(true)
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.explore-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.explore-header {
  margin-bottom: 30px;
}

.explore-title {
  color: #ff2442;
  text-align: center;
  margin-bottom: 20px;
  font-size: 28px;
  font-weight: bold;
}

.filter-tabs {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-bottom: 20px;
}

.tab-btn {
  padding: 8px 24px;
  border: none;
  background: #f5f5f5;
  color: #666;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
}

.tab-btn:hover {
  background: #ffe8e8;
  color: #ff2442;
}

.tab-btn.active {
  background: #ff2442;
  color: white;
}

.posts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.loading-container {
  text-align: center;
  padding: 40px 20px;
  color: #666;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  margin: 0 auto 10px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #ff2442;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-message {
  text-align: center;
  padding: 40px 20px;
  color: #ff2442;
}

.retry-btn {
  margin-top: 10px;
  padding: 8px 24px;
  background: #ff2442;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  transition: background 0.3s ease;
}

.retry-btn:hover {
  background: #e01e3a;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #999;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.empty-title {
  font-size: 18px;
  color: #666;
  margin-bottom: 10px;
  font-weight: 500;
}

.empty-subtitle {
  font-size: 14px;
  color: #999;
}

@media (max-width: 1200px) {
  .posts-grid {
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 16px;
  }
}

@media (max-width: 768px) {
  .explore-container {
    padding: 15px;
  }

  .explore-title {
    font-size: 24px;
  }

  .posts-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 12px;
  }

  .filter-tabs {
    gap: 8px;
  }

  .tab-btn {
    padding: 6px 16px;
    font-size: 13px;
  }
}
</style>