<template>
  <div class="product-publish-container">
    <div class="publish-content">
      <div class="publish-header">
        <button class="back-btn" @click="handleBack">
          <span>←</span> 返回
        </button>
        <h1 class="page-title">{{ isEditMode ? '编辑商品' : '发布商品' }}</h1>
        <button class="publish-submit-btn" @click="handlePublish" :disabled="!canPublish || publishing">
          {{ publishing ? '提交中...' : (isEditMode ? '保存商品' : '发布商品') }}
        </button>
      </div>

      <div class="publish-form">
        <!-- 商品图片上传 -->
        <div class="image-upload-section">
          <div class="upload-label">
            商品主图 <span class="required">*</span>
          </div>
          <div class="image-upload-area">
            <div v-if="!form.mainImage" class="upload-placeholder" @click="triggerFileInput">
              <div class="upload-icon">📷</div>
              <p class="upload-text">上传主图</p>
              <p class="upload-hint">支持 JPG/PNG，建议尺寸 800x800</p>
            </div>
            <div v-else class="image-preview-container">
              <img :src="getImageUrl(form.mainImage)" class="image-preview" />
              <div class="image-actions">
                <button class="change-image-btn" @click="triggerFileInput">更换图片</button>
                <button class="remove-image-btn" @click="form.mainImage = ''">删除</button>
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

        <!-- 基本信息 -->
        <div class="form-group">
          <label class="form-label">
            <span class="required">*</span> 商品名称
          </label>
          <input
            v-model="form.name"
            type="text"
            class="form-input"
            placeholder="请输入商品名称（最多60字）"
            maxlength="60"
          />
        </div>

        <div class="form-group">
          <label class="form-label">
            副标题/卖点
          </label>
          <input
            v-model="form.title"
            type="text"
            class="form-input"
            placeholder="请输入商品卖点（选填）"
            maxlength="100"
          />
        </div>

        <div class="form-group">
          <label class="form-label">
            <span class="required">*</span> 商品分类
          </label>
          
          <!-- 一级分类 -->
          <select 
            v-model="selectedCategory1" 
            class="form-select mb-16"
            @change="handleCategory1Change"
          >
            <option :value="null" disabled>请选择一级分类</option>
            <option v-for="cat in categoryList1" :key="cat.id" :value="cat.id">
              {{ cat.name }}
            </option>
          </select>

          <!-- 二级分类 -->
          <transition name="fade">
            <div v-if="selectedCategory1" class="subcategory-select-wrapper">
              <select 
                v-model="form.categoryId" 
                class="form-select"
                :disabled="loadingCategory2"
              >
                <option value="" disabled>{{ loadingCategory2 ? '加载中...' : '请选择二级分类' }}</option>
                <option v-for="cat in categoryList2" :key="cat.id" :value="cat.id">
                  {{ cat.name }}
                </option>
              </select>
            </div>
          </transition>
          <div v-if="!form.categoryId && selectedCategory1 && !loadingCategory2" class="error-text">
            请完成分类选择
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">
            商品规格 (SKU)
          </label>
          <div class="sku-manager">
            <div v-for="(sku, index) in form.skus" :key="index" class="sku-item">
              <div class="sku-row">
                <input v-model="sku.name" placeholder="规格名称 (如: 红色 L码)" class="form-input sku-name" />
                <input v-model.number="sku.price" type="number" placeholder="价格" class="form-input sku-price" />
                <input v-model.number="sku.stock" type="number" placeholder="库存" class="form-input sku-stock" />
                <button type="button" class="remove-sku-btn" @click="removeSku(index)" v-if="form.skus.length > 1">删除</button>
              </div>
            </div>
            <button type="button" class="add-sku-btn" @click="addSku">+ 添加规格</button>
          </div>
        </div>

        <!-- 详细描述 -->
        <div class="form-group">
          <label class="form-label">
            商品详情
          </label>
          <textarea
            v-model="form.detail"
            class="form-textarea"
            placeholder="请输入商品详细描述..."
            rows="6"
          ></textarea>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { postProduct, updateProduct, getProductDetail, getCategoryList } from '@/api/product'
