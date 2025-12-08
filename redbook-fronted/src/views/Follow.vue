<template>
  <div class="explore-container">
    <div class="explore-header">
      <h2 class="explore-title">关注动态</h2>
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
      <p class="empty-title">你还没有关注任何人</p>
      <p class="empty-subtitle">
        关注更多用户，发现精彩内容
      </p>
    </div>

    <!-- 笔记详情模态框 -->
    <NoteDetailModal
      v-model:visible="showNoteDetail"
      :note-id="currentNoteId"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import PostCard from '@/components/PostCard.vue'
import NoteDetailModal from '@/components/NoteDetailModal.vue'
import { getNoteListByAttention } from '@/api/note'
import { getImageUrl } from '@/utils/image'

const router = useRouter()
const userStore = useUserStore()

const posts = ref([])
const loading = ref(false)
const error = ref('')

// 笔记详情控制
const showNoteDetail = ref(false)
const currentNoteId = ref(null)

// 转换后端数据为前端需要的格式
const transformNoteData = (noteVo) => {
  return {
    id: noteVo.id,
    title: noteVo.title || '无标题',
    image: getImageUrl(noteVo.image, 'https://via.placeholder.com/300x400/f0f0f0/999999?text=小红书'),
    likes: noteVo.like || 0,
    comments: noteVo.comment || 0,
    collects: noteVo.collection || 0,
    author: {
      id: noteVo.user?.id,
      name: noteVo.user?.nickname || '匿名用户',
      avatar: getImageUrl(noteVo.user?.image, 'https://via.placeholder.com/32x32/ff2442/ffffff?text=U')
    },
    content: noteVo.content,
    time: noteVo.dealTime || noteVo.time,
    distance: noteVo.distance,
    address: noteVo.address
  }
}

// 加载笔记列表
const loadPosts = async (isRefresh = false) => {
  if (loading.value) return

  loading.value = true
  error.value = ''

  try {
    // 检查是否登录
    if (!userStore.isLoggedIn) {
      error.value = '请先登录查看关注内容'
      return
    }

    const response = await getNoteListByAttention()

    console.log('关注列表响应:', response)

    if (response && Array.isArray(response)) {
      const transformedPosts = response.map(transformNoteData)
      posts.value = transformedPosts
    } else {
      posts.value = []
    }
  } catch (err) {
    console.error('加载关注列表失败:', err)
    error.value = err.message || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

// 重试加载
const retryLoad = () => {
  error.value = ''
  loadPosts(true)
}

// 点击帖子
const handlePostClick = (post) => {
  console.log('点击帖子:', post)
  currentNoteId.value = post.id
  showNoteDetail.value = true
}

onMounted(() => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
  } else {
    loadPosts()
  }
})
</script>

<style scoped>
.explore-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.explore-header {
  margin-bottom: 20px;
}

.explore-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

.posts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
}

.loading-container {
  text-align: center;
  padding: 40px 0;
  color: #999;
}

.loading-spinner {
  width: 30px;
  height: 30px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #ff2442;
  border-radius: 50%;
  margin: 0 auto 10px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-message {
  text-align: center;
  padding: 40px 0;
  color: #666;
}

.retry-btn {
  margin-top: 10px;
  padding: 8px 20px;
  background: #ff2442;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
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

.empty-title {
  font-size: 16px;
  color: #333;
  margin-bottom: 8px;
}

.empty-subtitle {
  font-size: 14px;
  color: #999;
}
</style>
