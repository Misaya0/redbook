USE `rb_message`;

DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `recipient_id` bigint(20) NOT NULL COMMENT '被通知的用户ID',
  `actor_id` bigint(20) NOT NULL COMMENT '触发行为的用户ID',
  `type` varchar(32) NOT NULL COMMENT '通知类型: FOLLOW/LIKE/COLLECT/COMMENT/REPLY',
  `target_type` varchar(32) NOT NULL COMMENT '目标类型: USER/NOTE/COMMENT',
  `target_id` bigint(20) NOT NULL COMMENT '目标ID',
  `content_snapshot` varchar(512) DEFAULT NULL COMMENT '内容快照',
  `is_read` tinyint(1) DEFAULT '0' COMMENT '是否已读 0:未读 1:已读',
  `event_id` varchar(64) NOT NULL COMMENT '事件ID，用于幂等',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_id` (`event_id`),
  KEY `idx_recipient_type_read` (`recipient_id`, `type`, `is_read`),
  KEY `idx_recipient_created` (`recipient_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知表';

DROP TABLE IF EXISTS `unread_counter`;
CREATE TABLE `unread_counter` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `recipient_id` bigint(20) NOT NULL COMMENT '用户ID',
  `type` varchar(32) NOT NULL COMMENT '消息分组类型: LIKE_COLLECT/FOLLOW/COMMENT',
  `unread_count` int(11) DEFAULT '0' COMMENT '未读数量',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recipient_type` (`recipient_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='未读计数表';
