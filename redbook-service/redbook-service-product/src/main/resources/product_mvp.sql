-- 1. 商品分类表
CREATE TABLE `rb_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL COMMENT '分类名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父分类ID',
  `level` int DEFAULT '1' COMMENT '层级',
  `icon` varchar(255) DEFAULT NULL COMMENT '图标',
  `sort` int DEFAULT '0' COMMENT '排序',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类';

-- 初始分类数据
INSERT INTO `rb_category` (`id`, `name`, `parent_id`, `level`, `icon`, `sort`) VALUES (1, '电脑/办公', 0, 1, NULL, 10);
INSERT INTO `rb_category` (`id`, `name`, `parent_id`, `level`, `icon`, `sort`) VALUES (2, '手机/数码', 0, 1, NULL, 20);
INSERT INTO `rb_category` (`id`, `name`, `parent_id`, `level`, `icon`, `sort`) VALUES (3, '家用电器', 0, 1, NULL, 30);
INSERT INTO `rb_category` (`id`, `name`, `parent_id`, `level`, `icon`, `sort`) VALUES (4, '家居/家具', 0, 1, NULL, 40);
INSERT INTO `rb_category` (`id`, `name`, `parent_id`, `level`, `icon`, `sort`) VALUES (5, '服饰/内衣', 0, 1, NULL, 50);
INSERT INTO `rb_category` (`id`, `name`, `parent_id`, `level`, `icon`, `sort`) VALUES (6, '鞋/运动', 0, 1, NULL, 60);
INSERT INTO `rb_category` (`id`, `name`, `parent_id`, `level`, `icon`, `sort`) VALUES (7, '汽车用品', 0, 1, NULL, 70);

-- 二级分类
-- 电脑/办公
INSERT INTO `rb_category` (`name`, `parent_id`, `level`, `icon`, `sort`) VALUES ('笔记本', 1, 2, NULL, 1);
INSERT INTO `rb_category` (`name`, `parent_id`, `level`, `icon`, `sort`) VALUES ('台式机', 1, 2, NULL, 2);
INSERT INTO `rb_category` (`name`, `parent_id`, `level`, `icon`, `sort`) VALUES ('办公设备', 1, 2, NULL, 3);

-- 手机/数码
INSERT INTO `rb_category` (`name`, `parent_id`, `level`, `icon`, `sort`) VALUES ('手机', 2, 2, NULL, 1);
INSERT INTO `rb_category` (`name`, `parent_id`, `level`, `icon`, `sort`) VALUES ('智能设备', 2, 2, NULL, 2);
INSERT INTO `rb_category` (`name`, `parent_id`, `level`, `icon`, `sort`) VALUES ('摄影摄像', 2, 2, NULL, 3);

-- 服饰/内衣
INSERT INTO `rb_category` (`name`, `parent_id`, `level`, `icon`, `sort`) VALUES ('男装', 5, 2, NULL, 1);
INSERT INTO `rb_category` (`name`, `parent_id`, `level`, `icon`, `sort`) VALUES ('女装', 5, 2, NULL, 2);
INSERT INTO `rb_category` (`name`, `parent_id`, `level`, `icon`, `sort`) VALUES ('内衣', 5, 2, NULL, 3);

-- 2. 商品主表 (SPU)
CREATE TABLE `rb_product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `shop_id` bigint NOT NULL COMMENT '店铺ID',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `name` varchar(128) NOT NULL COMMENT '商品名称',
  `title` varchar(255) DEFAULT NULL COMMENT '副标题/卖点',
  `price` decimal(10,2) DEFAULT NULL COMMENT '展示价格',
  `main_image` varchar(255) NOT NULL COMMENT '主图',
  `detail_images` text COMMENT '详情图(JSON数组)',
  `status` tinyint DEFAULT '0' COMMENT '状态: 0-下架, 1-上架',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_shop` (`shop_id`),
  KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SPU表';

-- 3. 商品规格表 (SKU)
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

ALTER TABLE `rb_product` ADD COLUMN `price` decimal(10,2) DEFAULT NULL COMMENT '展示价格';

CREATE TABLE `rb_spec` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `spec_code` varchar(64) NOT NULL COMMENT '规格编码(稳定key, 如 color/size/storage/package)',
  `spec_name` varchar(64) NOT NULL COMMENT '规格名称(展示用)',
  `display_type` varchar(16) NOT NULL DEFAULT 'chip' COMMENT '展示类型: chip/list',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_spec_code` (`spec_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规格组定义';

CREATE TABLE `rb_spec_option` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `spec_id` bigint NOT NULL COMMENT '规格组ID',
  `option_value` varchar(64) NOT NULL COMMENT '选项值(稳定值)',
  `option_label` varchar(255) NOT NULL COMMENT '选项展示文案',
  `image` varchar(255) DEFAULT NULL COMMENT '选项图片(可选)',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `idx_spec` (`spec_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规格组选项';

CREATE TABLE `rb_category_spec` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_id` bigint NOT NULL COMMENT '品类ID',
  `spec_id` bigint NOT NULL COMMENT '规格组ID',
  `sort` int NOT NULL DEFAULT '0' COMMENT '规格组在该品类下的排序',
  `required` tinyint NOT NULL DEFAULT '1' COMMENT '是否必选: 0-否,1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_spec` (`category_id`,`spec_id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_spec` (`spec_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='品类-规格组关系';

INSERT INTO `rb_spec` (`spec_code`,`spec_name`,`display_type`,`sort`) VALUES
('version','版本','chip',10),
('color','颜色分类','list',20),
('disk','硬盘容量','chip',30),
('ram','内存容量','chip',40),
('storage','存储容量','chip',30),
('size','尺码','chip',30),
('package','套餐类型','chip',50);

INSERT INTO `rb_category_spec` (`category_id`,`spec_id`,`sort`,`required`)
SELECT 1, id, 10, 1 FROM `rb_spec` WHERE `spec_code` = 'version';
INSERT INTO `rb_category_spec` (`category_id`,`spec_id`,`sort`,`required`)
SELECT 1, id, 20, 1 FROM `rb_spec` WHERE `spec_code` = 'color';
INSERT INTO `rb_category_spec` (`category_id`,`spec_id`,`sort`,`required`)
SELECT 1, id, 30, 1 FROM `rb_spec` WHERE `spec_code` = 'disk';
INSERT INTO `rb_category_spec` (`category_id`,`spec_id`,`sort`,`required`)
SELECT 1, id, 40, 1 FROM `rb_spec` WHERE `spec_code` = 'ram';
INSERT INTO `rb_category_spec` (`category_id`,`spec_id`,`sort`,`required`)
SELECT 1, id, 50, 0 FROM `rb_spec` WHERE `spec_code` = 'package';

INSERT INTO `rb_category_spec` (`category_id`,`spec_id`,`sort`,`required`)
SELECT 2, id, 10, 1 FROM `rb_spec` WHERE `spec_code` = 'color';
INSERT INTO `rb_category_spec` (`category_id`,`spec_id`,`sort`,`required`)
SELECT 2, id, 20, 1 FROM `rb_spec` WHERE `spec_code` = 'storage';
INSERT INTO `rb_category_spec` (`category_id`,`spec_id`,`sort`,`required`)
SELECT 2, id, 30, 0 FROM `rb_spec` WHERE `spec_code` = 'package';

INSERT INTO `rb_category_spec` (`category_id`,`spec_id`,`sort`,`required`)
SELECT 5, id, 10, 1 FROM `rb_spec` WHERE `spec_code` = 'color';
INSERT INTO `rb_category_spec` (`category_id`,`spec_id`,`sort`,`required`)
SELECT 5, id, 20, 1 FROM `rb_spec` WHERE `spec_code` = 'size';
