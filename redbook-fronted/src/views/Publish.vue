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

        <!-- 关联商品（可选） -->
        <div class="form-group">
          <label class="form-label">
            关联商品 <span class="optional-hint" style="color: #999; font-size: 14px; font-weight: normal;">（可选）</span>
          </label>

          <div v-if="selectedProduct" class="linked-product-card">
            <img
              class="linked-product-image"
              :src="getImageUrl(selectedProduct.mainImage || selectedProduct.image)"
              alt="商品图"
            />
            <div class="linked-product-info">
              <div class="linked-product-name">{{ selectedProduct.name || '未命名商品' }}</div>
              <div class="linked-product-price" v-if="selectedProduct.price != null">¥{{ selectedProduct.price }}</div>
            </div>
            <div class="linked-product-actions">
              <button
                type="button"
                class="linked-product-action linked-product-action--primary"
                @click="openProductModal"
                :disabled="isEditMode"
              >
                更换
              </button>
              <button
                type="button"
                class="linked-product-action"
                @click="clearProductLink"
                :disabled="isEditMode"
              >
                取消关联
              </button>
            </div>
          </div>

          <div v-else class="link-product-empty">
            <div class="link-product-hint">未关联商品</div>
            <button type="button" class="link-product-btn" @click="openProductModal" :disabled="isEditMode">
              选择商品
            </button>
          </div>

          <div v-if="isEditMode" class="link-product-tip">编辑模式暂不支持修改关联商品</div>
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

  <!-- 选择商品弹窗 -->
  <div v-if="productModalVisible" class="product-modal-mask" @click="closeProductModal">
    <div class="product-modal" @click.stop>
      <div class="product-modal-header">
        <div class="product-modal-title">选择要关联的商品</div>
        <button type="button" class="product-modal-close" @click="closeProductModal">×</button>
      </div>

      <div class="product-modal-search">
        <input
          v-model="productSearchKeyword"
          class="product-search-input"
          placeholder="搜索商品名称"
          @keyup.enter="handleProductSearch"
        />
        <button type="button" class="product-search-btn" @click="handleProductSearch" :disabled="productSearching">
          {{ productSearching ? '搜索中...' : '搜索' }}
        </button>
      </div>

      <div class="product-modal-body">
        <div v-if="productSearching" class="product-modal-loading">正在搜索...</div>
        <div v-else-if="productSearchTried && productSearchResults.length === 0" class="product-modal-empty">
          未找到相关商品
        </div>
        <div v-else class="product-result-list">
          <button
            v-for="p in productSearchResults"
            :key="p.id"
            type="button"
            class="product-result-item"
            :class="{ active: String(tempSelectedProduct?.id) === String(p.id) }"
            @click="tempSelectedProduct = p"
          >
            <img class="product-result-image" :src="getImageUrl(p.mainImage || p.image)" alt="商品图" />
            <div class="product-result-info">
              <div class="product-result-name">{{ p.name || '未命名商品' }}</div>
              <div class="product-result-price" v-if="p.price != null">¥{{ p.price }}</div>
            </div>
            <div class="product-result-check">
              <span v-if="String(tempSelectedProduct?.id) === String(p.id)">已选</span>
              <span v-else>选择</span>
            </div>
          </button>
        </div>
      </div>

      <div class="product-modal-footer">
        <button type="button" class="product-modal-btn product-modal-btn--cancel" @click="closeProductModal">
          取消
        </button>
        <button
          type="button"
          class="product-modal-btn product-modal-btn--confirm"
          :disabled="!tempSelectedProduct"
          @click="confirmProductSelection"
        >
          确认选择
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { postNote, updateNote, getNote } from '@/api/note'
import { searchProducts } from '@/api/product'
import { useModal } from '@/utils/modal'
import { getImageUrl } from '@/utils/image'

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
  latitude: '',
  productId: ''
})

