CREATE DATABASE IF NOT EXISTS `rb_message` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `rb_message`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for rb_conversation
-- 采用"双向会话"设计(Mailbox模式)，即A和B的对话会产生两条记录：
-- 1. user_id=A, talker_id=B (A的视角)
-- 2. user_id=B, talker_id=A (B的视角)
-- 这样方便管理各自的未读数、置顶、删除状态
-- ----------------------------
DROP TABLE IF EXISTS `rb_conversation`;
CREATE TABLE `rb_conversation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '所属用户ID',
  `talker_id` bigint(20) NOT NULL COMMENT '对话方用户ID',
  `last_message_content` varchar(1000) DEFAULT NULL COMMENT '最后一条消息内容快照',
  `last_message_time` datetime DEFAULT NULL COMMENT '最后一条消息时间',
  `unread_count` int(11) DEFAULT '0' COMMENT '未读消息数',
  `is_top` tinyint(1) DEFAULT '0' COMMENT '是否置顶 0:否 1:是',
  `is_muted` tinyint(1) DEFAULT '0' COMMENT '是否免打扰 0:否 1:是',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_talker` (`user_id`,`talker_id`) USING BTREE,
  KEY `idx_user_time` (`user_id`,`update_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

-- ----------------------------
-- Table structure for rb_message
-- 消息存储表
-- ----------------------------
DROP TABLE IF EXISTS `rb_message`;
CREATE TABLE `rb_message` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sender_id` bigint(20) NOT NULL COMMENT '发送者ID',
  `receiver_id` bigint(20) NOT NULL COMMENT '接收者ID',
  `content` text NOT NULL COMMENT '消息内容',
  `msg_type` int(4) DEFAULT '0' COMMENT '消息类型 0:文本 1:图片 2:语音',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_sender_receiver` (`sender_id`,`receiver_id`) USING BTREE,
  KEY `idx_receiver_sender` (`receiver_id`,`sender_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

SET FOREIGN_KEY_CHECKS = 1;