import { getMyShop } from '@/api/shop'
import { uploadFile } from '@/api/common'
import { getImageUrl } from '@/utils/image'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()

// 状态
const isEditMode = ref(false)
const publishing = ref(false)
const fileInput = ref(null)
const shopId = ref(null)
const categoryList1 = ref([]) // 一级分类
const categoryList2 = ref([]) // 二级分类
const selectedCategory1 = ref(null) // 选中一级分类
const loadingCategory2 = ref(false)

const form = reactive({
  id: null,
  name: '',
  title: '',
  mainImage: '',
  price: '',
  stock: '',
  detail: '',
  shopId: null,
  categoryId: '',
  skus: [
    { name: '', price: '', stock: '', image: '', specs: '{}' }
  ]
})

// 是否可以发布
const canPublish = computed(() => {
  return (
    form.name.trim() &&
    form.mainImage &&
    form.categoryId &&
    form.skus.length > 0 &&
    form.skus.every(sku => sku.price > 0 && sku.stock >= 0)
  )
})

const addSku = () => {
  form.skus.push({
    name: '',
    price: '',
    stock: '',
    image: form.mainImage || '',
    specs: '{}'
  })
}

const removeSku = (index) => {
  if (form.skus.length <= 1) return
  form.skus.splice(index, 1)
}

// 分类联动逻辑
const handleCategory1Change = async () => {
  form.categoryId = '' // 清空二级分类选择
  categoryList2.value = []
  
  if (!selectedCategory1.value) return
  
  loadingCategory2.value = true
  try {
    const res = await getCategoryList(selectedCategory1.value, 2)
    categoryList2.value = res || []
  } catch (err) {
    ElMessage.error('加载二级分类失败')
  } finally {
    loadingCategory2.value = false
  }
}

// 初始化
const init = async () => {
  // 0. 加载一级分类
  try {
    const res = await getCategoryList(null, 1)
    categoryList1.value = res || []
  } catch (err) {
    console.error('获取分类失败', err)
  }

  // 1. 获取商家店铺ID
  try {
    const shop = await getMyShop()
    if (shop) {
      shopId.value = shop.id
    } else {
      ElMessage.warning('请先创建店铺')
      router.push('/merchant/profile')
      return
    }
  } catch (err) {
    ElMessage.error('获取店铺信息失败')
    return
  }

  // 2. 检查是否编辑模式 (回显逻辑待完善)
  const productId = route.query.id
  if (productId) {
    isEditMode.value = true
    loadProduct(productId)
  }
}

