<template>
  <Transition name="zoom">
    <div v-if="visible" class="modal-overlay" @click="handleClose">
      <div class="modal-container" @click.stop>
        <div class="modal-content">
          <!-- 左侧图片区域 -->
          <div class="media-section">
            <div v-if="loading" class="loading-indicator">
              <div class="spinner"></div>
            </div>
            <img 
              v-else
              :src="note.image || defaultImage" 
              class="note-image" 
              alt="笔记图片"
            />
          </div>

          <!-- 右侧信息区域 -->
          <div class="info-section">
            <!-- 顶部用户信息 -->
            <div class="header-section">
              <div class="user-info">
                <img :src="note.user?.image || defaultAvatar" class="avatar" alt="头像" />
                <span class="username">{{ note.user?.nickname || '用户' + note.user?.id }}</span>
                <button 
                  v-if="!isSelf"
                  class="follow-btn" 
                  :class="{ 'followed': isFollowed }"
                  @click="handleFollow"
                >
                  {{ isFollowed ? '已关注' : '关注' }}
                </button>
              </div>
              <button class="close-btn" @click="handleClose">×</button>
            </div>

            <!-- 笔记内容 -->
            <div class="scroll-container">
              <div class="note-content">
                <h2 class="note-title">{{ note.title }}</h2>
                <p class="note-desc">{{ note.content }}</p>
                <div class="note-meta">
                  <span class="date">{{ note.time }}</span>
                  <span class="location" v-if="note.address">{{ note.address }}</span>
                </div>
              </div>

              <div class="divider"></div>

              <!-- 评论区 -->
              <div class="comments-section">
                <div class="comments-header">共 {{ comments.length }} 条评论</div>
                
                <div v-if="commentsLoading" class="comments-loading">
                  加载评论中...
                </div>
                
                <div v-else-if="comments.length === 0" class="empty-comments">
                  暂无评论，快来抢沙发吧~
                </div>

                <div v-else class="comment-list">
                  <div v-for="comment in comments" :key="comment.id" class="comment-item-container">
                    <div class="comment-item">
                      <img :src="comment.user?.image || defaultAvatar" class="comment-avatar" />
                      <div class="comment-content-wrapper">
                        <div class="comment-user">{{ comment.user?.nickname || '用户' }}</div>
                        <div class="comment-text">{{ comment.content }}</div>
                        <div class="comment-footer">
                          <div class="comment-meta">{{ comment.dealTime || comment.time }}</div>
                          <div class="comment-actions">
                            <div class="action-item" @click="handleCommentLike(comment)">
                              <i class="icon" :class="{ 'active': comment.isLiked }">
                                {{ comment.isLiked ? '❤️' : '🤍' }}
                              </i>
                              <span>{{ comment.likeCount || 0 }}</span>
                            </div>
                            <div class="action-item" @click="handleReply(comment)">
                              <i class="icon">💬</i>
                              <span>{{ comment.childrenList?.length || 0 }}</span>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                    
                    <!-- 子评论列表 -->
                    <div v-if="comment.childrenList && comment.childrenList.length" class="sub-comments">
                      <div v-for="child in comment.childrenList.slice(0, comment.shownCount || 1)" :key="child.id" class="comment-item sub-comment-item">
                        <img :src="child.user?.image || defaultAvatar" class="comment-avatar small" />
                        <div class="comment-content-wrapper">
                          <div class="comment-user">
                            {{ child.user?.nickname || '用户' }}
                            <span v-if="child.replyToUser" class="reply-target">
                              回复 <span class="target-name">{{ child.replyToUser }}</span>
                            </span>
                          </div>
                          <div class="comment-text">{{ child.content }}</div>
                          <div class="comment-footer">
                            <div class="comment-meta">{{ child.dealTime || child.time }}</div>
                            <div class="comment-actions">
                              <div class="action-item" @click="handleCommentLike(child)">
                                <i class="icon" :class="{ 'active': child.isLiked }">
                                  {{ child.isLiked ? '❤️' : '🤍' }}
                                </i>
                                <span>{{ child.likeCount || 0 }}</span>
                              </div>
                              <div class="action-item" @click="handleReply(child)">
                                <i class="icon">💬</i>
                                <span>{{ child.childrenList?.length || 0 }}</span>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                      <!-- 展开按钮 -->
                      <div 
                        v-if="comment.childrenList.length > 1 && (comment.shownCount || 1) < comment.childrenList.length" 
                        class="expand-btn"
                        @click="expandComments(comment)"
                      >
                        展开更多回复
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 底部互动区 (可选) -->
            <div class="footer-section">
              <div class="comment-input-wrapper" :class="{ 'expanded': isInputFocused }">
                <input 
                  v-model="commentInput" 
                  type="text" 
                  :placeholder="inputPlaceholder" 
                  @keyup.enter="handlePostComment"
                  @focus="handleFocus"
                  class="comment-input"
                />
              </div>
              
              <div class="action-bar" v-if="!isInputFocused">
                <div class="action-item" @click="handleLike">
                  <i class="icon" :class="{ 'active': isLiked }">
                    {{ isLiked ? '❤️' : '🤍' }}
                  </i>
                  <span>{{ note.like || 0 }}</span>
                </div>
                <div class="action-item" @click="handleCollect">
                  <i class="icon" :class="{ 'active': isCollected }">
                    {{ isCollected ? '⭐' : '☆' }}
                  </i>
                  <span>{{ note.collection || 0 }}</span>
                </div>
                <div class="action-item">
                  <i class="icon">💬</i>
                  <span>{{ note.comment || 0 }}</span>
                </div>
                <div class="action-item share-btn">
                  <i class="icon">↗️</i>
                </div>
              </div>

              <div class="focused-actions" v-else>
                <button class="action-btn send-btn" @click="handlePostComment" :disabled="!commentInput.trim()">发送</button>
                <button class="action-btn cancel-btn" @click="handleCancel">取消</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, watch, onMounted, nextTick } from 'vue'
