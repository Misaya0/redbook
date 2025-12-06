<template>
  <div class="profile-container">
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="error" class="error-container">
      <p class="error-message">{{ error }}</p>
      <button @click="loadUserInfo" class="retry-btn">重试</button>
    </div>

    <div v-else class="profile-content">
      <!-- 用户信息卡片 -->
      <div class="user-card">
        <div class="user-header">
          <div class="avatar-section">
            <img :src="userInfo.image || defaultAvatar" alt="用户头像" class="user-avatar" />
            <button class="edit-avatar-btn" @click="handleEditAvatar">
              <span>📷</span>
            </button>
          </div>

          <div class="user-info">
            <h1 class="user-nickname">{{ userInfo.nickname || '未设置昵称' }}</h1>
            <p class="user-id">小红书号：{{ userInfo.number || '未设置' }}</p>
            <p class="user-phone" v-if="userInfo.phone">手机号：{{ maskPhone(userInfo.phone) }}</p>
          </div>

          <button class="edit-profile-btn" @click="handleEditProfile">
            编辑资料
          </button>
        </div>

        <!-- 统计信息 -->
        <div class="stats-section">
          <div class="stat-item" @click="showAttentionList">
            <div class="stat-number">{{ attentionCount }}</div>
            <div class="stat-label">关注</div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item" @click="showFansList">
            <div class="stat-number">{{ fansCount }}</div>
            <div class="stat-label">粉丝</div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-number">{{ likeCount }}</div>
            <div class="stat-label">获赞与收藏</div>
          </div>
        </div>

        <!-- 详细信息 -->
        <div class="detail-section">
          <div class="detail-item" v-if="userInfo.sex">
            <span class="detail-label">性别</span>
            <span class="detail-value">{{ userInfo.sex }}</span>
          </div>
          <div class="detail-item" v-if="userInfo.birthday">
            <span class="detail-label">生日</span>
            <span class="detail-value">{{ userInfo.birthday }}</span>
          </div>
          <div class="detail-item" v-if="userInfo.address">
            <span class="detail-label">地区</span>
            <span class="detail-value">{{ userInfo.address }}</span>
          </div>
          <div class="detail-item" v-if="userInfo.identity">
            <span class="detail-label">身份</span>
            <span class="detail-value">{{ userInfo.identity }}</span>
          </div>
          <div class="detail-item" v-if="userInfo.school">
            <span class="detail-label">学校</span>
            <span class="detail-value">{{ userInfo.school }}</span>
          </div>
          <div class="detail-item" v-if="userInfo.time">
            <span class="detail-label">注册时间</span>
            <span class="detail-value">{{ userInfo.time }}</span>
          </div>
        </div>
      </div>

      <!-- 标签页 -->
      <div class="tabs-section">
        <div class="tabs-header">
          <div class="tabs">
            <button
              v-for="tab in tabs"
              :key="tab.value"
              :class="['tab-btn', { active: currentTab === tab.value }]"
              @click="handleTabChange(tab.value)"
            >
              {{ tab.label }}
            </button>
          </div>
          
          <button 
            v-if="currentTab === 'notes'" 
            class="manage-btn" 
            @click="toggleManagementMode"
          >
            {{ isManagementMode ? '退出管理' : '笔记管理' }}
          </button>
        </div>

        <div class="tab-content">
          <!-- 加载状态 -->
          <div v-if="tabLoading" class="tab-loading">
            <div class="loading-spinner"></div>
            <p>加载中...</p>
          </div>

          <!-- 笔记列表 -->
          <div v-else-if="currentTab === 'notes'" class="notes-container">
            <transition name="fade" mode="out-in">
              <!-- 普通视图 -->
              <div v-if="!isManagementMode" key="normal" class="notes-grid">
                <div v-if="notesList.length === 0" class="empty-tip">
                  <div class="empty-icon">📝</div>
                  <p>还没有发布笔记</p>
                </div>
                <div v-else class="posts-grid">
                  <PostCard
                    v-for="note in notesList"
                    :key="note.id"
                    :post="note"
                    @click="handleNoteClick"
                  />
                </div>
              </div>

              <!-- 管理视图 -->
              <div v-else key="management" class="management-list">
                <div v-if="notesList.length === 0" class="empty-tip">
                  <div class="empty-icon">📝</div>
                  <p>还没有发布笔记</p>
                </div>
                <div v-else class="management-items">
                  <div v-for="note in notesList" :key="note.id" class="management-item">
                    <!-- 左侧：缩略图 -->
                    <div class="item-left">
                      <img :src="note.image" class="item-thumb" alt="笔记缩略图" />
                    </div>
                    
                    <!-- 中间：信息 -->
                    <div class="item-center">
                      <h3 class="item-title">{{ note.title }}</h3>
                      <p class="item-time">{{ note.time }}</p>
                      <div class="item-stats">
                        <span class="stat-unit">
                          <span class="icon">💬</span> {{ note.comments }}
                        </span>
                        <span class="stat-unit">
                          <span class="icon">❤️</span> {{ note.likes }}
                        </span>
                        <span class="stat-unit">
                          <span class="icon">⭐</span> {{ note.collects }}
                        </span>
                      </div>
                    </div>

                    <!-- 右侧：操作 -->
                    <div class="item-right">
                      <button class="action-btn edit" @click.stop="handleEditNote(note)">
                        <span class="action-icon">✏️</span> 编辑
                      </button>
                      <button class="action-btn delete" @click.stop="handleDeleteNote(note)">
                        <span class="action-icon">🗑️</span> 删除
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </transition>
          </div>

          <!-- 点赞列表 -->
          <div v-else-if="currentTab === 'likes'" class="likes-grid">
            <div v-if="likesList.length === 0" class="empty-tip">
              <div class="empty-icon">❤️</div>
              <p>还没有点赞内容</p>
            </div>
            <div v-else class="posts-grid">
              <PostCard
                v-for="note in likesList"
                :key="note.id"
                :post="note"
                @click="handleNoteClick"
              />
            </div>
          </div>

          <!-- 收藏列表 -->
          <div v-else-if="currentTab === 'collections'" class="collections-grid">
            <div v-if="collectionsList.length === 0" class="empty-tip">
              <div class="empty-icon">⭐</div>
              <p>还没有收藏内容</p>
            </div>
            <div v-else class="posts-grid">
              <PostCard
                v-for="note in collectionsList"
                :key="note.id"
                :post="note"
                @click="handleNoteClick"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 弹窗组件 -->
    <div v-if="showDialog" class="modal-overlay" @click="closeDialog">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ dialogTitle }}</h3>
          <button class="close-btn" @click="closeDialog">×</button>
        </div>
        <div class="modal-body">
          <div v-if="dialogLoading" class="loading-spinner-small"></div>
          <div v-else-if="!dialogList || dialogList.length === 0" class="empty-text">暂无数据</div>
          <div v-else class="user-list">
            <div v-for="user in dialogList" :key="user.userId" class="user-item" @click="handleUserClick(user.userId)">
              <img :src="user.image || defaultAvatar" class="user-item-avatar" />
              <span class="user-item-name">{{ user.nickname || '用户' + user.userId }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Toast 提示 -->
    <div v-if="toast.visible" class="toast-message">
      {{ toast.message }}
    </div>

    <!-- 编辑资料弹窗 -->
    <div v-if="showEditDialog" class="modal-overlay" @click="showEditDialog = false">
      <div class="modal-content edit-profile-modal" @click.stop>
        <div class="modal-header">
          <h3>编辑资料</h3>
          <button class="close-btn" @click="showEditDialog = false">×</button>
        </div>
        <div class="modal-body">
          <div class="edit-form">
             <!-- Avatar Upload -->
            <div class="form-item avatar-upload-item">
              <div class="avatar-wrapper" @click="triggerFileInput">
                <img :src="editForm.image || defaultAvatar" class="edit-avatar-preview" />
                <div class="avatar-mask">
                  <span>📷</span>
                </div>
              </div>
              <input type="file" ref="fileInput" @change="handleFileChange" accept="image/*" style="display: none" />
            </div>

            <!-- Nickname -->
            <div class="form-item">
              <label>昵称</label>
              <input v-model="editForm.nickname" type="text" placeholder="请输入昵称" class="form-input" />
            </div>
            
            <!-- Sex -->
            <div class="form-item">
              <label>性别</label>
              <select v-model="editForm.sex" class="form-select">
                 <option value="">请选择性别</option>
                 <option value="男">男</option>
                 <option value="女">女</option>
              </select>
            </div>

             <!-- Birthday -->
            <div class="form-item">
              <label>生日</label>
              <input v-model="editForm.birthday" type="date" class="form-input" />
            </div>

            <!-- Address -->
            <div class="form-item">
              <label>地区</label>
              <input v-model="editForm.address" type="text" placeholder="请输入地区" class="form-input" />
            </div>

             <!-- Identity -->
            <div class="form-item">
              <label>身份</label>
              <input v-model="editForm.identity" type="text" placeholder="请输入身份/职业" class="form-input" />
            </div>

            <!-- School -->
            <div class="form-item">
              <label>学校</label>
              <input v-model="editForm.school" type="text" placeholder="请输入学校" class="form-input" />
            </div>
          </div>
        </div>
        <div class="modal-footer">
           <button class="cancel-btn" @click="showEditDialog = false">取消</button>
           <button class="save-btn" @click="handleSaveProfile" :disabled="editLoading">
             {{ editLoading ? '保存中...' : '保存' }}
           </button>
        </div>
      </div>
    </div>

    <!-- 笔记详情模态框 -->
    <NoteDetailModal
      v-model:visible="showNoteDetail"
      :note-id="currentNoteId"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getUserInfo, getAttentionList, getFansList, updateUserImage, editUserInfo } from '@/api/user'
