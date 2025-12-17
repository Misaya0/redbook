-- 如果项目使用了 ShardingSphere 分表，通常物理表可能是 rb_note_0, rb_note_1 等
-- 鉴于您提到是 rb_note1, rb_note2, rb_note3，请执行以下语句：
use rb_note;
ALTER TABLE `rb_note_0` ADD COLUMN `product_id` bigint DEFAULT NULL COMMENT '关联商品ID';
ALTER TABLE `rb_note_1` ADD COLUMN `product_id` bigint DEFAULT NULL COMMENT '关联商品ID';
ALTER TABLE `rb_note_2` ADD COLUMN `product_id` bigint DEFAULT NULL COMMENT '关联商品ID';

-- 建议为该字段添加索引，以加速 getRelatedNotes 查询
CREATE INDEX `idx_product_id` ON `rb_note_0` (`product_id`);
CREATE INDEX `idx_product_id` ON `rb_note_1` (`product_id`);
CREATE INDEX `idx_product_id` ON `rb_note_2` (`product_id`);
