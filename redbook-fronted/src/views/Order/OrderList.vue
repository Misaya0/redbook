<template>
  <div class="order-list-container">
    <!-- 统计卡片 -->
    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>总订单数</span>
            </div>
          </template>
          <div class="card-value">{{ stats.totalOrders || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>待付款</span>
            </div>
          </template>
          <div class="card-value">{{ stats.pendingPayment || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>待发货</span>
            </div>
          </template>
          <div class="card-value">{{ stats.paid || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>总销售额</span>
            </div>
          </template>
          <div class="card-value">¥{{ stats.totalAmount || 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <div class="search-bar">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="订单ID">
          <el-input v-model="searchForm.orderId" placeholder="订单ID" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="待付款" :value="0" />
            <el-option label="已付款" :value="1" />
            <el-option label="已取消" :value="2" />
            <el-option label="已发货" :value="3" />
            <el-option label="已完成" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table :data="tableData" border style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="订单ID" width="180" />
      <el-table-column label="商品信息" min-width="250">
        <template #default="scope">
          <div style="display: flex; align-items: center;">
            <el-image :src="scope.row.productImage" style="width: 50px; height: 50px; margin-right: 10px;" />
            <span>{{ scope.row.productName }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="finalPrice" label="金额" width="120">
        <template #default="scope">
          ¥{{ scope.row.finalPrice }}
        </template>
      </el-table-column>
      <el-table-column prop="quantity" label="数量" width="80" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="scope">
          <el-button link type="primary" size="small" @click="handleDetail(scope.row)">详情</el-button>
          <el-button 
            v-if="scope.row.status === 1" 
            link 
            type="success" 
            size="small" 
            @click="handleStatus(scope.row, 3)"
          >发货</el-button>
          <el-button 
            v-if="scope.row.status === 3" 
            link 
            type="success" 
            size="small" 
            @click="handleStatus(scope.row, 4)"
          >完成</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-container">
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        prev-text="上一页"
        next-text="下一页"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
      <div v-if="currentOrder">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单ID">{{ currentOrder.id }}</el-descriptions-item>
          <el-descriptions-item label="状态">
             <el-tag :type="getStatusType(currentOrder.status)">{{ getStatusText(currentOrder.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="商品名称" :span="2">{{ currentOrder.productName }}</el-descriptions-item>
          <el-descriptions-item label="商品图片" :span="2">
            <el-image :src="currentOrder.productImage" style="width: 100px; height: 100px" />
          </el-descriptions-item>
          <el-descriptions-item label="单价">¥{{ currentOrder.finalPrice / currentOrder.quantity }}</el-descriptions-item>
          <el-descriptions-item label="数量">{{ currentOrder.quantity }}</el-descriptions-item>
          <el-descriptions-item label="总价">¥{{ currentOrder.finalPrice }}</el-descriptions-item>
          <el-descriptions-item label="下单用户ID">{{ currentOrder.userId }}</el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ currentOrder.createTime }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { searchOrders, updateOrderStatus, getOrderDetail, getStatistics, type OrderVo, type OrderSearchDto, type OrderStatisticsVo } from '@/api/order';
import { ElMessage } from 'element-plus';

const loading = ref(false);
const tableData = ref<OrderVo[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const dateRange = ref<[string, string] | null>(null);
const stats = ref<Partial<OrderStatisticsVo>>({});

const searchForm = reactive<Partial<OrderSearchDto>>({
  orderId: undefined,
  status: undefined,
  userId: undefined
});

const detailVisible = ref(false);
const currentOrder = ref<OrderVo | null>(null);

const loadData = async () => {
  loading.value = true;
  try {
    fetchStats();
    const params: OrderSearchDto = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      ...searchForm,
      startTime: dateRange.value ? dateRange.value[0] + ' 00:00:00' : undefined,
      endTime: dateRange.value ? dateRange.value[1] + ' 23:59:59' : undefined
    };
    
    const res: any = await searchOrders(params);
    if (res.list) {
      tableData.value = res.list;
      total.value = res.total;
    } else if (Array.isArray(res)) {
      tableData.value = res;
      total.value = res.length;
    }
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
};

const fetchStats = async () => {
  try {
    const res: any = await getStatistics();
    if (res) {
      // res could be the object directly if interceptor logic applies, or wrapped
      // Based on my change to request.js, if total is present it returns {list, total}, else data.
      // But getStatistics returns a single object (OrderStatisticsVo), so no 'total' field.
      // So request.js will return data.data (the OrderStatisticsVo).
      stats.value = res;
    }
  } catch (e) {
    console.error('获取统计数据失败', e);
  }
};

onMounted(() => {
  loadData();
});

const handleSearch = () => {
  currentPage.value = 1;
  loadData();
};

const resetSearch = () => {
  searchForm.orderId = undefined;
  searchForm.status = undefined;
  dateRange.value = null;
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

const handleDetail = async (row: OrderVo) => {
  // 如果列表中信息不全，可以调详情接口
  // 这里直接用列表数据也行，或者调一下接口保险
  try {
    const res = await getOrderDetail(row.id!);
    if (res.data) {
      currentOrder.value = res.data;
      detailVisible.value = true;
    }
  } catch (e) {
    console.error(e);
  }
};

const handleStatus = async (row: OrderVo, status: number) => {
  try {
    await updateOrderStatus(row.id!, status);
    ElMessage.success('状态更新成功');
    loadData();
  } catch (e) {
    ElMessage.error('更新失败');
  }
};

const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '待付款',
    1: '已付款',
    2: '已取消',
    3: '已发货',
    4: '已完成'
  };
  return map[status] || '未知';
};

const getStatusType = (status: number) => {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'primary',
    2: 'info',
    3: 'success',
    4: 'success'
  };
  return map[status] || '';
};
</script>

<style scoped>
.order-list-container {
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-value {
  font-size: 24px;
  font-weight: bold;
  color: #ff2442;
  text-align: center;
}
</style>