import { getNoteListByOwn, getNoteByLike, getNoteByCollection, deleteNote } from '@/api/note'
import { useModal } from '@/utils/modal'
import PostCard from '@/components/PostCard.vue'
import NoteDetailModal from '@/components/NoteDetailModal.vue'

const router = useRouter()
const userStore = useUserStore()
const { showConfirm } = useModal()

const loading = ref(false)
const error = ref('')
const userInfo = ref({})
const attentionCount = ref(0)
const fansCount = ref(0)
const likeCount = ref(0)
const currentTab = ref('notes')
const isManagementMode = ref(false)
const tabLoading = ref(false)

// 笔记、点赞、收藏列表
const notesList = ref([])
const likesList = ref([])
const collectionsList = ref([])

const defaultAvatar = 'https://via.placeholder.com/120x120/ff2442/ffffff?text=U'

const tabs = [
  { label: '笔记', value: 'notes' },
  { label: '点赞', value: 'likes' },
  { label: '收藏', value: 'collections' }
]

// 弹窗相关状态
const showDialog = ref(false)
const dialogTitle = ref('')
const dialogList = ref([])
const dialogLoading = ref(false)

// 编辑资料相关状态
const showEditDialog = ref(false)
const editLoading = ref(false)
const editForm = ref({
  nickname: '',
  sex: '',
  birthday: '',
  address: '',
  identity: '',
  school: '',
  image: ''
})
const fileInput = ref(null)

