-- RedBook 项目时间字段类型迁移脚本
-- 将所有 String 类型的时间字段转换为 DATETIME 类型，并统一格式

-- 1. 用户表 (rb_user)
UPDATE `rb_user` SET `time` = NULL WHERE `time` = '';
ALTER TABLE `rb_user` MODIFY COLUMN `time` DATETIME;

-- 2. 笔记表 (rb_note_0, rb_note_1, rb_note_2)
-- 假设使用了 ShardingSphere 分表
UPDATE `rb_note_0` SET `time` = NULL WHERE `time` = '';
ALTER TABLE `rb_note_0` MODIFY COLUMN `time` DATETIME;

UPDATE `rb_note_1` SET `time` = NULL WHERE `time` = '';
ALTER TABLE `rb_note_1` MODIFY COLUMN `time` DATETIME;

UPDATE `rb_note_2` SET `time` = NULL WHERE `time` = '';
ALTER TABLE `rb_note_2` MODIFY COLUMN `time` DATETIME;

-- 3. 评论表 (rb_comment)
UPDATE `rb_comment` SET `time` = NULL WHERE `time` = '';
ALTER TABLE `rb_comment` MODIFY COLUMN `time` DATETIME;

-- 4. 关注表 (rb_attention)
UPDATE `rb_attention` SET `time` = NULL WHERE `time` = '';
ALTER TABLE `rb_attention` MODIFY COLUMN `time` DATETIME;

-- 5. 订单表 (rb_order)
UPDATE `rb_order` SET `create_time` = NULL WHERE `create_time` = '';
ALTER TABLE `rb_order` MODIFY COLUMN `create_time` DATETIME;

-- 6. 店铺表 (rb_shop)
UPDATE `rb_shop` SET `time` = NULL WHERE `time` = '';
ALTER TABLE `rb_shop` MODIFY COLUMN `time` DATETIME;

-- 注意：如果原数据格式不标准，ALTER TABLE 可能会报错。
-- 如果报错，请先执行格式化语句，例如：
-- UPDATE `rb_note_0` SET `time` = STR_TO_DATE(`time`, '%Y-%m-%d %H:%i:%s') WHERE `time` LIKE '%-%-% %:%:%';
