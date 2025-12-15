<template>
  <div class="product-list-container">
    <div class="search-bar">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="商品名称" clearable />
        </el-form-item>
        <el-form-item label="价格区间">
          <el-input-number v-model="searchForm.minPrice" :min="0" placeholder="最低价" />
          -
          <el-input-number v-model="searchForm.maxPrice" :min="0" placeholder="最高价" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="success" @click="handleAdd">发布商品</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table :data="tableData" border style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="image" label="图片" width="120">
        <template #default="scope">
          <el-image :src="scope.row.image" style="width: 100px; height: 100px" fit="cover" />
        </template>
      </el-table-column>
      <el-table-column prop="name" label="商品名称" min-width="200" />
      <el-table-column prop="price" label="价格" width="120">
        <template #default="scope">
          ¥{{ scope.row.price }}
        </template>
      </el-table-column>
      <el-table-column prop="stock" label="库存" width="100" />
      <el-table-column prop="sales" label="销量" width="100" />
      <el-table-column prop="time" label="发布时间" width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="scope">
          <el-button link type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button link type="primary" size="small" @click="handleDetail(scope.row)">详情</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(scope.row)">下架</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-container">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 商品表单弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '发布商品' : '编辑商品'"
      width="600px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="商品图片" prop="image">
          <el-input v-model="form.image" placeholder="图片URL" />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" :precision="0" />
        </el-form-item>
        <el-form-item label="店铺ID" prop="shopId">
          <el-input-number v-model="form.shopId" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { getProductList, searchProduct, postProduct, updateProduct, deleteProduct, type Product } from '@/api/product';
import { ElMessage, ElMessageBox } from 'element-plus';

const loading = ref(false);
const tableData = ref<Product[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0); // 这里的 total 可能不准确，因为后端没有返回 total

const searchForm = reactive({
  keyword: '',
  minPrice: undefined as number | undefined,
  maxPrice: undefined as number | undefined
});

const dialogVisible = ref(false);
const dialogType = ref<'add' | 'edit'>('add');
const formRef = ref();
const form = reactive<Product>({
  name: '',
  price: 0,
  stock: 0,
  image: '',
  shopId: 1
});

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }],
  image: [{ required: true, message: '请输入图片URL', trigger: 'blur' }]
};

const loadData = async () => {
  console.log('ProductList: loadData start', { pageNum: currentPage.value, pageSize: pageSize.value });
  loading.value = true;
  try {
    let res;
    if (searchForm.keyword || searchForm.minPrice || searchForm.maxPrice) {
      console.log('ProductList: calling searchProduct');
      res = await searchProduct({
        pageNum: currentPage.value,
        pageSize: pageSize.value,
        ...searchForm
      });
    } else {
      console.log('ProductList: calling getProductList');
      res = await getProductList(currentPage.value, pageSize.value);
    }
    console.log('ProductList: res', res);
    
    if (res.list) {
      tableData.value = res.list;
      total.value = res.total;
    } else if (Array.isArray(res)) {
      tableData.value = res;
      // 兼容旧接口
      total.value = res.length;
    }
  } catch (error) {
    console.error('ProductList: loadData error', error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  console.log('ProductList: onMounted');
  loadData();
});

const handleSearch = () => {
  currentPage.value = 1;
  loadData();
};

const resetSearch = () => {
  searchForm.keyword = '';
  searchForm.minPrice = undefined;
  searchForm.maxPrice = undefined;
  handleSearch();
};

const handleSizeChange = (val: number) => {
  pageSize.value = val;
  loadData();
};

const handleCurrentChange = (val: number) => {
  currentPage.value = val;
  loadData();
};

const handleAdd = () => {
  dialogType.value = 'add';
  Object.assign(form, {
    name: '',
    price: 0,
    stock: 0,
    image: '',
    shopId: 1
  });
  dialogVisible.value = true;
};

const handleEdit = (row: Product) => {
  dialogType.value = 'edit';
  Object.assign(form, row);
  dialogVisible.value = true;
};

const submitForm = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        if (dialogType.value === 'add') {
          await postProduct(form);
          ElMessage.success('发布成功');
        } else {
          await updateProduct(form);
          ElMessage.success('更新成功');
        }
        dialogVisible.value = false;
        loadData();
      } catch (error) {
        console.error(error);
        ElMessage.error('操作失败');
      }
    }
  });
};

const handleDelete = (row: Product) => {
  ElMessageBox.confirm('确认下架（删除）该商品吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await deleteProduct(row.id!);
      ElMessage.success('下架成功');
      loadData();
    } catch (e) {
      console.error(e);
    }
  });
};

const handleDetail = (row: Product) => {
  // 详情页逻辑
  ElMessageBox.alert(`
    <p>ID: ${row.id}</p>
    <p>名称: ${row.name}</p>
    <p>价格: ${row.price}</p>
    <p>库存: ${row.stock}</p>
    <img src="${row.image}" style="max-width: 100%"/>
  `, '商品详情', {
    dangerouslyUseHTMLString: true
  });
};
</script>

<style scoped>
.product-list-container {
  padding: 20px;
}
.search-bar {
  margin-bottom: 20px;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