const loadProduct = async (id) => {
  try {
    const res = await getProductDetail(id)
    if (res) {
      // 确保 shopId 被正确设置
      if (res.shopId) {
        shopId.value = res.shopId
      }

      // 过滤掉不需要的字段，特别是 shop 对象，因为它会导致后端反序列化失败
      // 仅复制 form 中定义的字段
      Object.keys(form).forEach(key => {
        if (res[key] !== undefined) {
          form[key] = res[key]
        }
      })
      
      // 特殊处理 skus，确保格式正确
      if (res.skus) {
        form.skus = res.skus.map(s => ({
          ...s,
          price: Number(s.price),
          stock: Number(s.stock)
        }))
      }
      
      // 特殊处理详情 (从 customAttributes 取回)
      if (res.customAttributes && res.customAttributes.length > 0) {
        const detailAttr = res.customAttributes.find(a => a.label === '商品详情')
        if (detailAttr && detailAttr.value && detailAttr.value.length > 0) {
          form.detail = detailAttr.value[0]
        }
      }
      
      // 回显分类逻辑
      if (res.categoryId) {
        // 1. 获取该二级分类的详情，找到其父分类ID (这里假设后端返回了 categoryId 但没有返回 parentId，需要查)
        // 简单做法：遍历所有一级分类，查找哪个一级分类下包含这个二级分类
        // 或者优化：后端返回商品详情时带上 categoryParentId
        
        // 尝试自动匹配：先加载所有一级分类，然后尝试找到该二级分类的父级
        // 由于没有直接接口查父级，这里尝试一种变通方法：
        // 假设 categoryId 是二级分类ID，我们需要知道它属于哪个一级分类
        
        // 临时方案：遍历所有一级分类，分别请求其子分类，看是否包含当前 categoryId
        // 注意：这会产生多次请求，性能较差。建议后端 getProductDetail 返回 parentCategoryId
        
        // 既然无法直接获取，我们先尝试直接加载二级分类列表（如果有 parentId 最好）
        // 假设 res.categoryId 对应的分类对象里有 parentId，但 getProductDetail 返回的是 ProductVo，不一定包含 Category 详情
        
        // 优化方案：
        // 遍历 categoryList1，逐个加载二级分类检查（慢）
        // 或者：请求一个新接口 getCategoryById(res.categoryId) 获取分类详情拿到 parentId
        
        // 这里采用更稳妥的方式：
        // 如果后端 ProductVo 中没有 parentCategoryId，我们需要额外获取分类信息
        // 假设 getProductDetail 返回的结构中，category_id 是二级分类 ID
        
        // 模拟：我们假设后端返回了 parentCategoryId 或者我们能通过 categoryId 获取到
        // 暂时先尝试暴力匹配（仅用于演示，实际应优化后端）
        let found = false
        for (const cat1 of categoryList1.value) {
           try {
             const subCats = await getCategoryList(cat1.id, 2)
             if (subCats && subCats.find(c => c.id == res.categoryId)) {
               selectedCategory1.value = cat1.id
               categoryList2.value = subCats
               form.categoryId = res.categoryId
               found = true
               break
             }
           } catch (e) { /* ignore */ }
        }
        
        if (!found) {
           console.warn('未找到对应的父分类')
        }
      }
    }
  } catch (err) {
    ElMessage.error('加载商品失败')
  }
}

// 图片上传
const triggerFileInput = () => {
  fileInput.value.click()
}

const handleFileChange = async (e) => {
  const file = e.target.files[0]
  if (!file) return

  // 验证文件类型和大小
  if (!['image/jpeg', 'image/png', 'image/jpg'].includes(file.type)) {
    ElMessage.error('仅支持 JPG/PNG 格式图片')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB')
    return
  }

  try {
    // 调用真实上传接口
    const url = await uploadFile(file)
    form.mainImage = url
  } catch (err) {
    ElMessage.error('图片上传失败')
  }
}

// 提交发布
const handlePublish = async () => {
  if (!canPublish.value) return
  
  // 确保 shopId 存在
  if (!shopId.value) {
    try {
      const shop = await getMyShop()
      if (shop) shopId.value = shop.id
    } catch (e) {
      console.error(e)
    }
    if (!shopId.value) {
      ElMessage.error('无法获取店铺信息，请刷新重试')
      return
    }
  }

  publishing.value = true
  try {
    // 计算最低价
    const minPrice = Math.min(...form.skus.map(s => Number(s.price)))

    // 构造符合后端 ProductDto 结构的 payload
    // 适配：严格筛选字段，防止多余字段（如 shop, relatedNotes 等）导致 400 错误
    const payload = {
      id: form.id,
      shopId: Number(shopId.value),
      categoryId: Number(form.categoryId),
      name: form.name,
      title: form.title,
      mainImage: form.mainImage,
      price: minPrice,
      status: 1, // 默认为上架状态
      
      // 严格构建 SKU 列表，去除多余字段
      skus: form.skus.map(s => ({
        id: s.id, // 如果是编辑，保留ID
        name: s.name,
        price: Number(s.price),
        stock: Number(s.stock),
        image: s.image || form.mainImage,
        specs: s.specs || '{}'
      })),
      
      // 适配详情字段
      productAttribute: {
        customAttributes: [
          { label: '商品详情', value: [form.detail || ''] }
        ]
      }
    }
    
    // 如果是编辑模式，不需要 shopId (后端通常不更新 shopId)
    // 并且确保没有多余字段
    
    if (isEditMode.value) {
      await updateProduct(payload)
      ElMessage.success('商品更新成功')
    } else {
      await postProduct(payload)
      ElMessage.success('商品发布成功')
    }
    
    router.push('/merchant/products')
  } catch (err) {
    console.error(err)
    ElMessage.error(err.message || '操作失败')
  } finally {
    publishing.value = false
  }
}

