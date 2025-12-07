<template>
  <div class="post-card" @click="handleClick">
    <div class="post-image-container">
      <img
        :src="post.image"
        :alt="post.title"
        class="post-image"
        @error="handleImageError"
      />
    </div>

    <div class="post-content">
      <div class="post-title" v-html="post.title"></div>
      <div class="post-footer">
        <div class="post-author" @click.stop="navigateToUser(post.author.id)">
          <img
            :src="post.author.avatar"
            :alt="post.author.name"
            class="author-avatar"
          />
          <span class="author-name">{{ post.author.name }}</span>
        </div>
        <div class="post-likes">
          <i class="icon">❤️</i>
          <span class="like-count">{{ formatNumber(post.likes) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  post: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['click'])
const router = useRouter()

const handleClick = () => {
  emit('click', props.post)
}

const navigateToUser = (userId) => {
  if (!userId) return
  router.push(`/user/${userId}`)
}

const handleImageError = (event) => {
  event.target.src = 'https://via.placeholder.com/300x400/f0f0f0/999999?text=小红书'
}

const formatNumber = (num) => {
  if (num === undefined || num === null) return '0'
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  } else if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}
</script>

<style lang="scss" scoped>
.post-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);

    .post-overlay {
      opacity: 1;
    }
  }
}

.post-image-container {
  position: relative;
  width: 100%;
  aspect-ratio: 3/4;
  overflow: hidden;
}

.post-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.post-card:hover .post-image {
  transform: scale(1.05);
}

.post-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(transparent 60%, rgba(0, 0, 0, 0.7));
  opacity: 0;
  transition: opacity 0.3s ease;
  display: flex;
  align-items: flex-end;
  padding: 16px;
}

.post-stats {
  display: flex;
  gap: 16px;
  color: white;
  font-size: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.icon {
  font-size: 14px;
}

.stat-count {
  font-weight: 500;
}

.post-content {
  padding: 12px;
}

.post-title {
  font-size: 14px;
  color: #333;
  line-height: 1.4;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  min-height: 40px;
}

.post-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.post-author {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0; /* 防止挤压 */
}

.author-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.author-name {
  font-size: 12px;
  color: #666;
  font-weight: 400;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.post-likes {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #666;
  font-size: 12px;
  margin-left: 8px;
}

.post-likes .icon {
  font-size: 14px;
}

.like-count {
  font-weight: 400;
}

@media (max-width: 768px) {
  .post-card {
    border-radius: 8px;
  }

  .post-stats {
    gap: 12px;
    font-size: 11px;
  }

  .post-content {
    padding: 10px;
  }

  .post-title {
    font-size: 13px;
    margin-bottom: 6px;
  }
}
</style>