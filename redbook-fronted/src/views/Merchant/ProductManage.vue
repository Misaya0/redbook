<template>
  <div class="product-manage-container" ref="containerRef">
    <div class="header">
      <h2>商家中心 - 商品管理</h2>
      <el-button type="primary" @click="handleAdd">发布新商品</el-button>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-form :inline="true" :model="searchForm" @submit.prevent="handleSearch">
        <el-form-item label="商品名称">
          <el-input v-model="searchForm.keyword" placeholder="请输入关键词" clearable/>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" @click="handleSearch" :loading="loading">查询</el-button>
          <el-button @click="resetSearch" :disabled="loading">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="category-filter" v-loading="categoryLoading" element-loading-text="加载分类中...">
        <div class="category-header">
          <div class="category-label">分类</div>

          <div class="pill-list pill-list--level1">
            <button
                v-for="item in categoryLevel1"
                :key="item.id"
                type="button"
                class="pill"
                :class="{ 'pill--active': activeCategory1 === String(item.id) }"
                @click="handleCategory1TabChange(String(item.id))"
            >
              {{ item.name }}
            </button>
          </div>
        </div>

        <div v-if="activeCategory1 !== '-1'" class="subcategory-row">
          <div class="category-label-spacer"></div>
          <div class="pill-list pill-list--level2">
            <button
                type="button"
                class="pill pill--sub"
                :class="{ 'pill--active': activeCategory2 == null }"
                @click="handleCategory2Click({ id: -1 })"
            >
              全部
            </button>
            <button
                v-for="sub in categoryLevel2"
                :key="sub.id"
                type="button"
                class="pill pill--sub"
                :class="{ 'pill--active': activeCategory2 === sub.id }"
                @click="handleCategory2Click(sub)"
            >
              {{ sub.name }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 商品列表表格 -->
    <el-table
        :data="tableData"
        border
        style="width: 100%"
        v-loading="loading"
        element-loading-text="加载中..."
    >
      <template #empty>
        <el-empty description="暂无商品"/>
      </template>
      <el-table-column prop="id" label="ID" width="80"/>
      <el-table-column label="商品图片" width="100">
        <template #default="scope">
          <el-image
              :src="getImageUrl(scope.row.mainImage)"
              style="width: 60px; height: 60px; border-radius: 4px;"
              fit="cover"
          />
        </template>
      </el-table-column>
      <el-table-column prop="name" label="商品名称" min-width="200" show-overflow-tooltip/>
      <el-table-column label="价格" width="120">
        <template #default="scope">
          <span style="color: #ff2442; font-weight: bold;">¥{{ scope.row.price }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="totalStock" label="总库存" width="100"/>
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '上架中' : '已下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="scope">
          <el-button link type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button link type="primary" size="small" @click="handleSku(scope.row)">SKU管理</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-container">
      <div class="page-count" v-if="total > 0">共 {{ pageCount }} 页</div>
      <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total,sizes,prev,pager,next,jumper"
          :total="total"
          prev-text="上一页"
          next-text="下一页"
          :disabled="loading"
          background
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
      />
    </div>

    <!-- SKU管理弹窗 -->
    <el-dialog
        v-model="skuDialogVisible"
        title="SKU库存管理"
        width="700px"
    >
      <div v-loading="skuLoading">
        <div class="sku-header" style="margin-bottom: 15px; display: flex; justify-content: flex-end;">
          <el-button type="primary" size="small" @click="addSkuItem">添加规格</el-button>
        </div>

        <el-table :data="skuList" border style="width: 100%">
          <el-table-column label="规格名称" min-width="150">
            <template #default="{ row }">
              <el-input v-model="row.name" placeholder="如: 红色 L码"/>
            </template>
          </el-table-column>
          <el-table-column label="价格" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.price" :min="0" :precision="2" :controls="false" style="width: 100%"/>
            </template>
          </el-table-column>
          <el-table-column label="库存" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.stock" :min="0" :controls="false" style="width: 100%"/>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ $index }">
              <el-button
                  type="danger"
                  link
                  @click="removeSkuItem($index)"
                  :disabled="skuList.length <= 1"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="skuDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveSkus" :loading="skuLoading">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>


<script setup>
import {ref, reactive, onMounted, watch, computed, nextTick} from 'vue'
import {useRouter} from 'vue-router'
import {searchProductMySql, deleteProduct, getProductDetail, updateProduct, getCategoryList} from '@/api/product'
import {getMyShop} from '@/api/shop'
import {getImageUrl} from '@/utils/image'
import {ElMessage, ElMessageBox} from 'element-plus'

const router = useRouter()

onMounted(() => {
  init()
  fetchCategoryLevel1()
})
// 状态
const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const pageCount = computed(() => {
  const size = Number(pageSize.value) || 10
  const t = Number(total.value) || 0
  return Math.max(1, Math.ceil(t / size))
})
const containerRef = ref(null)
const shopId = ref(null) // 当前商家店铺ID
const searchForm = reactive({
  keyword: '',
  categoryId: null,
  categoryIds: []
})

// 分类筛选状态
const categoryLoading = ref(false)
const categoryLevel1 = ref([])
const categoryLevel2 = ref([])
const activeCategory1 = ref('-1') // -1: 全部
const activeCategory2 = ref(null) // null: 全部(二级)

// SKU 管理相关
const skuDialogVisible = ref(false)
const skuLoading = ref(false)
const currentProduct = ref(null)

const skuList = ref([])
// 方法
const init = async () => {
  try {
    // 1. 获取当前商家的店铺ID
    const shop = await getMyShop()
    if (shop) {
      shopId.value = shop.id
      // 监听器会自动触发 fetchList，这里不再重复调用
    } else {
      ElMessage.warning('请先创建店铺')
    }
  } catch (err) {
    ElMessage.error('获取店铺信息失败')
  }
}

// 加载一级分类，并补充“全部”
const fetchCategoryLevel1 = async () => {
  if (categoryLoading.value) return
  categoryLoading.value = true
  try {
    const res = await getCategoryList(null, 1)
    const list = Array.isArray(res) ? res : []
    categoryLevel1.value = [{id: -1, name: '全部'}, ...list]
  } catch (e) {
    categoryLevel1.value = [{id: -1, name: '全部'}]
  } finally {
    categoryLoading.value = false
  }
}

// 切换一级分类：加载二级分类（不直接筛选，二级点击时筛选）
const handleCategory1TabChange = async (name) => {
  activeCategory1.value = String(name)
  activeCategory2.value = null
  searchForm.categoryId = null
  searchForm.categoryIds = []

  if (activeCategory1.value === '-1') {
    categoryLevel2.value = []
    handleSearch()
    return
  }

  categoryLevel2.value = []
  categoryLoading.value = true
  try {
    const res = await getCategoryList(Number(activeCategory1.value), 2)
    categoryLevel2.value = Array.isArray(res) ? res : []
    const ids = categoryLevel2.value.map(c => c.id).filter(v => v != null)
    searchForm.categoryIds = ids.length > 0 ? ids : [-1]
    handleSearch()
  } catch (e) {
    categoryLevel2.value = []
    searchForm.categoryIds = [-1]
    handleSearch()
  } finally {
    categoryLoading.value = false
  }
}

// 点击二级分类：设置 categoryId 并触发搜索；支持点击“全部”清空筛选
const handleCategory2Click = (item) => {
  const id = item?.id
  if (id === -1) {
    activeCategory2.value = null
    searchForm.categoryId = null
    const ids = categoryLevel2.value.map(c => c.id).filter(v => v != null)
    searchForm.categoryIds = ids.length > 0 ? ids : [-1]
    handleSearch()
    return
  }

  if (activeCategory2.value === id) return
  activeCategory2.value = id
  searchForm.categoryId = id
  searchForm.categoryIds = []
  handleSearch()
}

// 监听店铺ID变化，一旦获取到ID自动加载商品
watch(shopId, (val) => {
  if (val) {
    currentPage.value = 1
    fetchList()
  }
})

const scrollToTop = async () => {
  await nextTick()
  const el = containerRef.value
  if (el && typeof el.scrollIntoView === 'function') {
    el.scrollIntoView({behavior: 'smooth', block: 'start'})
    return
  }
  if (typeof window !== 'undefined' && typeof window.scrollTo === 'function') {
    window.scrollTo({top: 0, behavior: 'smooth'})
  }
}

const fetchList = async () => {
  if (!shopId.value) return
  if (loading.value) return // 防止重复请求

  loading.value = true
  try {
    const res = await searchProductMySql({
      keyword: searchForm.keyword,
      categoryId: searchForm.categoryId,
      categoryIds: searchForm.categoryIds,
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      shopId: shopId.value
    })

    let list = []
    if (res && res.list) {
      list = res.list
      total.value = Number(res.total || 0)
    } else if (res && res.records) {
      list = res.records
      total.value = Number(res.total || 0)
    } else if (Array.isArray(res)) {
      list = res
      total.value = res.length
    }

    tableData.value = list

  } catch (err) {
    console.error(err)
    ElMessage.error('获取列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  scrollToTop()
  fetchList()
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.categoryId = null
  searchForm.categoryIds = []
  activeCategory1.value = '-1'
  activeCategory2.value = null
  categoryLevel2.value = []
  handleSearch()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  scrollToTop()
  fetchList()
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  scrollToTop()
  fetchList()
}

const handleAdd = () => {
  router.push('/merchant/publish')
}

const handleEdit = (row) => {
  router.push({path: '/merchant/publish', query: {id: row.id}})
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除该商品吗?', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteProduct(row.id)
      ElMessage.success('删除成功')
      fetchList()
    } catch (err) {
      ElMessage.error('删除失败')
    }
  })
}

const handleSku = async (row) => {
  currentProduct.value = row
  skuLoading.value = true
  skuDialogVisible.value = true

  try {
    const res = await getProductDetail(row.id)
    if (res && res.skus) {
      skuList.value = res.skus.map(s => ({
        ...s,
        price: Number(s.price),
        stock: Number(s.stock)
      }))
    } else {
      // 如果没有SKU，创建一个默认的
      skuList.value = [{
        name: '默认规格',
        price: Number(row.price),
        stock: 0,
        image: row.mainImage,
        specs: '{}'
      }]
    }
  } catch (err) {
    ElMessage.error('加载SKU失败')
  } finally {
    skuLoading.value = false
  }
}

const saveSkus = async () => {
  if (!currentProduct.value) return

  // 简单校验
  if (skuList.value.some(s => !s.name || s.price < 0 || s.stock < 0)) {
    ElMessage.warning('请完善SKU信息，价格和库存不能为负数')
    return
  }

  skuLoading.value = true
  try {
    // 构造更新 payload
    const payload = {
      ...currentProduct.value,
      skus: skuList.value
    }

    // 解决后端反序列化报错：Cannot construct instance of `org.bson.types.ObjectId`
    // 前端 currentProduct 可能包含 productAttribute，但 id 格式不符合 ObjectId 要求
    // 且 updateProduct 接口目前只更新 SPU 和 SKU，不更新 MongoDB 中的属性，故直接移除
    if ('productAttribute' in payload) delete payload.productAttribute
    // 移除 Product 实体中不存在的字段，防止 Jackson 反序列化报错
    if ('customAttributes' in payload) delete payload.customAttributes
    if ('shop' in payload) delete payload.shop
    if ('relatedNotes' in payload) delete payload.relatedNotes
    if ('totalStock' in payload) delete payload.totalStock

    await updateProduct(payload)
    ElMessage.success('SKU更新成功')
    skuDialogVisible.value = false
    fetchList() // 刷新列表
  } catch (err) {
    ElMessage.error('保存失败')
  } finally {
    skuLoading.value = false
  }
}

const addSkuItem = () => {
  skuList.value.push({
    name: '',
    price: 0,
    stock: 0,
    image: currentProduct.value.mainImage,
    specs: '{}'
  })
}

const removeSkuItem = (index) => {
  skuList.value.splice(index, 1)
}

</script>

<style scoped>
.product-manage-container {
  padding: 20px;
  background: white;
  min-height: 100vh;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
}

.search-bar {
  margin-bottom: 20px;
  background: #f9f9f9;
  padding: 15px;
  border-radius: 4px;
}

.category-filter {
  margin-top: 8px;
}

.category-label {
  color: #606266;
  font-size: 14px;
  white-space: nowrap;
  width: 44px;
}

.category-header {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.pill-list {
  display: flex;
  gap: 8px;
  min-width: 0;
}

.pill-list--level1 {
  flex: 1;
  overflow-x: auto;
  overflow-y: hidden;
  white-space: nowrap;
  padding-bottom: 2px;
}

.pill {
  flex: 0 0 auto;
  border: 0;
  border-radius: 999px;
  padding: 7px 12px;
  font-size: 13px;
  line-height: 1;
  background: #eef0f3;
  color: #606266;
  cursor: pointer;
  user-select: none;
  transition: transform 0.15s ease, box-shadow 0.15s ease, background 0.15s ease, color 0.15s ease;
}

.pill:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
  background: #e7e9ee;
}

.pill:active {
  transform: translateY(0);
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.08);
}

.pill--active {
  background: #ff2442;
  color: #fff;
  box-shadow: 0 8px 18px rgba(255, 36, 66, 0.25);
}

.pill--active:hover {
  background: #e61f3b;
  box-shadow: 0 10px 22px rgba(255, 36, 66, 0.28);
}

.pill--sub.pill--active {
  background: #409eff;
  color: #fff;
  box-shadow: 0 8px 18px rgba(64, 158, 255, 0.25);
}

.pill--sub.pill--active:hover {
  background: #337ecc;
  box-shadow: 0 10px 22px rgba(64, 158, 255, 0.28);
}

.pill--sub {
  padding: 7px 12px;
}

.subcategory-row {
  margin-top: 10px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.category-label-spacer {
  width: 44px;
  flex: 0 0 44px;
}

.pill-list--level2 {
  flex: 1;
  flex-wrap: wrap;
  white-space: normal;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.page-count {
  color: #666;
  font-size: 13px;
  white-space: nowrap;
}

@media (max-width: 768px) {
  .header {
    gap: 10px;
    flex-direction: column;
    align-items: flex-start;
  }

  .pagination-container {
    justify-content: center;
  }

  .page-count {
    text-align: left;
  }
}
</style>