const handleBack = () => {
  router.back()
}

onMounted(() => {
  init()
})
</script>

<style scoped>
.product-publish-container {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 20px;
  display: flex;
  justify-content: center;
}

.publish-content {
  width: 100%;
  max-width: 800px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  padding: 30px;
}

.publish-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.back-btn {
  border: none;
  background: none;
  font-size: 16px;
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-title {
  font-size: 20px;
  font-weight: bold;
  color: #333;
}

.publish-submit-btn {
  background: #ff2442;
  color: white;
  border: none;
  padding: 10px 24px;
  border-radius: 24px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
}

.publish-submit-btn:disabled {
  background: #ffb3c0;
  cursor: not-allowed;
}

.publish-form {
  padding: 0 20px;
}

.form-group {
  margin-bottom: 24px;
}

.form-row {
  display: flex;
  gap: 20px;
}

.half {
  flex: 1;
}

.form-label {
  display: block;
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 10px;
}

.required {
  color: #ff2442;
  margin-right: 4px;
}

.form-input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 16px;
  outline: none;
  transition: border-color 0.3s;
}

.form-input:focus {
  border-color: #ff2442;
}

.form-select {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 16px;
  outline: none;
  transition: border-color 0.3s;
  background-color: white;
}

.form-select:focus {
  border-color: #ff2442;
}

.form-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 16px;
  outline: none;
  resize: vertical;
}

.sku-manager {
  border: 1px solid #ddd;
  padding: 15px;
  border-radius: 8px;
  background-color: #f9f9f9;
}

.sku-item {
  margin-bottom: 10px;
}

.sku-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.sku-name {
  flex: 2;
}

.sku-price, .sku-stock {
  flex: 1;
}

.remove-sku-btn {
  color: #ff2442;
  border: 1px solid #ff2442;
  background: white;
  padding: 8px 12px;
  border-radius: 4px;
  cursor: pointer;
}

.remove-sku-btn:active {
  background: rgba(255, 36, 66, 0.08);
}

.add-sku-btn {
  width: 100%;
  padding: 10px;
  border: 1px dashed #999;
  background: white;
  color: #666;
  border-radius: 4px;
  cursor: pointer;
  margin-top: 10px;
}

.add-sku-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.add-sku-btn:active {
  background: rgba(255, 36, 66, 0.06);
}

/* 图片上传样式 */
.image-upload-area {
  width: 150px;
  height: 150px;
  border: 2px dashed #ddd;
  border-radius: 8px;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  overflow: hidden;
  position: relative;
  transition: border-color 0.3s;
}

.image-upload-area:hover {
  border-color: #ff2442;
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
}

.upload-hint {
  font-size: 12px;
  color: #ccc;
  margin-top: 4px;
}

.image-preview-container {
  width: 100%;
  height: 100%;
  position: relative;
}

.image-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-actions {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  opacity: 0;
  transition: opacity 0.3s;
  gap: 10px;
}

.image-preview-container:hover .image-actions {
  opacity: 1;
}

.change-image-btn, .remove-image-btn {
  background: white;
  border: none;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.remove-image-btn {
  color: #ff2442;
}
  .error-text {
    color: #ff2442;
    font-size: 12px;
    margin-top: 4px;
  }
  
  .subcategory-select-wrapper {
    margin-top: 16px;
  }
  
  .mb-16 {
    margin-bottom: 16px;
  }
  
  /* 动画 */
  .fade-enter-active,
  .fade-leave-active {
    transition: opacity 0.3s ease;
  }
  
  .fade-enter-from,
  .fade-leave-to {
    opacity: 0;
  }
</style>
