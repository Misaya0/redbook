<template>
  <div class="publish-container">
    <div class="publish-content">
      <div class="publish-header">
        <button class="back-btn" @click="handleBack">
          <span>←</span> 返回
        </button>
        <h1 class="page-title">发布笔记</h1>
        <button class="publish-submit-btn" @click="handlePublish" :disabled="!canPublish || publishing">
          {{ publishing ? '发布中...' : '发布' }}
        </button>
      </div>

      <div class="publish-form">
        <!-- 图片上传区域 -->
        <div class="image-upload-section">
          <div class="upload-label">
            <span class="required">*</span> 上传图片
          </div>
          <div class="image-upload-area">
            <div v-if="!imagePreview" class="upload-placeholder" @click="triggerFileInput">
              <div class="upload-icon">📷</div>
              <p class="upload-text">点击上传图片</p>
              <p class="upload-hint">支持 JPG、PNG 格式，最大 10MB</p>
            </div>
            <div v-else class="image-preview-container">
              <img :src="imagePreview" alt="预览图" class="image-preview" />
              <div class="image-actions">
                <button class="change-image-btn" @click="triggerFileInput">更换图片</button>
                <button class="remove-image-btn" @click="removeImage">删除图片</button>
              </div>
            </div>
            <input
              ref="fileInput"
              type="file"
              accept="image/jpeg,image/png,image/jpg"
              @change="handleFileChange"
              style="display: none"
            />
          </div>
        </div>

        <!-- 标题输入 -->
        <div class="form-group">
          <label class="form-label">
            <span class="required">*</span> 标题
          </label>
          <input
            v-model="formData.title"
            type="text"
            class="form-input"
            placeholder="请输入标题（最多50字）"
            maxlength="50"
          />
          <div class="char-count">{{ formData.title.length }}/50</div>
        </div>

        <!-- 内容输入 -->
        <div class="form-group">
          <label class="form-label">
            <span class="required">*</span> 内容
          </label>
          <textarea
            v-model="formData.content"
            class="form-textarea"
            placeholder="分享你的精彩内容...（最多1000字）"
            maxlength="1000"
            rows="10"
          ></textarea>
          <div class="char-count">{{ formData.content.length }}/1000</div>
        </div>

        <!-- 类型选择 -->
        <div class="form-group">
          <label class="form-label">
            <span class="required">*</span> 类型
          </label>
          <div class="type-options">
            <button
              v-for="type in noteTypes"
              :key="type.value"
              :class="['type-btn', { active: formData.type === type.value }]"
              @click="formData.type = type.value"
            >
              <span class="type-icon">{{ type.icon }}</span>
              <span class="type-label">{{ type.label }}</span>
            </button>
          </div>
        </div>

        <!-- 位置信息（可选） -->
        <div class="form-group">
          <label class="form-label">位置（可选）</label>
          <div class="location-input">
            <input
              v-model="formData.address"
              type="text"
              class="form-input"
              placeholder="添加位置信息"
            />
            <button class="location-btn" @click="getLocation">
              📍 获取当前位置
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { postNote } from '@/api/note'

const router = useRouter()
const userStore = useUserStore()

const fileInput = ref(null)
const imageFile = ref(null)
const imagePreview = ref('')
const publishing = ref(false)

const formData = ref({
  title: '',
  content: '',
  type: '',
  address: '',
  longitude: '',
  latitude: ''
})

const noteTypes = [
  { value: '穿搭', label: '穿搭', icon: '👗' },
  { value: '美妆', label: '美妆', icon: '💄' },
  { value: '美食', label: '美食', icon: '🍔' },
  { value: '旅行', label: '旅行', icon: '✈️' },
  { value: '家居', label: '家居', icon: '🏠' },
  { value: '健身', label: '健身', icon: '💪' },
  { value: '摄影', label: '摄影', icon: '📷' },
  { value: '其他', label: '其他', icon: '📝' }
]

// 是否可以发布
const canPublish = computed(() => {
  return (
    imageFile.value &&
    formData.value.title.trim() &&
    formData.value.content.trim() &&
    formData.value.type
  )
})

// 触发文件选择
const triggerFileInput = () => {
  fileInput.value.click()
}

// 处理文件选择
const handleFileChange = (event) => {
  const file = event.target.files[0]
  if (!file) return

  // 验证文件类型
  if (!file.type.match(/^image\/(jpeg|png|jpg)$/)) {
    alert('只支持 JPG、PNG 格式的图片')
    return
  }

  // 验证文件大小（10MB）
  if (file.size > 10 * 1024 * 1024) {
    alert('图片大小不能超过 10MB')
    return
  }

  imageFile.value = file

  // 生成预览
  const reader = new FileReader()
  reader.onload = (e) => {
    imagePreview.value = e.target.result
  }
  reader.readAsDataURL(file)
}