// 关联商品相关状态
const productModalVisible = ref(false)
const productSearchKeyword = ref('')
const productSearching = ref(false)
const productSearchTried = ref(false)
const productSearchResults = ref([])
const tempSelectedProduct = ref(null)
const selectedProduct = ref(null)

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
        formData.value.productId = note.productId ? String(note.productId) : ''

        if (note.product) {
          selectedProduct.value = {
            ...note.product,
            id: note.product.id ?? note.productId
          }
        }
        
        if (note.image) {
          imagePreview.value = getImageUrl(note.image)
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

const openProductModal = () => {
  productModalVisible.value = true
  productSearchKeyword.value = ''
  productSearchResults.value = []
  productSearchTried.value = false
  tempSelectedProduct.value = selectedProduct.value
}

const closeProductModal = () => {
  productModalVisible.value = false
  tempSelectedProduct.value = null
}

const clearProductLink = async () => {
  const confirmed = await showConfirm('确定取消关联该商品吗？', '确认')
  if (!confirmed) return
  selectedProduct.value = null
  formData.value.productId = ''
}

const handleProductSearch = async () => {
  const keyword = productSearchKeyword.value.trim()
  if (!keyword) {
    await showAlert('请输入关键词再搜索', '提示')
    return
  }

  if (productSearching.value) return
  productSearching.value = true
  productSearchTried.value = true

  try {
    const res = await searchProducts({
      keyword,
      pageNum: 1,
      pageSize: 20
    })

    const list = Array.isArray(res) ? res : (res.list || res.records || res.data || [])
    productSearchResults.value = list
    if (tempSelectedProduct.value) {
      const hit = list.find(p => String(p?.id) === String(tempSelectedProduct.value?.id))
      if (!hit) {
        tempSelectedProduct.value = null
      }
    }
  } catch (e) {
    console.error('Search product failed', e)
    if (!e.isHandled) {
      await showAlert('搜索商品失败，请稍后重试', '错误')
    }
  } finally {
    productSearching.value = false
  }
}

const confirmProductSelection = () => {
  if (!tempSelectedProduct.value) return
  selectedProduct.value = tempSelectedProduct.value
  formData.value.productId = selectedProduct.value?.id ? String(selectedProduct.value.id) : ''
  closeProductModal()
}

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

    if (!isEditMode.value && formData.value.productId) {
      formDataToSend.append('productId', formData.value.productId)
    }

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
  if (formData.value.title || formData.value.content || imageFile.value || formData.value.productId) {
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

.linked-product-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid #eee;
  border-radius: 10px;
  background: #fff;
}

.linked-product-image {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
  border: 1px solid #f0f0f0;
}

.linked-product-info {
  flex: 1;
  min-width: 0;
}

.linked-product-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.linked-product-price {
  margin-top: 4px;
  font-size: 13px;
  color: #ff2442;
  font-weight: 600;
}

.linked-product-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.linked-product-action {
  padding: 6px 10px;
  border-radius: 8px;
  font-size: 12px;
  cursor: pointer;
  border: 1px solid #ddd;
  background: #fff;
  color: #333;
}

.linked-product-action--primary {
  border-color: #ff2442;
  color: #ff2442;
}

.linked-product-action:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.link-product-empty {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px;
  border: 1px dashed #ddd;
  border-radius: 10px;
  background: #fafafa;
}

.link-product-hint {
  color: #999;
  font-size: 13px;
}

.link-product-btn {
  padding: 8px 14px;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid #ff2442;
  background: #fff5f7;
  color: #ff2442;
}

.link-product-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.link-product-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
}

.product-modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  z-index: 1000;
}

.product-modal {
  width: 100%;
  max-width: 520px;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.product-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.product-modal-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.product-modal-close {
  border: none;
  background: none;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
  color: #999;
}

.product-modal-search {
  display: flex;
  gap: 10px;
  padding: 12px 16px;
  border-bottom: 1px solid #f5f5f5;
}

.product-search-input {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 10px;
  font-size: 14px;
}

.product-search-input:focus {
  outline: none;
  border-color: #ff2442;
}

.product-search-btn {
  padding: 10px 14px;
  border-radius: 10px;
  border: none;
  background: #ff2442;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  white-space: nowrap;
}

.product-search-btn:disabled {
  background: #ffb3c0;
  cursor: not-allowed;
}

.product-modal-body {
  max-height: 55vh;
  overflow: auto;
  padding: 12px 16px;
}

.product-modal-loading,
.product-modal-empty {
  padding: 18px 0;
  text-align: center;
  color: #999;
  font-size: 13px;
}

.product-result-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.product-result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border: 1px solid #eee;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  text-align: left;
}

.product-result-item.active {
  border-color: #ff2442;
  background: #fff5f7;
}

.product-result-image {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.product-result-info {
  flex: 1;
  min-width: 0;
}

.product-result-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.product-result-price {
  margin-top: 4px;
  font-size: 13px;
  color: #ff2442;
  font-weight: 600;
}

.product-result-check {
  font-size: 12px;
  color: #666;
  flex-shrink: 0;
}

.product-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
}

.product-modal-btn {
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 14px;
  cursor: pointer;
  border: 1px solid #ddd;
  background: #fff;
  color: #333;
}

.product-modal-btn--confirm {
  border: none;
  background: #ff2442;
  color: #fff;
}

.product-modal-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}
</style>