// 笔记详情弹窗状态
const showNoteDetail = ref(false)
const currentNoteId = ref(null)

// Toast 状态
const toast = ref({
  visible: false,
  message: ''
})

// 显示 Toast
const showToast = (message) => {
  toast.value.message = message
  toast.value.visible = true
  setTimeout(() => {
    toast.value.visible = false
  }, 3000)
}

const closeDialog = () => {
  showDialog.value = false
  dialogList.value = []
}

const handleUserClick = (userId) => {
  console.log('点击用户:', userId)
  // TODO: 跳转到用户详情页，后续需实现 /user/:id 路由
  // router.push(`/user/${userId}`)
  closeDialog()
}

// 加载用户信息
const loadUserInfo = async (showLoading = true) => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }

  if (showLoading) {
    loading.value = true
  }
  error.value = ''

  try {
    const response = await getUserInfo()
    console.log('用户信息:', response)

    if (response) {
      userInfo.value = response
      // 同步更新到 store，确保 Header 组件头像也能更新
      userStore.userInfo = response
      
      // 加载关注列表获取关注数
      await loadAttentionCount()
      // 加载粉丝列表获取粉丝数
      await loadFansCount()
    } else {
      error.value = '获取用户信息失败'
    }
  } catch (err) {
    console.error('加载用户信息失败:', err)
    error.value = err.message || '加载失败，请稍后重试'
  } finally {
    if (showLoading) {
      loading.value = false
    }
  }
}

