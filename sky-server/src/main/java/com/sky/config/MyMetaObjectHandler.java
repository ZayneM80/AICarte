package com.sky.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.sky.context.BaseContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 */
@Component
@Slf4j
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("开始执行插入操作的自动填充...");

        try {
            this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
            this.strictInsertFill(metaObject, "createUser", BaseContext::getCurrentId, Long.class);
            this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
            this.strictInsertFill(metaObject, "updateUser", BaseContext::getCurrentId, Long.class);
        } catch (Exception e) {
            log.error("自动填充失败：{}", e.getMessage());
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("开始执行更新操作的自动填充...");

        try {
            this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
            this.strictUpdateFill(metaObject, "updateUser", BaseContext::getCurrentId, Long.class);
        } catch (Exception e) {
            log.error("自动填充失败：{}", e.getMessage());
        }
    }

}
