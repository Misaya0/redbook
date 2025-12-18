<template>
  <div class="product-manage-container">
    <div class="header">
      <h2>商家中心 - 商品管理</h2>
      <el-button type="primary" @click="handleAdd">发布新商品</el-button>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-form :inline="true" :model="searchForm" @submit.prevent="handleSearch">
        <el-form-item label="商品名称">
          <el-input v-model="searchForm.keyword" placeholder="请输入关键词" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 商品列表表格 -->
    <el-table :data="tableData" border style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="商品图片" width="100">
        <template #default="scope">
          <el-image 
            :src="getImageUrl(scope.row.mainImage)" 
            style="width: 60px; height: 60px; border-radius: 4px;" 
            fit="cover" 
          />
        </template>
      </el-table-column>
      <el-table-column prop="name" label="商品名称" min-width="200" show-overflow-tooltip />
      <el-table-column label="价格" width="120">
        <template #default="scope">
          <span style="color: #ff2442; font-weight: bold;">¥{{ scope.row.price }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="totalStock" label="总库存" width="100" />
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
      <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, prev, pager, next"
          :total="total"
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
              <el-input v-model="row.name" placeholder="如: 红色 L码" />
            </template>
          </el-table-column>
          <el-table-column label="价格" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.price" :min="0" :precision="2" :controls="false" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="库存" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.stock" :min="0" :controls="false" style="width: 100%" />
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
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { searchProductMySql, deleteProduct, getProductDetail, updateProduct } from '@/api/product'
import { getMyShop } from '@/api/shop'
import { getImageUrl } from '@/utils/image'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

onMounted(() => {
  init()
})
// 状态
const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const shopId = ref(null) // 当前商家店铺ID
const searchForm = reactive({
  keyword: ''
})
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

// 监听店铺ID变化，一旦获取到ID自动加载商品
watch(shopId, (val) => {
  if (val) {
    fetchList()
  }
})
watch(currentPage, (val, oldVal) => {
  if (val !== oldVal) fetchList()
})

const fetchList = async () => {
  console.trace('[fetchList called]')
  if (!shopId.value) return
  if (loading.value) return // 防止重复请求
  
  loading.value = true
  try {
    const res = await searchProductMySql({
      keyword: searchForm.keyword,
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      shopId: shopId.value
    })
    
    let list = []
    if (res && res.list) {
      list = res.list
      total.value = res.total
    } else if (res && res.records) {
      list = res.records
      total.value = res.total
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
  fetchList()
}

const resetSearch = () => {
  searchForm.keyword = ''
  handleSearch()
}

const handleCurrentChange = (val) => {
  // currentPage 已通过 v-model 更新
  fetchList()
}

const handleAdd = () => {
  router.push('/merchant/publish')
}

const handleEdit = (row) => {
  router.push({ path: '/merchant/publish', query: { id: row.id } })
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

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