// 加载关注数
const loadAttentionCount = async () => {
  try {
    if (userInfo.value.id) {
      const attentions = await getAttentionList(userInfo.value.id)
      if (attentions && Array.isArray(attentions)) {
        attentionCount.value = attentions.length
      }
    }
  } catch (err) {
    console.error('加载关注数失败:', err)
  }
}

// 加载粉丝数
const loadFansCount = async () => {
  try {
    if (userInfo.value.id) {
      const fans = await getFansList(userInfo.value.id)
      if (fans && Array.isArray(fans)) {
        fansCount.value = fans.length
      }
    }
  } catch (err) {
    console.error('加载粉丝数失败:', err)
  }
}

// 手机号脱敏
const maskPhone = (phone) => {
  if (!phone) return ''
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

// 编辑头像
const handleEditAvatar = () => {
  // 直接打开编辑资料弹窗，并聚焦到头像上传部分（如果需要）
  // 或者直接触发文件选择（如果只想改头像）
  // 这里我们选择打开编辑弹窗
  handleEditProfile()
}

// 编辑资料
const handleEditProfile = () => {
  editForm.value = {
    ...userInfo.value,
    sex: userInfo.value.sex || '', // 确保 select 能正确绑定
    birthday: userInfo.value.birthday || '',
    image: userInfo.value.image || defaultAvatar
  }
  showEditDialog.value = true
}

// 触发文件选择
const triggerFileInput = () => {
  fileInput.value.click()
}

// 处理文件选择
const handleFileChange = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  
  // 验证文件类型和大小
  if (!file.type.startsWith('image/')) {
    showToast('请选择图片文件')
    return
  }
  
  if (file.size > 1 * 1024 * 1024) {
    showToast('图片大小不能超过1MB')
    return
  }
  
  try {
    // 可以先显示本地预览
    const reader = new FileReader()
    reader.onload = (e) => {
      editForm.value.image = e.target.result
    }
    reader.readAsDataURL(file)
    
    // 上传图片
    // 注意：后端 updateUserImage 接口不仅上传图片，还会直接更新用户头像字段
    // 如果我们只想在点击保存时才更新头像，这里应该只上传并获取URL
    // 但目前的接口逻辑是直接更新，所以这里上传成功后，相当于头像已经改了
    // 为了保持一致性，我们可以在这里上传，然后如果用户点击取消，可能需要 revert (比较复杂)
    // 或者接受“头像上传即生效”的逻辑，这在很多应用中也是常见的
    // 更好的做法是后端提供一个 uploadOnly 的接口，或者 editInfo 接受 image URL
    // 假设目前必须用 updateUserImage
    await updateUserImage(file)
    
    // 重新获取用户信息以确保同步
    await loadUserInfo(false)
    
  } catch (error) {
    console.error('上传头像失败:', error)
    if (!error.isHandled) {
      showToast('上传头像失败，请重试')
    }
  }
}