import { getNote, likeNote, isLike, collectNote, isCollection } from '@/api/note'
import { getCommentList, postComment, likeComment, unlikeComment } from '@/api/comment'
import { isAttention, toggleAttention } from '@/api/user'
import { useUserStore } from '@/store/user'

const props = defineProps({
  visible: Boolean,
  noteId: [Number, String]
})

const emit = defineEmits(['update:visible'])

const note = ref({})
const comments = ref([])
const loading = ref(false)
const commentsLoading = ref(false)
const isFollowed = ref(false)
const isSelf = ref(false)
const isLiked = ref(false)
const isCollected = ref(false)
const isInputFocused = ref(false)
const commentInput = ref('')
const replyingTo = ref(null) // 当前正在回复的评论（或子评论）
const inputPlaceholder = ref('说点什么...')
const defaultAvatar = 'https://via.placeholder.com/40x40/ff2442/ffffff?text=U'
const defaultImage = 'https://via.placeholder.com/600x800/f0f0f0/999999?text=Loading'

const handleClose = () => {
  emit('update:visible', false)
}

const checkFollowStatus = async (userId) => {
  if (!userId) return
  try {
    const res = await isAttention(userId)
    if (res === 0) {
      isSelf.value = true
      isFollowed.value = false
    } else if (res === 1) {
      isSelf.value = false
      isFollowed.value = true
    } else {
      isSelf.value = false
      isFollowed.value = false
    }
  } catch (error) {
    console.error('获取关注状态失败', error)
  }
}

const checkLikeAndCollectionStatus = async (id) => {
  try {
    const [likeRes, collectRes] = await Promise.all([
      isLike(id),
      isCollection(id)
    ])
    isLiked.value = !!likeRes
    isCollected.value = !!collectRes
  } catch (error) {
    console.error('获取点赞收藏状态失败', error)
  }
}

const handleFollow = async () => {
  if (isSelf.value) return
  try {
    const targetId = note.value.user?.id
    if (!targetId) return
    await toggleAttention(targetId)
    isFollowed.value = !isFollowed.value
  } catch (error) {
    console.error('操作失败', error)
  }
}

