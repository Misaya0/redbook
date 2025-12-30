/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80026
 Source Host           : 10.1.40.66:3306
 Source Schema         : redbook

 Target Server Type    : MySQL
 Target Server Version : 80026
 File Encoding         : 65001

 Date: 01/12/2025 00:00:00
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for rb_comment
-- ----------------------------

use rb_note;

DROP TABLE IF EXISTS `rb_comment`;
CREATE TABLE `rb_comment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '内容',
  `time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '时间',
  `parent_id` bigint(20) DEFAULT '0' COMMENT '父id',
  `note_id` bigint(20) DEFAULT NULL COMMENT '笔记id',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_note_id` (`note_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='评论表';

SET FOREIGN_KEY_CHECKS = 1;


ALTER TABLE `rb_comment` ADD COLUMN `target_user_id` bigint(20) DEFAULT NULL COMMENT '回复目标用户id';
ALTER TABLE `rb_comment` ADD COLUMN `like_count` int(11) DEFAULT 0 COMMENT '点赞数';