// 保存资料
const handleSaveProfile = async () => {
  if (!editForm.value.nickname) {
    showToast('昵称不能为空')
    return
  }
  
  editLoading.value = true
  try {
    // 构造提交的数据，过滤掉不需要的字段
    const dataToSubmit = {
      nickname: editForm.value.nickname,
      sex: editForm.value.sex,
      birthday: editForm.value.birthday,
      address: editForm.value.address,
      identity: editForm.value.identity,
      school: editForm.value.school,
      // image: editForm.value.image // 如果头像是独立接口上传的，这里可能不需要传，或者传 URL
    }
    
    await editUserInfo(dataToSubmit)
    
    // 更新成功，刷新用户信息
    await loadUserInfo(false)
    showEditDialog.value = false
    
  } catch (error) {
    console.error('保存资料失败:', error)
    if (!error.isHandled) {
      showToast('保存失败: ' + (error.message || '未知错误'))
    }
  } finally {
    editLoading.value = false
  }
}

// 显示关注列表
const showAttentionList = async () => {
  dialogTitle.value = '我的关注'
  showDialog.value = true
  dialogLoading.value = true
  try {
    const res = await getAttentionList(userInfo.value.id)
    dialogList.value = res || []
  } catch (e) {
    console.error(e)
    dialogList.value = []
  } finally {
    dialogLoading.value = false
  }
}

// 显示粉丝列表
const showFansList = async () => {
  dialogTitle.value = '我的粉丝'
  showDialog.value = true
  dialogLoading.value = true
  try {
    const res = await getFansList(userInfo.value.id)
    dialogList.value = res || []
  } catch (e) {
    console.error(e)
    dialogList.value = []
  } finally {
    dialogLoading.value = false
  }
}

// 转换笔记数据格式
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
    time: noteVo.dealTime || noteVo.time
  }
}

// 加载笔记列表
const loadNotes = async () => {
  tabLoading.value = true
  try {
    const response = await getNoteListByOwn()
    console.log('我的笔记:', response)
    if (response && Array.isArray(response)) {
      notesList.value = response.map(transformNoteData)
    } else {
      notesList.value = []
    }
  } catch (err) {
    console.error('加载笔记失败:', err)
    notesList.value = []
  } finally {
    tabLoading.value = false
  }
}

// 加载点赞列表
const loadLikes = async () => {
  tabLoading.value = true
  try {
    const response = await getNoteByLike()
    console.log('我的点赞:', response)
    if (response && Array.isArray(response)) {
      likesList.value = response.map(transformNoteData)
    } else {
      likesList.value = []
    }
  } catch (err) {
    console.error('加载点赞失败:', err)
    likesList.value = []
  } finally {
    tabLoading.value = false
  }
}

// 加载收藏列表
const loadCollections = async () => {
  tabLoading.value = true
  try {
    const response = await getNoteByCollection()
    console.log('我的收藏:', response)
    if (response && Array.isArray(response)) {
      collectionsList.value = response.map(transformNoteData)
    } else {
      collectionsList.value = []
    }
  } catch (err) {
    console.error('加载收藏失败:', err)
    collectionsList.value = []
  } finally {
    tabLoading.value = false
  }
}

// 点击笔记卡片
const handleNoteClick = (note) => {
  console.log('点击笔记:', note)
  currentNoteId.value = note.id
  showNoteDetail.value = true
}

// 切换标签
const handleTabChange = (tabValue) => {
  currentTab.value = tabValue
  isManagementMode.value = false
}

// 切换管理模式
const toggleManagementMode = () => {
  isManagementMode.value = !isManagementMode.value
}

// 编辑笔记
const handleEditNote = (note) => {
  console.log('Edit note:', note)
  router.push({ 
    path: '/publish', 
    query: { id: note.id } 
  })
}

