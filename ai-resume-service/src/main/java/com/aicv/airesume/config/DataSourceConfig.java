package com.aicv.airesume.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 数据源配置类，用于配置HikariCP连接池以处理数据库冷启动问题
 */
@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);

        // 基础连接池配置
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(300000); // 5分钟
        config.setMaxLifetime(1800000); // 30分钟

        // 关键配置 - 处理冷启动问题
        config.setConnectionTimeout(30000); // 30秒，给数据库足够的时间冷启动
        config.setValidationTimeout(10000); // 10秒
        config.setConnectionTestQuery("SELECT 1"); // 测试连接是否有效
        
        // 在连接获取失败时自动重试的配置
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        
        // 对于MySQL，启用自动重连
        config.addDataSourceProperty("autoReconnect", "true");
        config.addDataSourceProperty("maxReconnects", "3");
        config.addDataSourceProperty("initialTimeout", "10");

        return new HikariDataSource(config);
    }
}