-- ============================================
-- RedBook 项目数据库表结构
-- ============================================

-- ============================================
-- 1. rb_user 数据库 - 用户服务
-- ============================================

-- 用户表
CREATE DATABASE IF NOT EXISTS `rb_user` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `rb_user`;

CREATE TABLE IF NOT EXISTS `rb_user` (
    `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `image` VARCHAR(255) DEFAULT NULL COMMENT '头像',
    `number` BIGINT(20) DEFAULT NULL COMMENT '小红书号',
    `sex` VARCHAR(10) DEFAULT NULL COMMENT '性别',
    `birthday` VARCHAR(20) DEFAULT NULL COMMENT '生日',
    `address` VARCHAR(100) DEFAULT NULL COMMENT '地区',
    `identity` VARCHAR(50) DEFAULT NULL COMMENT '身份',
    `school` VARCHAR(100) DEFAULT NULL COMMENT '学校',
    `time` VARCHAR(20) DEFAULT NULL COMMENT '注册时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_number` (`number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 关注表
CREATE TABLE IF NOT EXISTS `rb_attention` (
    `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `other_id` INT(11) NOT NULL COMMENT '被关注者id',
    `own_id` INT(11) NOT NULL COMMENT '关注者id',
    `time` VARCHAR(20) DEFAULT NULL COMMENT '关注时间',
    PRIMARY KEY (`id`),
    KEY `idx_other_id` (`other_id`),
    KEY `idx_own_id` (`own_id`),
    UNIQUE KEY `uk_attention` (`own_id`, `other_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关注表';

-- ============================================
-- 2. rb_note 数据库 - 笔记服务
-- ============================================

CREATE DATABASE IF NOT EXISTS `rb_note` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `rb_note`;

-- 笔记表（分表：rb_note_0, rb_note_1, rb_note_2）
CREATE TABLE IF NOT EXISTS `rb_note_0` (
    `id` BIGINT(20) NOT NULL COMMENT '主键（雪花算法）',
    `title` VARCHAR(200) DEFAULT NULL COMMENT '标题',
    `content` TEXT COMMENT '内容',
    `image` VARCHAR(500) DEFAULT NULL COMMENT '图片',
    `time` VARCHAR(20) DEFAULT NULL COMMENT '发布时间',
    `type` VARCHAR(50) DEFAULT NULL COMMENT '类型',
    `address` VARCHAR(200) DEFAULT NULL COMMENT '地址',
    `longitude` DOUBLE DEFAULT NULL COMMENT '经度',
    `latitude` DOUBLE DEFAULT NULL COMMENT '纬度',
    `like` INT(11) DEFAULT 0 COMMENT '点赞数',
    `collection` INT(11) DEFAULT 0 COMMENT '收藏数',
    `user_id` INT(11) DEFAULT NULL COMMENT '发布人id',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_time` (`time`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记表0';

CREATE TABLE IF NOT EXISTS `rb_note_1` (
    `id` BIGINT(20) NOT NULL COMMENT '主键（雪花算法）',
    `title` VARCHAR(200) DEFAULT NULL COMMENT '标题',
    `content` TEXT COMMENT '内容',
    `image` VARCHAR(500) DEFAULT NULL COMMENT '图片',
    `time` VARCHAR(20) DEFAULT NULL COMMENT '发布时间',
    `type` VARCHAR(50) DEFAULT NULL COMMENT '类型',
    `address` VARCHAR(200) DEFAULT NULL COMMENT '地址',
    `longitude` DOUBLE DEFAULT NULL COMMENT '经度',
    `latitude` DOUBLE DEFAULT NULL COMMENT '纬度',
    `like` INT(11) DEFAULT 0 COMMENT '点赞数',
    `collection` INT(11) DEFAULT 0 COMMENT '收藏数',
    `user_id` INT(11) DEFAULT NULL COMMENT '发布人id',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_time` (`time`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记表1';

CREATE TABLE IF NOT EXISTS `rb_note_2` (
    `id` BIGINT(20) NOT NULL COMMENT '主键（雪花算法）',
    `title` VARCHAR(200) DEFAULT NULL COMMENT '标题',
    `content` TEXT COMMENT '内容',
    `image` VARCHAR(500) DEFAULT NULL COMMENT '图片',
    `time` VARCHAR(20) DEFAULT NULL COMMENT '发布时间',
    `type` VARCHAR(50) DEFAULT NULL COMMENT '类型',
    `address` VARCHAR(200) DEFAULT NULL COMMENT '地址',
    `longitude` DOUBLE DEFAULT NULL COMMENT '经度',
    `latitude` DOUBLE DEFAULT NULL COMMENT '纬度',
    `like` INT(11) DEFAULT 0 COMMENT '点赞数',
    `collection` INT(11) DEFAULT 0 COMMENT '收藏数',
    `user_id` INT(11) DEFAULT NULL COMMENT '发布人id',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_time` (`time`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记表2';

-- 话题表
CREATE TABLE IF NOT EXISTS `rb_topic` (
    `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `content` VARCHAR(100) DEFAULT NULL COMMENT '内容',
    `hot` INT(11) DEFAULT 0 COMMENT '热度',
    PRIMARY KEY (`id`),
    KEY `idx_hot` (`hot`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='话题表';

-- 笔记话题关联表
CREATE TABLE IF NOT EXISTS `rb_note_topic` (
    `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `topic_id` INT(11) NOT NULL COMMENT '话题id',
    `note_id` BIGINT(20) NOT NULL COMMENT '笔记id',
    PRIMARY KEY (`id`),
    KEY `idx_topic_id` (`topic_id`),
    KEY `idx_note_id` (`note_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记话题关联表';

-- 收藏表
CREATE TABLE IF NOT EXISTS `rb_collection` (
    `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `note_id` INT(11) NOT NULL COMMENT '笔记id',
    `user_id` INT(11) NOT NULL COMMENT '用户id',
    PRIMARY KEY (`id`),
    KEY `idx_note_id` (`note_id`),
    KEY `idx_user_id` (`user_id`),
    UNIQUE KEY `uk_collection` (`user_id`, `note_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- 点赞表
CREATE TABLE IF NOT EXISTS `rb_like` (
    `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `note_id` INT(11) NOT NULL COMMENT '笔记id',
    `user_id` INT(11) NOT NULL COMMENT '用户id',
    PRIMARY KEY (`id`),
    KEY `idx_note_id` (`note_id`),
    KEY `idx_user_id` (`user_id`),
    UNIQUE KEY `uk_like` (`user_id`, `note_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点赞表';

-- 笔记浏览表
CREATE TABLE IF NOT EXISTS `rb_note_browse` (
    `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `note_id` INT(11) NOT NULL COMMENT '笔记id',
    `user_id` INT(11) NOT NULL COMMENT '用户id',
    `time` VARCHAR(20) DEFAULT NULL COMMENT '浏览时间',
    PRIMARY KEY (`id`),
    KEY `idx_note_id` (`note_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_time` (`time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记浏览表';

-- ============================================
-- 3. rb_product 数据库 - 商品服务
-- ============================================

CREATE DATABASE IF NOT EXISTS `rb_product` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `rb_product`;

-- 店铺表
CREATE TABLE IF NOT EXISTS `rb_shop` (
    `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` VARCHAR(100) DEFAULT NULL COMMENT '店铺名称',
    `image` VARCHAR(255) DEFAULT NULL COMMENT '头像',
    `time` VARCHAR(20) DEFAULT NULL COMMENT '成立时间',
    `fans` INT(11) DEFAULT 0 COMMENT '粉丝数',
    `sales` INT(11) DEFAULT 0 COMMENT '销量',
    `user_id` INT(11) DEFAULT NULL COMMENT '用户id',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='店铺表';

-- 商品表
CREATE TABLE IF NOT EXISTS `rb_product` (
    `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` VARCHAR(200) DEFAULT NULL COMMENT '商品名称',
    `price` DECIMAL(10,2) DEFAULT NULL COMMENT '商品价格',
    `image` VARCHAR(500) DEFAULT NULL COMMENT '商品图片',
    `time` VARCHAR(20) DEFAULT NULL COMMENT '发布时间',
    `sales` INT(11) DEFAULT 0 COMMENT '销量',
    `user_id` INT(11) DEFAULT NULL COMMENT '用户id',
    `shop_id` INT(11) DEFAULT NULL COMMENT '店铺id',
    `stock` INT(11) DEFAULT 0 COMMENT '库存',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_time` (`time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- 优惠券表
CREATE TABLE IF NOT EXISTS `rb_coupon` (
    `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` VARCHAR(100) DEFAULT NULL COMMENT '名称',
    `price` DECIMAL(10,2) DEFAULT NULL COMMENT '价格',
    `condition` DECIMAL(10,2) DEFAULT NULL COMMENT '使用条件',
    `time` DATETIME DEFAULT NULL COMMENT '有效截至时间',
    `stock` INT(11) DEFAULT 0 COMMENT '库存',
    PRIMARY KEY (`id`),
    KEY `idx_time` (`time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券表';

-- 用户优惠券表
CREATE TABLE IF NOT EXISTS `rb_user_coupon` (
    `user_id` INT(11) NOT NULL COMMENT '用户id',
    `coupon_id` INT(11) NOT NULL COMMENT '优惠券id',
    PRIMARY KEY (`user_id`, `coupon_id`),
    KEY `idx_coupon_id` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户优惠券表';

-- 商品浏览表
CREATE TABLE IF NOT EXISTS `rb_product_browse` (
    `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `product_id` INT(11) NOT NULL COMMENT '产品id',
    `user_id` INT(11) NOT NULL COMMENT '用户id',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品浏览表';

-- ============================================
-- 4. rb_order 数据库 - 订单服务
-- ============================================

CREATE DATABASE IF NOT EXISTS `rb_order` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `rb_order`;

-- 订单表
CREATE TABLE IF NOT EXISTS `rb_order` (
    `id` BIGINT(20) NOT NULL COMMENT '主键（雪花算法）',
    `product_id` INT(11) NOT NULL COMMENT '产品id',
    `quantity` INT(11) DEFAULT 1 COMMENT '商品数量',
    `coupon_id` INT(11) DEFAULT NULL COMMENT '优惠券id',
    `final_price` DECIMAL(10,2) DEFAULT NULL COMMENT '最终价格',
    `user_id` INT(11) NOT NULL COMMENT '订单归属人ID',
    `status` INT(11) DEFAULT 0 COMMENT '状态（0-待支付，1-已支付，2-已发货，3-已完成，4-已取消）',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 订单属性表
CREATE TABLE IF NOT EXISTS `rb_order_attribute` (
    `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id` BIGINT(20) NOT NULL COMMENT '订单id',
    `label` VARCHAR(50) DEFAULT NULL COMMENT '属性标签',
    `value` VARCHAR(200) DEFAULT NULL COMMENT '属性值',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单属性表';

-- ============================================
-- 5. rb_search 数据库 - 搜索服务
-- ============================================

CREATE DATABASE IF NOT EXISTS `rb_search` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `rb_search`;

-- 搜索历史表
CREATE TABLE IF NOT EXISTS `rb_history` (
    `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `history` VARCHAR(200) DEFAULT NULL COMMENT '记录',
    `user_id` INT(11) NOT NULL COMMENT '用户id',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搜索历史表';

