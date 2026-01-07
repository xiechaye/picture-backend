package com.chaye.picturebackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import javax.sql.DataSource;

/**
 * 主数据源配置 (MySQL)
 * 确保 MyBatis 和其他默认组件使用这个，而不是去连 PostgreSQL
 */
@Configuration
public class MasterDataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource") // 读取 application.yml 根节点的配置
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }
}