// 删除笔记
const handleDeleteNote = async (note) => {
  const confirmed = await showConfirm('确定要删除这条笔记吗？', '删除确认')
  if (!confirmed) {
    return
  }
  
  try {
    await deleteNote(note.id)
    showToast('删除成功')
    
    // 从列表中移除
    notesList.value = notesList.value.filter(item => item.id !== note.id)
    
    // 也可以重新加载列表
    // loadNotes()
  } catch (error) {
    console.error('删除失败:', error)
    if (!error.isHandled) {
      showToast('删除失败: ' + (error.message || '未知错误'))
    }
  }
}

// 监听标签切换
watch(currentTab, (newTab) => {
  if (newTab === 'notes' && notesList.value.length === 0) {
    loadNotes()
  } else if (newTab === 'likes' && likesList.value.length === 0) {
    loadLikes()
  } else if (newTab === 'collections' && collectionsList.value.length === 0) {
    loadCollections()
  }
})

onMounted(() => {
  loadUserInfo()
  // 默认加载笔记列表
  loadNotes()
})
</script>

<style scoped>
.profile-container {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 80px 20px 20px;
}

.loading-container,
.error-container {
  text-align: center;
  padding: 60px 20px;
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
  color: #ff2442;
  margin-bottom: 20px;
}

.retry-btn {
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

.profile-content {
  max-width: 1000px;
  margin: 0 auto;
}

.user-card {
  background: white;
  border-radius: 16px;
  padding: 40px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.user-header {
  display: flex;
  align-items: flex-start;
  gap: 30px;
  margin-bottom: 30px;
  position: relative;
}

.avatar-section {
  position: relative;
}

.user-avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.edit-avatar-btn {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #ff2442;
  color: white;
  border: 3px solid white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  transition: all 0.3s ease;
}

.edit-avatar-btn:hover {
  background: #e01e3a;
  transform: scale(1.1);
}

.user-info {
  flex: 1;
}

.user-nickname {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
}

.user-id {
  font-size: 14px;
  color: #999;
  margin-bottom: 4px;
}

.user-phone {
  font-size: 14px;
  color: #999;
}

.edit-profile-btn {
  padding: 10px 24px;
  background: white;
  color: #ff2442;
  border: 2px solid #ff2442;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.edit-profile-btn:hover {
  background: #ff2442;
  color: white;
}

.stats-section {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 40px;
  margin-bottom: 30px;
}

.stat-item {
  text-align: center;
  cursor: pointer;
  transition: transform 0.2s;
}

.stat-item:hover {
  transform: translateY(-2px);
}

.stat-number {
  font-size: 20px;
  font-weight: bold;
  color: #333;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #999;
}

.toast-message {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  background-color: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 12px 24px;
  border-radius: 8px;
  z-index: 9999;
  font-size: 14px;
  animation: fadeIn 0.3s, fadeOut 0.3s 2.7s forwards;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translate(-50%, -20px); }
  to { opacity: 1; transform: translate(-50%, 0); }
}

@keyframes fadeOut {
  from { opacity: 1; }
  to { opacity: 0; }
}

.stat-divider {
  width: 1px;
  height: 20px;
  background: #eee;
}

.detail-section {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  padding-top: 20px;
  border-top: 1px solid #f5f5f5;
}

.detail-item {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.detail-label {
  color: #999;
  margin-right: 8px;
}

.detail-value {
  color: #333;
}

.tabs-section {
  background: white;
  border-radius: 16px;
  padding: 20px;
  min-height: 400px;
}

.tabs-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #eee;
  margin-bottom: 20px;
  padding-right: 10px;
}

.tabs {
  display: flex;
  gap: 30px;
}

.tab-btn {
  padding: 12px 4px;
  background: none;
  border: none;
  font-size: 16px;
  color: #666;
  cursor: pointer;
  position: relative;
  transition: all 0.3s;
}

.tab-btn.active {
  color: #333;
  font-weight: bold;
}

.tab-btn.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 100%;
  height: 2px;
  background: #ff2442;
}

