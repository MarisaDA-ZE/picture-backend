-- 用户表 --
CREATE TABLE IF NOT EXISTS user  (
    `id` bigint NOT NULL COMMENT '主键ID',
    `account` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '账号',
    `nick_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '昵称',
    `password` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
    `role` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'user' COMMENT '用户角色（user、admin）',
    `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
    `email` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱号',
    `profile` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '个人描述',
    `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户头像（URL地址）',
    `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除（0:未删除，1:已删除）',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `edit_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '编辑时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `idx_account`(`account` ASC) USING BTREE COMMENT '账号唯一索引',
    UNIQUE INDEX `idx_phone`(`phone` ASC) USING BTREE COMMENT '手机号唯一索引',
    UNIQUE INDEX `idx_email`(`email` ASC) USING BTREE COMMENT '邮箱唯一索引'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- 会员表 --
CREATE TABLE IF NOT EXISTS user_vip (
    `id` bigint NOT NULL COMMENT '主键ID',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `vip_code` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'VIP码',
    `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `expire_time` datetime NULL DEFAULT NULL COMMENT 'VIP过期时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `idx_user_id`(`user_id` ASC) USING BTREE COMMENT '用户ID索引'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会员表' ROW_FORMAT = Dynamic;

-- 图片表 --
CREATE TABLE IF NOT EXISTS picture (
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
    INDEX `idx_m_hue_bucket`(`m_hue_bucket` ASC) USING BTREE COMMENT '主色调分量索引'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '图片表' ROW_FORMAT = Dynamic;

-- 空间表 --
CREATE TABLE IF NOT EXISTS space (
    `id` bigint NOT NULL COMMENT '主键ID',
    `user_id` bigint NOT NULL COMMENT '所属用户ID',
    `space_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '空间名称',
    `space_level` tinyint NOT NULL DEFAULT 0 COMMENT '空间等级（0-普通版、1-专业版、2-旗舰版）',
    `max_size` bigint NULL DEFAULT 0 COMMENT '最大存储空间（Byte）',
    `max_count` int NULL DEFAULT 0 COMMENT '最大存储数量（张）',
    `total_size` bigint NULL DEFAULT NULL COMMENT '已使用的空间大小（Byte）',
    `total_count` int NULL DEFAULT NULL COMMENT '已使用的存储数量（张）',
    `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `edit_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
    `is_delete` tinyint NULL DEFAULT 0 COMMENT '是否删除（0-未删除、1-已删除）',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_user_id`(`user_id` ASC) USING BTREE COMMENT '用户ID索引',
    INDEX `idx_space_name`(`space_name` ASC) USING BTREE COMMENT '空间名称索引',
    INDEX `idx_space_level`(`space_level` ASC) USING BTREE COMMENT '空间等级索引'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '空间表' ROW_FORMAT = Dynamic;








