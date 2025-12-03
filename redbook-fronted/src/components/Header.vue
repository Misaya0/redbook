<template>
  <header class="header">
    <div class="header-container">
      <div class="logo-section">
        <div class="logo">
          <span class="logo-text">小红书</span>
        </div>
        <div class="search-section">
          <div class="search-box">
            <input
              type="text"
              placeholder="搜索用户、笔记、商品"
              class="search-input"
              v-model="searchKeyword"
              @keyup.enter="handleSearch"
            />
            <button class="search-btn" @click="handleSearch">
              <i class="search-icon">🔍</i>
            </button>
          </div>
        </div>
      </div>

      <nav class="nav-section">
        <a href="#" class="nav-item active">探索</a>
        <a href="#" class="nav-item">关注</a>
        <a href="#" class="nav-item">消息</a>
        <a href="#" class="nav-item">我</a>
      </nav>

      <div class="user-section">
        <button class="publish-btn" @click="handlePublish">
          <i class="publish-icon">+</i>
          发布
        </button>
        <div class="user-avatar" @click="handleAvatarClick">
          <img :src="userAvatar" alt="用户头像" />
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const searchKeyword = ref('')

// 获取用户头像
const userAvatar = computed(() => {
  return userStore.userInfo?.image || 'https://via.placeholder.com/32x32/ff2442/ffffff?text=U'
})

const handleSearch = () => {
  console.log('搜索:', searchKeyword.value)
}

// 点击头像跳转到个人中心
const handleAvatarClick = () => {
  if (userStore.isLoggedIn) {
    router.push('/profile')
  } else {
    router.push('/login')
  }
}

// 点击发布按钮跳转到发布页面
const handlePublish = () => {
  if (userStore.isLoggedIn) {
    router.push('/publish')
  } else {
    alert('请先登录')
    router.push('/login')
  }
}
</script>

<style lang="scss" scoped>
.header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #e5e5e5;
  z-index: 1000;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.header-container {
  max-width: 1200px;
  height: 100%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 30px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo-img {
  height: 32px;
  width: auto;
}

.logo-text {
  font-size: 24px;
  font-weight: bold;
  color: #ff2442;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.search-section {
  flex: 1;
  max-width: 400px;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
  background: #f5f5f5;
  border-radius: 20px;
  padding: 8px 16px;
  transition: all 0.3s ease;

  &:hover {
    background: #e8e8e8;
  }

  &:focus-within {
    background: #fff;
    box-shadow: 0 0 0 2px #ff2442;
  }
}

.search-input {
  flex: 1;
  border: none;
  background: none;
  outline: none;
  font-size: 14px;
  color: #333;

  &::placeholder {
    color: #999;
  }
}

.search-btn {
  border: none;
  background: none;
  cursor: pointer;
  padding: 4px;
  color: #666;

  &:hover {
    color: #ff2442;
  }
}

.search-icon {
  font-size: 16px;
}

.nav-section {
  display: flex;
  align-items: center;
  gap: 30px;
}

.nav-item {
  text-decoration: none;
  color: #666;
  font-size: 16px;
  font-weight: 500;
  padding: 8px 16px;
  border-radius: 20px;
  transition: all 0.3s ease;

  &:hover {
    color: #ff2442;
    background: #fef5f5;
  }

  &.active {
    color: #ff2442;
    background: #fef5f5;
  }
}

.user-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.publish-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #ff2442;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    background: #e01e3a;
    transform: translateY(-1px);
  }
}

.publish-icon {
  font-size: 16px;
  font-weight: bold;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    transform: scale(1.1);
  }

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

@media (max-width: 768px) {
  .header-container {
    padding: 0 16px;
  }

  .logo-section {
    gap: 16px;
  }

  .search-section {
    display: none;
  }

  .nav-section {
    gap: 16px;
  }

  .nav-item {
    font-size: 14px;
    padding: 6px 12px;
  }
}
</style>