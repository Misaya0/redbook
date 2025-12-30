<template>
  <div class="merchant-order-container">
    <!-- 1. 顶部统计看板 -->
    <div class="dashboard-cards">
      <el-card shadow="hover" class="stat-card warning">
        <div class="stat-content">
          <div class="stat-label">待发货</div>
          <div class="stat-value">{{ statistics.paid || 0 }}</div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card primary">
        <div class="stat-content">
          <div class="stat-label">今日成交额 (GMV)</div>
          <div class="stat-value">¥{{ statistics.totalAmount || 0 }}</div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card info">
        <div class="stat-content">
          <div class="stat-label">售后/退款处理</div>
          <div class="stat-value">{{ statistics.cancelled || 0 }}</div>
        </div>
      </el-card>
    </div>

    <!-- 2. 高级筛选区 -->
    <div class="filter-section">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="关键词">
          <el-input 
            v-model="searchForm.keyword" 
            placeholder="订单号 / 手机号 / 收货人 / 商品" 
            style="width: 300px;"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 150px;">
            <el-option label="待付款" :value="0" />
            <el-option label="待发货" :value="1" />
            <el-option label="已发货" :value="3" />
            <el-option label="已完成" :value="4" />
            <el-option label="已取消" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="下单时间">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 380px;"
            @change="handleDateChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 3. 高密度表格区 -->
    <div class="table-section" v-loading="loading">
      <!-- 批量操作栏 -->
      <div class="bulk-actions" v-if="selectedRows.length > 0">
        <span class="selected-count">已选 {{ selectedRows.length }} 项</span>
        <el-button type="primary" size="small" @click="handleBulkShip">批量发货</el-button>
        <el-button size="small">批量打印快递单</el-button>
        <el-button size="small">批量备注</el-button>
      </div>

      <el-table 
        :data="tableData" 
        style="width: 100%" 
        border 
        size="small"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center" />
        
        <el-table-column label="订单信息" width="220">
          <template #default="{ row }">
            <div class="cell-group">
              <div class="order-no">ID: {{ row.id }}</div>
              <div class="order-time">{{ row.createTime }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="商品信息" min-width="250">
          <template #default="{ row }">
            <div class="product-cell">
              <el-image :src="getImageUrl(row.productImage)" class="table-img" />
              <div class="product-meta">
                <div class="p-name" :title="row.productName">{{ row.productName }}</div>
                <div class="p-spec">{{ formatSkuSpec(row.skuSpec) || '默认规格' }}</div>
                <div class="p-price">¥{{ row.skuPrice }} x {{ row.quantity }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="实付金额" width="100" align="center">
          <template #default="{ row }">
            <span class="price-text">¥{{ row.finalPrice }}</span>
          </template>
        </el-table-column>

        <el-table-column label="买家信息" width="150">
          <template #default="{ row }">
            <div class="buyer-info">
              <div class="buyer-name">{{ row.buyerName ? `用户名: ${row.buyerName}` : `用户ID: ${row.userId}` }}</div>
              <div v-if="row.buyerPhone" class="buyer-phone">手机号: {{ row.buyerPhone }}</div>
              <el-button link type="primary" size="small">联系买家</el-button>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="商家备注" width="150">
          <template #default="{ row }">
             <div class="seller-memo" @click="editMemo(row)">
               {{ row.merchantMemo || '点击添加备注...' }}
               <el-icon class="edit-icon"><Edit /></el-icon>
             </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="plain">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-group">
              <el-button 
                v-if="row.status === 1" 
                type="primary" 
                size="small" 
                link
                @click="handleStatus(row, 3)"
              >发货</el-button>
              <el-button type="primary" link size="small" @click="handleDetail(row)">详情</el-button>
            </div>
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
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { searchOrders, updateOrderStatus, getStatistics, type OrderVo, type OrderSearchDto, type OrderStatisticsVo } from '@/api/order';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getImageUrl } from '@/utils/image';
import { Edit } from '@element-plus/icons-vue';

// 状态定义
const loading = ref(false);
const tableData = ref<OrderVo[]>([]);
const currentPage = ref(1);
const pageSize = ref(20); // 商家端默认一页显示更多
const total = ref(0);
const selectedRows = ref<OrderVo[]>([]);

const statistics = ref<OrderStatisticsVo>({} as OrderStatisticsVo);

// 搜索表单
const searchForm = reactive<Partial<OrderSearchDto>>({
  keyword: undefined,
  status: undefined,
  startTime: undefined,
  endTime: undefined
});
const dateRange = ref<[string, string] | null>(null);

const formatSkuSpec = (spec?: string) => {
  if (!spec) return '';
  return spec
    .split(' / ')
    .map(part => part.replace(/^[^:]+:\s*/, '').trim())
    .filter(Boolean)
    .join(' / ');
};

// 方法
const loadData = async () => {
  loading.value = true;
  try {
    const params: OrderSearchDto = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      ...searchForm
    };
    
    // 处理状态 -1 或 undefined
    if (params.status === undefined || params.status === null) {
        delete params.status;
    }

    const res: any = await searchOrders(params);
    if (res.list) {
      tableData.value = res.list;
      total.value = Number(res.total) || 0;
    } else if (Array.isArray(res)) {
      tableData.value = res;
      total.value = res.length;
    }
  } catch (error) {
    console.error(error);
    ElMessage.error('加载订单失败');
  } finally {
    loading.value = false;
  }
};