const handleLike = async () => {
  try {
    await likeNote(props.noteId)
    isLiked.value = !isLiked.value
    // 乐观更新 UI
    if (isLiked.value) {
      note.value.like = (note.value.like || 0) + 1
    } else {
      note.value.like = Math.max(0, (note.value.like || 0) - 1)
    }
  } catch (error) {
    console.error('点赞失败', error)
  }
}

const handleCollect = async () => {
  try {
    await collectNote(props.noteId)
    isCollected.value = !isCollected.value
    // 乐观更新 UI
    if (isCollected.value) {
      note.value.collection = (note.value.collection || 0) + 1
    } else {
      note.value.collection = Math.max(0, (note.value.collection || 0) - 1)
    }
  } catch (error) {
    console.error('收藏失败', error)
  }
}

const handleFocus = () => {
  isInputFocused.value = true
}

const handleCancel = () => {
  isInputFocused.value = false
  commentInput.value = ''
  replyingTo.value = null
  inputPlaceholder.value = '说点什么...'
}

const handleCommentLike = async (comment) => {
  try {
    if (comment.isLiked) {
      await unlikeComment(comment.id)
      comment.likeCount = Math.max(0, (comment.likeCount || 0) - 1)
      comment.isLiked = false
    } else {
      await likeComment(comment.id)
      comment.likeCount = (comment.likeCount || 0) + 1
      comment.isLiked = true
    }
  } catch (error) {
    console.error('操作失败', error)
  }
}

const handleReply = (comment) => {
  replyingTo.value = comment
  inputPlaceholder.value = `回复 ${comment.user?.nickname || '用户'}...`
  isInputFocused.value = true
  // 聚焦输入框
  nextTick(() => {
      document.querySelector('.comment-input')?.focus()
  })
}

const processComments = (list) => {
  if (!list) return []
  return list.map(comment => {
    if (comment.childrenList && comment.childrenList.length > 0) {
      // 按ID升序排序（最早发布的在前）
      comment.childrenList.sort((a, b) => a.id - b.id)
      // 初始化显示数量，默认为1
      comment.shownCount = 1
    }
    return comment
  })
}

const expandComments = (comment) => {
  if (!comment.shownCount) comment.shownCount = 1
  comment.shownCount += 5
}

const handlePostComment = async () => {
  const content = commentInput.value.trim()
  if (!content) return
  
  try {
    await postComment({
      noteId: props.noteId,
      content: content,
      parentId: replyingTo.value ? replyingTo.value.id : 0
    })
    
    commentInput.value = ''
    isInputFocused.value = false
    replyingTo.value = null
    inputPlaceholder.value = '说点什么...'
    
    // 重新加载评论
    const commentsRes = await getCommentList(props.noteId)
    comments.value = processComments(commentsRes)
    note.value.comment = (note.value.comment || 0) + 1
    
  } catch (error) {
    console.error('评论失败', error)
  }
}

const loadData = async () => {
  if (!props.noteId) return
  
  loading.value = true
  commentsLoading.value = true
  isFollowed.value = false
  isSelf.value = false
  isLiked.value = false
  isCollected.value = false
  isInputFocused.value = false
  commentInput.value = ''
  
  try {
    const [noteRes, commentsRes] = await Promise.all([
      getNote(props.noteId).catch(e => {
        console.error('获取笔记详情失败', e)
        return {}
      }),
      getCommentList(props.noteId).catch(e => {
        console.error('获取评论失败', e)
        return []
      })
    ])
    
    note.value = noteRes || {}
    comments.value = processComments(commentsRes)
    
    if (note.value.user?.id) {
      await checkFollowStatus(note.value.user.id)
    }
    await checkLikeAndCollectionStatus(props.noteId)
    
  } catch (error) {
    console.error('加载详情失败', error)
  } finally {
    loading.value = false
    commentsLoading.value = false
  }
}

