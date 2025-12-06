<template>
  <div class="publish-container">
    <div class="publish-content">
      <div class="publish-header">
        <button class="back-btn" @click="handleBack">
          <span>←</span> 返回
        </button>
        <h1 class="page-title">{{ isEditMode ? '编辑笔记' : '发布笔记' }}</h1>
        <button class="publish-submit-btn" @click="handlePublish" :disabled="!canPublish || publishing">
          {{ publishing ? (isEditMode ? '保存中...' : '发布中...') : (isEditMode ? '保存' : '发布') }}
        </button>
      </div>

      <div class="publish-form">
        <!-- 图片上传区域 -->
        <div class="image-upload-section">
          <div class="upload-label">
            上传图片 <span class="optional-hint" style="color: #999; font-size: 14px; font-weight: normal;">（可选）</span>
          </div>
          <div class="image-upload-area">
            <div v-if="!imagePreview && !generatedPreview" class="upload-placeholder" @click="triggerFileInput">
              <div class="upload-icon">📷</div>
              <p class="upload-text">点击上传图片</p>
              <p class="upload-hint">如不上传，将自动生成封面图片</p>
            </div>
            <div v-else class="image-preview-container">
              <img :src="imagePreview || generatedPreview" alt="预览图" class="image-preview" />
              <div class="image-actions">
                <template v-if="imagePreview">
                  <button class="change-image-btn" @click="triggerFileInput">更换图片</button>
                  <button class="remove-image-btn" @click="removeImage">删除图片</button>
                </template>
                <template v-else>
                  <button class="change-image-btn" @click="triggerFileInput">上传自定义图片</button>
                  <button class="change-image-btn" @click="generateCoverImage">重新生成封面</button>
                </template>
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
            类型 <span class="optional-hint" style="color: #999; font-size: 14px; font-weight: normal;">（可选）</span>
          </label>
          <div class="type-options">
            <button
              v-for="type in noteTypes"
              :key="type.value"
              :class="['type-btn', { active: formData.type === type.value }]"
              @click="formData.type = formData.type === type.value ? '' : type.value"
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
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { postNote, updateNote, getNote } from '@/api/note'
import { useModal } from '@/utils/modal'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const { showAlert, showConfirm } = useModal()

const isEditMode = computed(() => !!route.query.id)
const noteId = computed(() => route.query.id)

const fileInput = ref(null)
const imageFile = ref(null)
const imagePreview = ref('')
const generatedPreview = ref('')
const publishing = ref(false)

const formData = ref({
  title: '',
  content: '',
  type: '',
  address: '',
  longitude: '',
  latitude: ''
})

onMounted(async () => {
  if (isEditMode.value) {
    try {
      const res = await getNote(noteId.value)
      const note = res.data || res
      if (note) {
        formData.value.title = note.title || ''
        formData.value.content = note.content || ''
        formData.value.type = note.type || ''
        formData.value.address = note.address || ''
        formData.value.longitude = note.longitude ? String(note.longitude) : ''
        formData.value.latitude = note.latitude ? String(note.latitude) : ''
        
        if (note.image) {
          imagePreview.value = note.image
        }
      }
    } catch (e) {
      console.error('Load note failed', e)
      if (!e.isHandled) {
        await showAlert('加载笔记失败', '错误')
      }
    }
  }
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
    formData.value.title.trim() &&
    formData.value.content.trim()
  )
})

// 自动生成图片逻辑
const generateCoverImage = () => {
  const canvas = document.createElement('canvas')
  const width = 600
  const height = 800
  canvas.width = width
  canvas.height = height
  const ctx = canvas.getContext('2d')

  // 背景色
  const colors = ['#F5F5F5', '#F8F9FA', '#FFF5F5', '#F5F9FF', '#F0FFF4', '#FFF9F0']
  const color = colors[Math.floor(Math.random() * colors.length)]
  ctx.fillStyle = color
  ctx.fillRect(0, 0, width, height)

  // 文字
  ctx.fillStyle = '#000000'
  ctx.font = 'bold 32px sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'

  const text = formData.value.title.trim() || '无标题'
  const maxWidth = width - 120 // 左右各60px边距
  const words = text.split('')
  let line = ''
  let lines = []

  for (let n = 0; n < words.length; n++) {
    const testLine = line + words[n]
    const metrics = ctx.measureText(testLine)
    const testWidth = metrics.width
    if (testWidth > maxWidth && n > 0) {
      lines.push(line)
      line = words[n]
    } else {
      line = testLine
    }
  }
  lines.push(line)

  const lineHeight = 48
  const startY = (height - (lines.length * lineHeight)) / 2

  for (let i = 0; i < lines.length; i++) {
    ctx.fillText(lines[i], width / 2, startY + (i * lineHeight) + lineHeight / 2)
  }

  generatedPreview.value = canvas.toDataURL('image/jpeg', 0.8)
}

// 监听标题变化自动生成图片
watch(() => formData.value.title, (newTitle) => {
  if (!imageFile.value) {
    if (newTitle.trim()) {
      generateCoverImage()
    } else {
      generatedPreview.value = ''
    }
  }
})

// 监听图片文件变化
watch(imageFile, (newFile) => {
  if (!newFile && formData.value.title.trim()) {
    generateCoverImage()
  }
})

// DataURL转Blob
const dataURLtoBlob = (dataurl) => {
  let arr = dataurl.split(','), mime = arr[0].match(/:(.*?);/)[1],
    bstr = atob(arr[1]), n = bstr.length, u8arr = new Uint8Array(n);
  while (n--) {
    u8arr[n] = bstr.charCodeAt(n);
  }
  return new Blob([u8arr], { type: mime });
}