const loadStatistics = async () => {
  try {
    const res = await getStatistics();
    if (res) {
      statistics.value = res as any;
    }
  } catch (e) {
    console.error(e);
  }
};

onMounted(() => {
  loadData();
  loadStatistics();
});

// 事件处理
const handleSearch = () => {
  currentPage.value = 1;
  loadData();
};

const resetSearch = () => {
  searchForm.keyword = undefined;
  searchForm.status = undefined;
  searchForm.startTime = undefined;
  searchForm.endTime = undefined;
  dateRange.value = null;
  handleSearch();
};

const handleDateChange = (val: [string, string] | null) => {
  if (val) {
    searchForm.startTime = val[0];
    searchForm.endTime = val[1];
  } else {
    searchForm.startTime = undefined;
    searchForm.endTime = undefined;
  }
};

const handleSelectionChange = (val: OrderVo[]) => {
  selectedRows.value = val;
};

const handleSizeChange = (val: number) => {
  pageSize.value = val;
  loadData();
};

const handleCurrentChange = (val: number) => {
  currentPage.value = val;
  loadData();
};

const handleStatus = async (row: OrderVo, status: number) => {
  try {
    await ElMessageBox.confirm('确定要发货吗？', '提示', { type: 'warning' });
    await updateOrderStatus(row.id!, status);
    ElMessage.success('操作成功');
    loadData();
    loadStatistics();
  } catch (e) {
    // cancelled
  }
};

const handleBulkShip = () => {
    if (!selectedRows.value.length) return;
    ElMessageBox.confirm(`确定要批量发货这 ${selectedRows.value.length} 个订单吗？`, '批量操作', { type: 'warning' })
    .then(async () => {
        // 这里应该调用批量接口，目前循环调用
        let successCount = 0;
        for (const row of selectedRows.value) {
            if (row.status === 1) { // 仅待发货的可发
                try {
                    await updateOrderStatus(row.id!, 3);
                    successCount++;
                } catch (e) {}
            }
        }
        ElMessage.success(`批量发货成功 ${successCount} 单`);
        loadData();
        loadStatistics();
    });
};

const handleDetail = (row: OrderVo) => {
    // 简单展示详情或跳转
    ElMessage.info(`查看订单 ${row.id} 详情`);
};

const editMemo = (row: OrderVo) => {
    ElMessageBox.prompt('请输入商家备注', '备注', {
        inputValue: row.merchantMemo || '',
    }).then(({ value }) => {
        // 这里应该调用后端保存备注，目前前端模拟
        row.merchantMemo = value;
        ElMessage.success('备注已保存');
    }).catch(() => {});
};

// 辅助函数
const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '待付款',
    1: '待发货',
    2: '已取消',
    3: '已发货',
    4: '已完成'
  };
  return map[status] || '未知';
};

const getStatusType = (status: number) => {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'danger', // 待发货红色预警
    2: 'info',
    3: 'primary',
    4: 'success'
  };
  return map[status] || '';
};
</script>

<style scoped>
.merchant-order-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

/* 统计看板 */
.dashboard-cards {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}
.stat-card {
  flex: 1;
  border: none;
}
.stat-card.warning { background: linear-gradient(135deg, #fff 0%, #fff1f0 100%); border-left: 4px solid #ff4d4f; }
.stat-card.primary { background: linear-gradient(135deg, #fff 0%, #e6f7ff 100%); border-left: 4px solid #1890ff; }
.stat-card.info { background: linear-gradient(135deg, #fff 0%, #f0f5ff 100%); border-left: 4px solid #722ed1; }

.stat-content {
  display: flex;
  flex-direction: column;
}
.stat-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

/* 筛选区 */
.filter-section {
  background: #fff;
  padding: 20px 20px 0 20px;
  border-radius: 4px;
  margin-bottom: 20px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

/* 表格区 */
.table-section {
  background: #fff;
  padding: 20px;
  border-radius: 4px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

.bulk-actions {
  margin-bottom: 15px;
  padding: 10px;
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.selected-count {
  font-size: 13px;
  color: #1890ff;
  margin-right: 10px;
}

.cell-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.order-no { font-weight: bold; color: #333; }
.order-time { font-size: 12px; color: #999; }

.product-cell {
  display: flex;
  align-items: center;
}
.table-img {
  width: 50px;
  height: 50px;
  border-radius: 4px;
  margin-right: 10px;
  flex-shrink: 0;
}
.product-meta {
  flex: 1;
  overflow: hidden;
}
.p-name {
  font-size: 13px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.p-spec {
  font-size: 12px;
  color: #999;
}
.p-price {
  font-size: 12px;
  color: #666;
}

.price-text {
  font-weight: bold;
  color: #ff4d4f;
}

.buyer-info {
  font-size: 13px;
}
.buyer-name {
  color: #333;
}
.buyer-phone {
  font-size: 12px;
  color: #999;
}

.seller-memo {
  color: #e6a23c;
  cursor: pointer;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.edit-icon { display: none; }
.seller-memo:hover .edit-icon { display: inline-block; }

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