.manage-btn {
  padding: 0 20px;
  background: white;
  color: #666;
  border: 1px solid #ddd;
  border-radius: 22px; /* Rounded pill shape */
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  height: 44px; /* Meets 44px touch target requirement */
  display: flex;
  align-items: center;
  justify-content: center;
}

.manage-btn:hover {
  color: #ff2442;
  border-color: #ff2442;
  background: #fff5f6;
}

/* Management List Styles */
.management-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.management-items {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.management-item {
  display: flex;
  padding: 15px;
  border: 1px solid #eee;
  border-radius: 12px;
  background: white;
  transition: all 0.3s ease;
}

.management-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transform: translateY(-2px);
}

.item-left {
  flex-shrink: 0;
  margin-right: 20px;
}

.item-thumb {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 8px;
}

.item-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 5px 0;
}

.item-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin: 0 0 8px 0;
  line-height: 1.4;
}

.item-time {
  font-size: 12px;
  color: #999;
  margin: 0;
}

.item-stats {
  display: flex;
  gap: 20px;
  margin-top: auto;
}

.stat-unit {
  font-size: 14px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 4px;
}

.item-right {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 15px;
  margin-left: 20px;
  padding-left: 20px;
  border-left: 1px solid #f5f5f5;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 20px;
  border: none;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
  min-width: 90px;
  justify-content: center;
}

.action-btn.edit {
  background: #f0f8ff;
  color: #007bff;
}

.action-btn.edit:hover {
  background: #e6f2ff;
}

.action-btn.delete {
  background: #fff0f0;
  color: #ff4d4f;
}

.action-btn.delete:hover {
  background: #ffe6e6;
}

/* Transition Styles */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

.posts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
}

.empty-tip {
  text-align: center;
  padding: 60px 0;
  color: #999;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.tab-loading {
  text-align: center;
  padding: 40px;
  color: #999;
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  width: 400px;
  max-height: 600px;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  padding: 0;
  line-height: 1;
}

.close-btn:hover {
  color: #666;
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  min-height: 200px;
}

.user-list {
  display: flex;
  flex-direction: column;
}

.user-item {
  display: flex;
  align-items: center;
  padding: 12px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.user-item:hover {
  background: #f5f5f5;
}

.user-item-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 12px;
  object-fit: cover;
  border: 1px solid #eee;
}

.user-item-name {
  font-size: 15px;
  color: #333;
  font-weight: 500;
}

/* Edit Profile Modal Styles */
.edit-profile-modal {
  width: 500px;
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 10px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.form-input,
.form-select {
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.3s;
}

.form-input:focus,
.form-select:focus {
  border-color: #ff2442;
}

.avatar-upload-item {
  align-items: center;
  margin-bottom: 10px;
}

.avatar-wrapper {
  position: relative;
  width: 100px;
  height: 100px;
  cursor: pointer;
}

.edit-avatar-preview {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #eee;
}

.avatar-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.avatar-wrapper:hover .avatar-mask {
  opacity: 1;
}

.avatar-mask span {
  font-size: 24px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.cancel-btn {
  padding: 8px 20px;
  background: #f5f5f5;
  color: #666;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.cancel-btn:hover {
  background: #e0e0e0;
}

.save-btn {
  padding: 8px 20px;
  background: #ff2442;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.save-btn:hover {
  background: #e01e3a;
}

.save-btn:disabled {
  background: #ffb3c0;
  cursor: not-allowed;
}

.loading-spinner-small {
  width: 24px;
  height: 24px;
  border: 2px solid #f3f3f3;
  border-top: 2px solid #ff2442;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 40px auto;
}

.empty-text {
  text-align: center;
  color: #999;
  padding: 40px 0;
}
</style>
