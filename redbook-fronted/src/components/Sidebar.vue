<template>
  <aside class="sidebar">
    <div class="sidebar-section">
      <h3 class="section-title">推荐关注</h3>
      <div class="user-list">
        <div
          v-for="user in recommendedUsers"
          :key="user.id"
          class="user-item"
        >
          <div class="user-info">
            <img
              :src="user.avatar"
              :alt="user.name"
              class="user-avatar"
            />
            <div class="user-details">
              <div class="user-name">{{ user.name }}</div>
              <div class="user-desc">{{ user.description }}</div>
            </div>
          </div>
          <button
            class="follow-btn"
            :class="{ followed: user.isFollowed }"
            @click="toggleFollow(user)"
          >
            {{ user.isFollowed ? '已关注' : '关注' }}
          </button>
        </div>
      </div>
    </div>

    <div class="sidebar-section">
      <h3 class="section-title">热门话题</h3>
      <div class="topic-list">
        <div
          v-for="topic in hotTopics"
          :key="topic.id"
          class="topic-item"
          @click="handleTopicClick(topic)"
        >
          <span class="topic-tag">#{{ topic.name }}</span>
          <span class="topic-count">{{ formatNumber(topic.posts) }}篇笔记</span>
        </div>
      </div>
    </div>

    <div class="sidebar-section">
      <h3 class="section-title">推荐频道</h3>
      <div class="channel-list">
        <div
          v-for="channel in channels"
          :key="channel.id"
          class="channel-item"
          @click="handleChannelClick(channel)"
        >
          <img
            :src="channel.icon"
            :alt="channel.name"
            class="channel-icon"
          />
          <span class="channel-name">{{ channel.name }}</span>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { ref } from 'vue'

const recommendedUsers = ref([
  {
    id: 1,
    name: '美妆达人小雅',
    avatar: 'https://via.placeholder.com/40x40/ff6b6b/ffffff?text=美',
    description: '分享日常美妆心得',
    isFollowed: false
  },
  {
    id: 2,
    name: '旅行摄影师',
    avatar: 'https://via.placeholder.com/40x40/4ecdc4/ffffff?text=旅',
    description: '记录美好旅程',
    isFollowed: true
  },
  {
    id: 3,
    name: '美食博主',
    avatar: 'https://via.placeholder.com/40x40/45b7d1/ffffff?text=食',
    description: '探索城市美食',
    isFollowed: false
  }
])

const hotTopics = ref([
  { id: 1, name: '秋冬穿搭', posts: 12500 },
  { id: 2, name: '护肤心得', posts: 8900 },
  { id: 3, name: '美食探店', posts: 15600 },
  { id: 4, name: '旅行攻略', posts: 9800 },
  { id: 5, name: '家居好物', posts: 7200 }
])

const channels = ref([
  { id: 1, name: '时尚穿搭', icon: 'https://via.placeholder.com/24x24/ff2442/ffffff?text=F' },
  { id: 2, name: '美妆护肤', icon: 'https://via.placeholder.com/24x24/ff6b6b/ffffff?text=M' },
  { id: 3, name: '美食', icon: 'https://via.placeholder.com/24x24/ffa502/ffffff?text=F' },
  { id: 4, name: '旅行', icon: 'https://via.placeholder.com/24x24/4ecdc4/ffffff?text=T' },
  { id: 5, name: '家居', icon: 'https://via.placeholder.com/24x24/45b7d1/ffffff?text=H' },
  { id: 6, name: '健身', icon: 'https://via.placeholder.com/24x24/a55eea/ffffff?text=J' }
])

const toggleFollow = (user) => {
  user.isFollowed = !user.isFollowed
}

const handleTopicClick = (topic) => {
  console.log('点击话题:', topic.name)
}

const handleChannelClick = (channel) => {
  console.log('点击频道:', channel.name)
}

const formatNumber = (num) => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  } else if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}
</script>

<style lang="scss" scoped>
.sidebar {
  width: 280px;
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 80px;
  height: fit-content;
}

.sidebar-section {
  margin-bottom: 24px;

  &:last-child {
    margin-bottom: 0;
  }
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.user-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.user-details {
  flex: 1;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 2px;
}

.user-desc {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.follow-btn {
  padding: 6px 12px;
  border: 1px solid #ff2442;
  background: #fff;
  color: #ff2442;
  border-radius: 16px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    background: #fef5f5;
  }

  &.followed {
    background: #f0f0f0;
    border-color: #ccc;
    color: #666;
  }
}

.topic-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.topic-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    background: #f8f8f8;
    border-radius: 6px;
    padding: 8px 8px;
  }
}

.topic-tag {
  font-size: 14px;
  color: #ff2442;
  font-weight: 500;
}

.topic-count {
  font-size: 12px;
  color: #999;
}

.channel-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.channel-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    background: #f8f8f8;
  }
}

.channel-icon {
  width: 24px;
  height: 24px;
  border-radius: 6px;
}

.channel-name {
  font-size: 12px;
  color: #666;
}

@media (max-width: 1024px) {
  .sidebar {
    display: none;
  }
}
</style>