package com.sky.ai.vector;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * pgvector 数据源配置（独立于主库 MySQL）
 *
 * 不暴露 DataSource Bean，避免与 Druid 主数据源冲突
 * 直接创建 JdbcTemplate 供 DishVectorRepository 使用
 *
 * 面试亮点：多数据源 + 异构存储（MySQL 存业务、pgvector 存向量）
 */
@Configuration
public class VectorDataSourceConfig {

    @Value("${spring.vector.datasource.url}")
    private String url;

    @Value("${spring.vector.datasource.username}")
    private String username;

    @Value("${spring.vector.datasource.password}")
    private String password;

    @Value("${spring.vector.datasource.driver-class-name}")
    private String driverClassName;

    @Bean(name = "vectorJdbcTemplate")
    public JdbcTemplate vectorJdbcTemplate() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName(driverClassName);
        return new JdbcTemplate(ds);
    }
}
