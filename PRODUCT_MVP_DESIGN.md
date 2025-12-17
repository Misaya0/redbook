# 商品服务 (Product Service) MVP 设计方案 - 内容电商版

## 1. 定位与闭环

**定位**：支持“种草-拔草”闭环。核心在于商品与笔记（内容）的强关联，以及高并发下的库存准确性。
**最小可用闭环 (MVP)**：
1.  **商家**：发布商品（SKU维度），管理库存。
2.  **内容创作者**：发布笔记时关联商品（"挂链"）。
3.  **消费者**：
    *   在笔记详情页看到商品卡片。
    *   在商品详情页看到“相关笔记”（种草秀）。
    *   下单购买（扣减库存）。

## 2. 接口边界 (Interface Boundaries)

### 2.1 Product Service <-> Note Service
*   **交互方向**：双向。
*   **Note -> Product** (Feign):
    *   `GET /product/simple/{id}`: 笔记展示商品卡片时，仅需商品图、标题、最低价。
*   **Product -> Note** (Feign):
    *   `GET /note/related/{productId}`: 商品详情页展示“关联笔记”列表。
*   **数据关联**：
    *   在 `Note` 表中增加 `product_id` (MVP简化，假设一篇笔记挂一个商品) 或关联表。

### 2.2 Product Service <-> Order Service
*   **交互方向**：Order -> Product。
*   **Order -> Product** (Feign):
    *   `POST /product/sku/lock`: 锁定库存（下单时）。
    *   `POST /product/sku/unlock`: 释放库存（取消订单/支付超时）。
    *   `GET /product/sku/{skuId}`: 获取下单时的商品快照信息（价格、规格）。

### 2.3 Product Service <-> Search Service
*   **交互方向**：异步消息 (MQ)。
*   **Product -> MQ -> Search**:
    *   商品上架/下架/修改 -> 发送 MQ 消息 -> Search 服务更新 ES 索引。
    *   Search 服务负责提供 `/search/product` 接口给前端。

---

## 3. 第一阶段数据库设计 (Schema MVP)

```sql
-- 1. 商品分类表 (支撑后台管理与筛选)
CREATE TABLE `rb_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL COMMENT '分类名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父分类ID',
  `level` int DEFAULT '1' COMMENT '层级',
  `icon` varchar(255) DEFAULT NULL COMMENT '图标',
  `sort` int DEFAULT '0' COMMENT '排序',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类';

-- 2. 商品主表 (SPU - Standard Product Unit)
CREATE TABLE `rb_product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `shop_id` bigint NOT NULL COMMENT '店铺ID',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `name` varchar(128) NOT NULL COMMENT '商品名称',
  `title` varchar(255) DEFAULT NULL COMMENT '副标题/卖点',
  `main_image` varchar(255) NOT NULL COMMENT '主图',
  `detail_images` text COMMENT '详情图(JSON数组)',
  `status` tinyint DEFAULT '0' COMMENT '状态: 0-下架, 1-上架',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_shop` (`shop_id`),
  KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SPU表';

-- 3. 商品规格表 (SKU - Stock Keeping Unit)
-- MVP阶段必须做，否则无法处理“颜色/尺码”和“库存管理”
CREATE TABLE `rb_sku` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL COMMENT '归属SPU ID',
  `name` varchar(128) NOT NULL COMMENT 'SKU名称 (如: 红色 64G)',
  `price` decimal(10,2) NOT NULL COMMENT '销售价',
  `stock` int NOT NULL DEFAULT '0' COMMENT '库存',
  `image` varchar(255) DEFAULT NULL COMMENT 'SKU特定图',
  `specs` json DEFAULT NULL COMMENT '规格属性JSON (如: {"color":"红", "size":"L"})',
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU表';

-- 注意：在 Note 服务中需添加字段
-- ALTER TABLE `rb_note` ADD COLUMN `product_id` bigint DEFAULT NULL COMMENT '关联商品ID';
```

## 4. 第一阶段接口清单 (API List)

### 4.1 内部接口 (Internal Feign Client)
供 Order, Note, Cart 服务调用。

| 接口方法 | 路径 | 描述 | 参数 | 返回 |
| :--- | :--- | :--- | :--- | :--- |
| `getSkuInfo` | `GET /internal/sku/{id}` | 获取SKU详情(含价格) | `skuId` | `SkuDTO` |
| `lockStock` | `POST /internal/sku/lock` | 锁定库存 | `List<SkuLockDTO>` | `Boolean` |
| `unlockStock` | `POST /internal/sku/unlock`| 释放库存 | `List<SkuLockDTO>` | `Boolean` |
| `deductStock` | `POST /internal/sku/deduct`| 真实扣减(支付后) | `List<SkuLockDTO>` | `Boolean` |

### 4.2 C端接口 (App Consumer)
供 App 前端调用。

| 接口方法 | 路径 | 描述 |
| :--- | :--- | :--- |
| `getProductDetail` | `GET /product/{id}` | **聚合接口**：返回 SPU信息 + SKU列表 + 店铺信息 + **关联笔记Top2** |
| `getProductList` | `GET /product/list` | 按分类/店铺分页查询 |
| `getProductComments` | `GET /product/{id}/comments` | (可选) 获取商品评价 |

### 4.3 B端接口 (Merchant Provider)
供商家后台使用。

| 接口方法 | 路径 | 描述 |
| :--- | :--- | :--- |
| `createProduct` | `POST /product` | 创建商品 (同时插入 SPU 和 SKU) |
| `updateProductStatus`| `PUT /product/{id}/status` | 上架/下架 |
| `updateSkuStock` | `PUT /product/sku/stock` | 补货 (直接改库存) |

## 5. 核心逻辑伪代码 (ProductServiceImpl)

### 商品详情页聚合 (Aggregation)
```java
public ProductVo getProductDetail(Long productId) {
    // 1. 并行查询 SPU 和 SKU
    CompletableFuture<Product> spuFuture = CompletableFuture.supplyAsync(() -> productMapper.selectById(productId));
    CompletableFuture<List<Sku>> skuFuture = CompletableFuture.supplyAsync(() -> skuMapper.selectByProductId(productId));
    
    // 2. 远程调用 Note 服务获取关联种草笔记 (内容电商核心)
    CompletableFuture<List<NoteSimpleDto>> noteFuture = CompletableFuture.supplyAsync(() -> noteClient.getRelatedNotes(productId));
    
    // 3. 组装结果
    ProductVo vo = new ProductVo();
    vo.setProduct(spuFuture.get());
    vo.setSkus(skuFuture.get());
    vo.setRelatedNotes(noteFuture.get()); // 展示"大家都在用"
    
    return vo;
}
```
