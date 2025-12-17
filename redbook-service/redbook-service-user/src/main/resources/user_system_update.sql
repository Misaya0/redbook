-- 1. 用户表：增加角色
ALTER TABLE `rb_user` ADD COLUMN `role` int DEFAULT 0 COMMENT '角色: 0-普通, 1-商家';

-- 2. 店铺表：增加挂链授权模式
ALTER TABLE `rb_shop` ADD COLUMN `link_auth_mode` tinyint DEFAULT 0 COMMENT '挂链授权模式: 0-自动, 1-手动';

-- 3. 笔记表：增加挂链状态和店铺ID(冗余，方便查询)
-- 注意：如果是分表，请对所有分表执行
ALTER TABLE `rb_note_1` ADD COLUMN `product_link_status` tinyint DEFAULT 1 COMMENT '挂链状态: 0-待审, 1-正常, 2-拒绝';
ALTER TABLE `rb_note_1` ADD COLUMN `shop_id` bigint DEFAULT NULL COMMENT '关联商品的店铺ID';

ALTER TABLE `rb_note_2` ADD COLUMN `product_link_status` tinyint DEFAULT 1 COMMENT '挂链状态: 0-待审, 1-正常, 2-拒绝';
ALTER TABLE `rb_note_2` ADD COLUMN `shop_id` bigint DEFAULT NULL COMMENT '关联商品的店铺ID';

ALTER TABLE `rb_note_0` ADD COLUMN `product_link_status` tinyint DEFAULT 1 COMMENT '挂链状态: 0-待审, 1-正常, 2-拒绝';
ALTER TABLE `rb_note_0` ADD COLUMN `shop_id` bigint DEFAULT NULL COMMENT '关联商品的店铺ID';