watch(() => props.visible, (newVal) => {
  if (newVal) {
    loadData()
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
    // 清空数据，防止下次打开闪烁旧数据
    setTimeout(() => {
      note.value = {}
      comments.value = []
    }, 300)
  }
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
  backdrop-filter: blur(4px);
}

.expand-btn {
  font-size: 13px;
  color: #13386c;
  cursor: pointer;
  margin-top: 8px;
  margin-left: 40px;
  font-weight: 500;
}
.expand-btn:hover {
  color: #3c6ba6;
}

.modal-container {
  width: 80%;
  height: 90%;
  max-width: 1200px;
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  transform-origin: center center;
  will-change: transform, opacity;
}

.modal-content {
  display: flex;
  width: 100%;
  height: 100%;
}

/* 左侧图片区域 */
.media-section {
  width: 50%; /* 修改为50%宽度 */
  height: 100%;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  flex-shrink: 0; /* 防止被压缩 */
}

.note-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

/* 右侧信息区域 */
.info-section {
  width: 50%; /* 修改为50%宽度 */
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  position: relative;
}

.header-section {
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #eee;
}

.username {
  font-weight: 600;
  font-size: 16px;
  color: #333;
}

.follow-btn {
  padding: 6px 16px;
  border-radius: 20px;
  border: 1px solid #ff2442;
  color: #ff2442;
  background: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  margin-left: 10px;
}

.follow-btn:hover {
  background: #fff5f6;
}

.follow-btn.followed {
  background: #f0f0f0;
  color: #999;
  border-color: #ddd;
}

.follow-btn.followed:hover {
  background: #e0e0e0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  color: #999;
  cursor: pointer;
  line-height: 1;
  padding: 0 8px;
}

.close-btn:hover {
  color: #333;
}

.scroll-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.note-content {
  margin-bottom: 24px;
}

.note-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #333;
  line-height: 1.4;
}

.note-desc {
  font-size: 15px;
  color: #333;
  line-height: 1.6;
  white-space: pre-wrap;
  margin-bottom: 12px;
}

.note-meta {
  font-size: 12px;
  color: #999;
  display: flex;
  gap: 12px;
}

.divider {
  height: 1px;
  background: #f0f0f0;
  margin: 20px -20px;
}

/* 评论区 */
.comments-header {
  font-size: 14px;
  color: #666;
  margin-bottom: 16px;
}

.comment-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.comment-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.comment-content-wrapper {
  flex: 1;
}

.comment-user {
  font-size: 13px;
  color: #999;
  margin-bottom: 4px;
}

.comment-text {
  font-size: 14px;
  color: #333;
  line-height: 1.5;
  margin-bottom: 4px;
}

.comment-meta {
  font-size: 12px;
  color: #bbb;
}

.footer-section {
  padding: 12px 24px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.comment-input-wrapper {
  flex: 1;
  transition: all 0.3s ease;
}

.comment-input-wrapper.expanded {
  flex: 1; /* 保持 flex: 1，但可以调整内部样式 */
}

.comment-input {
  width: 100%;
  padding: 10px 16px;
  border-radius: 20px;
  border: 1px solid transparent;
  background: #f5f5f5;
  font-size: 14px;
  color: #333;
  outline: none;
  transition: all 0.3s ease;
}

.comment-input:focus {
  background: #fff;
  border-color: #ff2442;
}

.comment-input::placeholder {
  color: #999;
}

.action-bar {
  display: flex;
  gap: 20px;
  align-items: center;
  flex-shrink: 0;
}

.focused-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-shrink: 0;
  animation: fadeIn 0.2s ease;
}

.action-btn {
  padding: 6px 16px;
  border-radius: 18px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.send-btn {
  background: #ff2442;
  color: white;
}

.send-btn:disabled {
  background: #ffb8c2;
  cursor: not-allowed;
}

.comment-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;
}

.comment-actions {
  display: flex;
  gap: 16px;
  align-items: center;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  transition: color 0.2s;
}

.action-item:hover {
  color: #333;
}

.action-item .icon {
  font-size: 14px;
  font-style: normal;
}

.action-item .icon.active {
  color: #ff2442;
}

.sub-comments {
  margin-left: 44px;
  margin-top: 12px;
}

.comment-avatar.small {
  width: 24px;
  height: 24px;
}

.sub-comment-item {
  margin-bottom: 12px;
}

.reply-target {
  color: #999;
  margin-left: 4px;
  font-weight: normal;
}

.target-name {
  color: #666;
  font-weight: 500;
}

.comment-item-container {
    margin-bottom: 20px;
}
</style>