// 触发文件选择
const triggerFileInput = () => {
  fileInput.value.click()
}

// 处理文件选择
const handleFileChange = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  // 验证文件类型
  if (!file.type.match(/^image\/(jpeg|png|jpg)$/)) {
    await showAlert('只支持 JPG、PNG 格式的图片', '格式错误')
    return
  }

  // 验证文件大小（10MB）
  if (file.size > 10 * 1024 * 1024) {
    await showAlert('图片大小不能超过 10MB', '文件过大')
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
    showAlert('您的浏览器不支持地理定位', '定位失败')
    return
  }

  navigator.geolocation.getCurrentPosition(
    (position) => {
      formData.value.longitude = position.coords.longitude.toString()
      formData.value.latitude = position.coords.latitude.toString()
      formData.value.address = `经度: ${position.coords.longitude.toFixed(6)}, 纬度: ${position.coords.latitude.toFixed(6)}`
      showAlert('位置获取成功', '成功')
    },
    (error) => {
      console.error('获取位置失败:', error)
      showAlert('获取位置失败，请手动输入', '定位失败')
    }
  )
}

// 发布笔记
const handlePublish = async () => {
  if (!userStore.isLoggedIn) {
    await showAlert('请先登录', '提示')
    router.push('/login')
    return
  }

  if (!canPublish.value) {
    await showAlert('请填写完整信息', '提示')
    return
  }

  publishing.value = true

  try {
    const formDataToSend = new FormData()
    
    if (imageFile.value) {
      formDataToSend.append('image', imageFile.value)
    } else {
      // 如果没有上传图片，且没有现有图片预览（针对编辑模式），则使用自动生成的图片
      if (!imagePreview.value) {
        if (!generatedPreview.value && formData.value.title.trim()) {
          generateCoverImage()
        }
        if (generatedPreview.value) {
          const blob = dataURLtoBlob(generatedPreview.value)
          formDataToSend.append('image', blob, 'cover.jpg')
        }
      }
    }

    formDataToSend.append('title', formData.value.title)
    formDataToSend.append('content', formData.value.content)
    formDataToSend.append('type', formData.value.type)
    formDataToSend.append('longitude', formData.value.longitude || '0')
    formDataToSend.append('latitude', formData.value.latitude || '0')

    if (isEditMode.value) {
      formDataToSend.append('id', noteId.value)
      await updateNote(formDataToSend)
      await showAlert('修改成功！', '成功')
    } else {
      console.log('发布笔记:', {
        title: formData.value.title,
        content: formData.value.content,
        type: formData.value.type
      })
      await postNote(formDataToSend)
      await showAlert('发布成功！', '成功')
    }

    router.push('/')
  } catch (error) {
    console.error('发布失败:', error)
    if (!error.isHandled) {
      await showAlert(error.message || '发布失败，请稍后重试', '错误')
    }
  } finally {
    publishing.value = false
  }
}

// 返回
const handleBack = async () => {
  if (formData.value.title || formData.value.content || imageFile.value) {
    const confirmed = await showConfirm('确定要放弃编辑吗？', '确认')
    if (confirmed) {
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
  background-color: #fff;
  padding-bottom: 50px;
}

.publish-content {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  margin-top: 20px;
}

.publish-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  margin-bottom: 20px;
}

.back-btn {
  border: none;
  background: none;
  font-size: 16px;
  color: #333;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.publish-submit-btn {
  background-color: #ff2442;
  color: #fff;
  border: none;
  padding: 8px 20px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.3s;
}

.publish-submit-btn:disabled {
  background-color: #ffb3c0;
  cursor: not-allowed;
}

.publish-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.image-upload-area {
  width: 100%;
  min-height: 200px;
  background-color: #f8f8f8;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 1px dashed #ddd;
  transition: all 0.3s;
  overflow: hidden;
  position: relative;
}

.image-upload-area:hover {
  border-color: #ff2442;
  background-color: #fff5f7;
}

.upload-placeholder {
  text-align: center;
  color: #999;
}

.upload-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.upload-text {
  font-size: 14px;
  margin-bottom: 4px;
}

.upload-hint {
  font-size: 12px;
  color: #ccc;
}

.image-preview-container {
  width: 100%;
  position: relative;
  display: flex;
  justify-content: center;
  background: #000;
}

.image-preview {
  max-width: 100%;
  max-height: 400px;
  object-fit: contain;
  display: block;
}

.image-actions {
  position: absolute;
  bottom: 10px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  gap: 10px;
  background: rgba(0,0,0,0.5);
  padding: 10px;
}

.change-image-btn, .remove-image-btn {
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  border: none;
}

.change-image-btn {
  background: #fff;
  color: #333;
}

.remove-image-btn {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  border: 1px solid #fff;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.required {
  color: #ff2442;
  margin-right: 4px;
}

.form-input, .form-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  color: #333;
  transition: border-color 0.3s;
  box-sizing: border-box;
}

.form-input:focus, .form-textarea:focus {
  border-color: #ff2442;
  outline: none;
}

.form-textarea {
  resize: vertical;
  min-height: 120px;
}

.char-count {
  text-align: right;
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.type-options {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.type-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid #f0f0f0;
  border-radius: 20px;
  background: #f8f8f8;
  color: #666;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s;
}

.type-btn.active {
  background: #fff5f7;
  border-color: #ff2442;
  color: #ff2442;
}

.location-input {
  display: flex;
  gap: 10px;
}

.location-btn {
  white-space: nowrap;
  padding: 0 12px;
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 8px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
}

.location-btn:hover {
  color: #ff2442;
  border-color: #ff2442;
}
</style>