// 删除图片
const removeImage = () => {
  imageFile.value = null
  imagePreview.value = ''
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

// 获取位置
const getLocation = () => {
  if (!navigator.geolocation) {
    alert('您的浏览器不支持地理定位')
    return
  }

  navigator.geolocation.getCurrentPosition(
    (position) => {
      formData.value.longitude = position.coords.longitude.toString()
      formData.value.latitude = position.coords.latitude.toString()
      formData.value.address = `经度: ${position.coords.longitude.toFixed(6)}, 纬度: ${position.coords.latitude.toFixed(6)}`
      alert('位置获取成功')
    },
    (error) => {
      console.error('获取位置失败:', error)
      alert('获取位置失败，请手动输入')
    }
  )
}

// 发布笔记
const handlePublish = async () => {
  if (!userStore.isLoggedIn) {
    alert('请先登录')
    router.push('/login')
    return
  }

  if (!canPublish.value) {
    alert('请填写完整信息')
    return
  }

  publishing.value = true

  try {
    const formDataToSend = new FormData()
    formDataToSend.append('image', imageFile.value)
    formDataToSend.append('title', formData.value.title)
    formDataToSend.append('content', formData.value.content)
    formDataToSend.append('type', formData.value.type)
    formDataToSend.append('longitude', formData.value.longitude || '0')
    formDataToSend.append('latitude', formData.value.latitude || '0')

    console.log('发布笔记:', {
      title: formData.value.title,
      content: formData.value.content,
      type: formData.value.type
    })

    await postNote(formDataToSend)

    alert('发布成功！')
    router.push('/')
  } catch (error) {
    console.error('发布失败:', error)
    alert(error.message || '发布失败，请稍后重试')
  } finally {
    publishing.value = false
  }
}

// 返回
const handleBack = () => {
  if (formData.value.title || formData.value.content || imageFile.value) {
    if (confirm('确定要放弃编辑吗？')) {
      router.back()
    }
  } else {
    router.back()
  }
}
</script>

<style scoped>
.publish-container {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 80px 20px 20px;
}

.publish-content {
  max-width: 800px;
  margin: 0 auto;
}

.publish-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 30px;
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #f5f5f5;
  border: none;
  border-radius: 8px;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background: #e5e5e5;
  color: #333;
}

.page-title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.publish-submit-btn {
  padding: 10px 32px;
  background: #ff2442;
  color: white;
  border: none;
  border-radius: 20px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.publish-submit-btn:hover:not(:disabled) {
  background: #e01e3a;
  transform: translateY(-1px);
}

.publish-submit-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
  transform: none;
}

.publish-form {
  background: white;
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.image-upload-section {
  margin-bottom: 30px;
}

.upload-label {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 12px;
}

.required {
  color: #ff2442;
  margin-right: 4px;
}

.image-upload-area {
  width: 100%;
}

.upload-placeholder {
  width: 100%;
  height: 300px;
  border: 2px dashed #ddd;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.upload-placeholder:hover {
  border-color: #ff2442;
  background: #fef5f5;
}

.upload-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.upload-text {
  font-size: 16px;
  color: #333;
  margin-bottom: 8px;
}

.upload-hint {
  font-size: 14px;
  color: #999;
}

.image-preview-container {
  position: relative;
}

.image-preview {
  width: 100%;
  max-height: 500px;
  object-fit: contain;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.image-actions {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}

.change-image-btn,
.remove-image-btn {
  flex: 1;
  padding: 10px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.change-image-btn {
  background: #f5f5f5;
  color: #333;
}

.change-image-btn:hover {
  background: #e5e5e5;
}

.remove-image-btn {
  background: #fff;
  color: #ff2442;
  border: 1px solid #ff2442;
}

.remove-image-btn:hover {
  background: #ff2442;
  color: white;
}

.form-group {
  margin-bottom: 24px;
}

.form-label {
  display: block;
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 12px;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: all 0.3s ease;
}

.form-input:focus {
  border-color: #ff2442;
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.form-textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  resize: vertical;
  font-family: inherit;
  transition: all 0.3s ease;
}

.form-textarea:focus {
  border-color: #ff2442;
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.char-count {
  text-align: right;
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.type-options {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
}

.type-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  background: #f5f5f5;
  border: 2px solid transparent;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.type-btn:hover {
  background: #ffe8e8;
  border-color: #ff2442;
}

.type-btn.active {
  background: #fff;
  border-color: #ff2442;
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.type-icon {
  font-size: 32px;
}

.type-label {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.location-input {
  display: flex;
  gap: 12px;
}

.location-btn {
  padding: 12px 20px;
  background: #f5f5f5;
  border: none;
  border-radius: 8px;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.3s ease;
}

.location-btn:hover {
  background: #e5e5e5;
  color: #333;
}

@media (max-width: 768px) {
  .publish-container {
    padding: 70px 10px 10px;
  }

  .publish-header {
    padding: 15px;
  }

  .page-title {
    font-size: 20px;
  }

  .publish-submit-btn {
    padding: 8px 20px;
    font-size: 14px;
  }

  .publish-form {
    padding: 20px;
  }

  .type-options {
    grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
    gap: 8px;
  }

  .type-btn {
    padding: 12px;
  }

  .type-icon {
    font-size: 24px;
  }

  .location-input {
    flex-direction: column;
  }

  .location-btn {
    width: 100%;
  }
}
</style>
