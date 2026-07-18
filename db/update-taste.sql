-- ============================================================
-- 为已有菜品填充 taste / spiciness（AI 推荐用）
-- 在 DataGrip / MySQL 中直接运行
-- ============================================================

ALTER TABLE `dish` ADD COLUMN `taste`      varchar(32) DEFAULT NULL COMMENT '口味(麻辣/酸辣/咸鲜/清淡/甜/蒜香)';
ALTER TABLE `dish` ADD COLUMN `spiciness`  varchar(32) DEFAULT NULL COMMENT '辣度(不辣/微辣/中辣/特辣)';

-- 鱼类
UPDATE `dish` SET taste = '麻辣', spiciness = '特辣' WHERE name = '蜀味水煮草鱼';
UPDATE `dish` SET taste = '酸辣', spiciness = '中辣' WHERE name = '老坛酸菜鱼';
UPDATE `dish` SET taste = '酸辣', spiciness = '中辣' WHERE name = '经典酸菜鮰鱼';
UPDATE `dish` SET taste = '咸鲜', spiciness = '不辣' WHERE name = '清蒸鲈鱼';
UPDATE `dish` SET taste = '辣',   spiciness = '中辣' WHERE name = '剁椒鱼头';
UPDATE `dish` SET taste = '麻辣', spiciness = '特辣' WHERE name = '草鱼2斤';
UPDATE `dish` SET taste = '麻辣', spiciness = '特辣' WHERE name = '江团鱼2斤';
UPDATE `dish` SET taste = '麻辣', spiciness = '特辣' WHERE name = '鮰鱼2斤';

-- 肉类
UPDATE `dish` SET taste = '咸鲜', spiciness = '不辣' WHERE name = '东坡肘子';
UPDATE `dish` SET taste = '咸鲜', spiciness = '不辣' WHERE name = '梅菜扣肉';
UPDATE `dish` SET taste = '酸辣', spiciness = '中辣' WHERE name = '金汤酸菜牛蛙';
UPDATE `dish` SET taste = '麻辣', spiciness = '中辣' WHERE name = '香锅牛蛙';
UPDATE `dish` SET taste = '麻辣', spiciness = '中辣' WHERE name = '馋嘴牛蛙';

-- 素菜
UPDATE `dish` SET taste = '麻辣', spiciness = '中辣' WHERE name = '麻婆豆腐';
UPDATE `dish` SET taste = '清淡', spiciness = '不辣' WHERE name = '清炒小油菜';
UPDATE `dish` SET taste = '蒜香', spiciness = '不辣' WHERE name = '蒜蓉娃娃菜';
UPDATE `dish` SET taste = '清淡', spiciness = '不辣' WHERE name = '清炒西兰花';
UPDATE `dish` SET taste = '清淡', spiciness = '不辣' WHERE name = '炝炒圆白菜';

-- 主食
UPDATE `dish` SET taste = '清淡', spiciness = '不辣' WHERE name = '米饭';
UPDATE `dish` SET taste = '清淡', spiciness = '不辣' WHERE name = '馒头';

-- 饮料
UPDATE `dish` SET taste = '甜',   spiciness = '不辣' WHERE name = '王老吉';
UPDATE `dish` SET taste = '甜',   spiciness = '不辣' WHERE name = '北冰洋';

-- 酒类
UPDATE `dish` SET taste = '清淡', spiciness = '不辣' WHERE name = '雪花啤酒';

-- 汤类
UPDATE `dish` SET taste = '咸鲜', spiciness = '不辣' WHERE name = '鸡蛋汤';
UPDATE `dish` SET taste = '咸鲜', spiciness = '不辣' WHERE name = '平菇豆腐汤';
