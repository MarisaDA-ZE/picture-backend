/*
 Navicat Premium Data Transfer

 Source Server         : 10.254@picture_backend
 Source Server Type    : MySQL
 Source Server Version : 80039
 Source Host           : 192.168.10.254:3306
 Source Schema         : picture_backend

 Target Server Type    : MySQL
 Target Server Version : 80039
 File Encoding         : 65001

 Date: 24/04/2025 05:10:14
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for notice
-- ----------------------------
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice`  (
    `id` bigint NOT NULL COMMENT '主键ID',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `notice_type` tinyint NOT NULL DEFAULT 0 COMMENT '消息类型（0-系统消息；1-用户消息）',
    `is_read` tinyint NOT NULL DEFAULT 0 COMMENT '是否已读（0-未读；1-已读）',
    `content` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
    `sender_id` bigint NOT NULL DEFAULT 0 COMMENT '发送方ID（0-系统；<ID>-用户）',
    `additional_params` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '一些附加信息（JSON格式）',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '是否被删除（0-未删除；1-已删除）',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_user_id`(`user_id` ASC) USING BTREE COMMENT '接收用户ID普通索引',
    INDEX `idx_sender_id`(`sender_id` ASC) USING BTREE COMMENT '发送用户ID普通索引',
    INDEX `udx_user_id_is_read`(`user_id` ASC, `is_read` ASC) USING BTREE COMMENT '用户ID和阅读状态的联合索引',
    INDEX `udx_userid_notice_type`(`user_id` ASC, `notice_type` ASC) USING BTREE COMMENT '用户ID和消息类型的联合索引'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '-- 用户消息表，用于记录用户接收到的各类消息 --' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for picture
-- ----------------------------
DROP TABLE IF EXISTS `picture`;
CREATE TABLE `picture`  (
    `id` bigint NOT NULL COMMENT '主键ID',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `space_id` bigint NULL DEFAULT NULL COMMENT '所属空间ID（为 NULL 表示公共空间）',
    `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图片名称',
    `url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图片URL地址',
    `url_thumb` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '拇指图尺寸URL',
    `url_original` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '原图尺寸URL',
    `saved_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件在文件服务器上的路径',
    `thumb_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '缩略图在文件服务器上的地址',
    `original_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '原图在文件服务器上的地址',
    `introduction` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图片描述',
    `fingerprint` varchar(48) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图片指纹（md5值，只为原图生成）',
    `category` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图片分类',
    `tags` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图片标签（JSON数组）',
    `pic_size` bigint NULL DEFAULT NULL COMMENT '图片大小',
    `pic_width` int NULL DEFAULT NULL COMMENT '图片宽度',
    `pic_height` int NULL DEFAULT NULL COMMENT '图片高度',
    `pic_color` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图片主色调（r,g,b）',
    `m_color_hue` float NULL DEFAULT NULL COMMENT '主要颜色的色调（0~360°）',
    `m_color_saturation` float NULL DEFAULT NULL COMMENT '主要颜色的饱和度（0~100°）',
    `m_color_value` float NULL DEFAULT NULL COMMENT '主要颜色的明度（0~100°）',
    `m_hue_bucket` tinyint NULL DEFAULT NULL COMMENT '主色调分桶量（每10°一个桶，共36个）',
    `m_sat_bucket` tinyint NULL DEFAULT NULL COMMENT '主要颜色的饱和度桶（每10°一个桶，共10个）',
    `m_val_bucket` tinyint NULL DEFAULT NULL COMMENT '主要颜色的明度桶（每10°一个桶，共10个）',
    `pic_scale` double NULL DEFAULT NULL COMMENT '图片长宽比',
    `pic_format` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图片格式',
    `review_status` tinyint NULL DEFAULT 0 COMMENT '审核状态（0:待审核，1:已通过，2:已拒绝）',
    `review_message` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核信息',
    `reviewer_id` bigint NULL DEFAULT NULL COMMENT '审核员ID',
    `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `edit_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
    `review_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
    `is_delete` tinyint NULL DEFAULT 0 COMMENT '逻辑删除（0:未删除，1:已删除）',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_user_id`(`user_id` ASC) USING BTREE COMMENT '用户ID索引',
    INDEX `idx_space_id`(`space_id` ASC) USING BTREE COMMENT '所属空间索引',
    INDEX `idx_name`(`name` ASC) USING BTREE COMMENT '图片名称索引',
    INDEX `idx_category`(`category` ASC) USING BTREE COMMENT '图片分分类索引',
    INDEX `idx_tags`(`tags` ASC) USING BTREE COMMENT '图片标签索引',
    INDEX `idx_introduction`(`introduction` ASC) USING BTREE COMMENT '图片描述索引',
    INDEX `idx_review_status`(`review_status` ASC) USING BTREE COMMENT '审核状态索引',
    INDEX `idx_reviewer_id`(`reviewer_id` ASC) USING BTREE COMMENT '审核员ID索引',
    INDEX `idx_fingerprint`(`fingerprint` ASC) USING BTREE COMMENT '文件指纹索引',
    INDEX `idx_hsv_bucket`(`m_hue_bucket` ASC, `m_sat_bucket` ASC, `m_val_bucket` ASC) USING BTREE COMMENT '图片主要颜色hsv分桶的联合索引'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '图片表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for space
-- ----------------------------
DROP TABLE IF EXISTS `space`;
CREATE TABLE `space`  (
    `id` bigint NOT NULL COMMENT '主键ID',
    `user_id` bigint NOT NULL COMMENT '所属用户ID',
    `space_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '空间名称',
    `space_level` tinyint NOT NULL DEFAULT 0 COMMENT '空间等级（0-普通版、1-专业版、2-旗舰版）',
    `space_type` tinyint NOT NULL DEFAULT 0 COMMENT '空间类型（0-私有，1-团队）',
    `max_size` bigint NULL DEFAULT 0 COMMENT '最大存储空间（Byte）',
    `max_count` int NULL DEFAULT 0 COMMENT '最大存储数量（张）',
    `total_size` bigint NULL DEFAULT 0 COMMENT '已使用的空间大小（Byte）',
    `total_count` int NULL DEFAULT 0 COMMENT '已使用的存储数量（张）',
    `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `edit_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
    `is_delete` tinyint NULL DEFAULT 0 COMMENT '是否删除（0-未删除、1-已删除）',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_user_id`(`user_id` ASC) USING BTREE COMMENT '用户ID索引',
    INDEX `idx_space_name`(`space_name` ASC) USING BTREE COMMENT '空间名称索引',
    INDEX `idx_space_level`(`space_level` ASC) USING BTREE COMMENT '空间等级索引',
    INDEX `idx_space_type`(`space_type` ASC) USING BTREE COMMENT '空间类型索引'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '空间表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for space_user
-- ----------------------------
DROP TABLE IF EXISTS `space_user`;
CREATE TABLE `space_user`  (
    `id` bigint NOT NULL COMMENT '主键ID',
    `space_id` bigint NOT NULL COMMENT '空间ID',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `space_role` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '空间角色（viewer、editor、admin）',
    `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_space_id_user_id`(`space_id` ASC, `user_id` ASC) USING BTREE COMMENT '空间ID和用户ID的唯一索引（一个用户在一个空间中只能有一个角色）',
    INDEX `idx_space_id`(`space_id` ASC) USING BTREE COMMENT '空间ID的索引',
    INDEX `idx_user_id`(`user_id` ASC) USING BTREE COMMENT '用户ID的索引'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '空间-用户的关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
    `id` bigint NOT NULL COMMENT '主键ID',
    `account` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '账号',
    `nick_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '昵称',
    `password` varchar(68) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
    `role` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'user' COMMENT '用户角色（user、admin）',
    `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
    `email` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱号',
    `profile` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '个人描述',
    `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户头像（URL地址）',
    `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除（0：未删除，1：已删除）',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `edit_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '编辑时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `idx_account`(`account` ASC) USING BTREE COMMENT '账号唯一索引',
    UNIQUE INDEX `idx_phone`(`phone` ASC) USING BTREE COMMENT '手机号唯一索引',
    UNIQUE INDEX `idx_email`(`email` ASC) USING BTREE COMMENT '邮箱唯一索引'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_vip
-- ----------------------------
DROP TABLE IF EXISTS `user_vip`;
CREATE TABLE `user_vip`  (
    `id` bigint NOT NULL COMMENT '主键ID',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `vip_code` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'VIP码',
    `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `expire_time` datetime NULL DEFAULT NULL COMMENT 'VIP过期时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `idx_user_id`(`user_id` ASC) USING BTREE COMMENT '用户ID索引'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会员表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;


-- 图床API密钥表
DROP TABLE IF EXISTS `api_key`;
CREATE TABLE `api_key` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `access_key` varchar(64) NOT NULL COMMENT '访问密钥',
    `secret_key` varchar(64) NOT NULL COMMENT '密钥',
    `name` varchar(64) NOT NULL COMMENT '密钥名称',
    `description` varchar(255) DEFAULT NULL COMMENT '密钥描述',
    `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-禁用 1-启用',
    `daily_limit` int NOT NULL DEFAULT '1000' COMMENT '每日调用限制',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_access_key` (`access_key`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图床API密钥表';

-- API调用记录表
DROP TABLE IF EXISTS `api_call_record`;
CREATE TABLE `api_call_record` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `api_key_id` bigint NOT NULL COMMENT 'API密钥ID',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `call_type` varchar(32) NOT NULL COMMENT '调用类型',
    `call_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
    `ip` varchar(64) DEFAULT NULL COMMENT '调用IP',
    `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-失败 1-成功',
    PRIMARY KEY (`id`),
    KEY `idx_api_key_id` (`api_key_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_call_time` (`call_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API调用记录表';

-- API密钥与空间关联表
DROP TABLE IF EXISTS `api_key_space`;
CREATE TABLE `api_key_space` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `api_key_id` bigint NOT NULL COMMENT 'API密钥ID',
    `space_id` bigint NOT NULL COMMENT '空间ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_api_key_space` (`api_key_id`,`space_id`),
    KEY `idx_space_id` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API密钥与空间关联表';