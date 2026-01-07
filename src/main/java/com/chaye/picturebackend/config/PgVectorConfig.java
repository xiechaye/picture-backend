package com.chaye.picturebackend.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

/**
 * @author chaye
 */
@Configuration
public class PgVectorConfig {
    @Value("${spring.ai.vectorstore.dimensions:1024}")
    private int dimensions;

    // ==========================================
    // 第一部分：PostgreSQL 基础连接配置
    // ==========================================

    /**
     * 1.1 定义配置读取器
     * 作用：读取 spring.datasource.vector 下的所有属性 (url, username, password...)
     * 并自动适配 HikariCP 所需的 jdbcUrl 格式
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.vector")
    public DataSourceProperties vectorDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * 1.2 创建 PostgreSQL 专用数据源
     * 作用：使用上面的配置属性构建真实的 DataSource
     */
    @Bean(name = "vectorDataSource")
    public DataSource vectorDataSource(@Qualifier("vectorDataSourceProperties") DataSourceProperties properties) {
        // initializeDataSourceBuilder() 会自动处理 url -> jdbcUrl 的转换
        return properties.initializeDataSourceBuilder().build();
    }

    /**
     * 2. 创建 PostgreSQL 专用的 JdbcTemplate
     */
    @Bean(name = "vectorJdbcTemplate")
    public JdbcTemplate vectorJdbcTemplate(@Qualifier("vectorDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // ==========================================
    // 第二部分：Spring AI VectorStore 配置
    // ==========================================

    /**
     * 3. 创建 VectorStore (用于存储和检索向量)
     * 关键点：参数里使用了 @Qualifier("vectorJdbcTemplate")，
     * 确保它使用的是 pgsql 的连接，而不是 MySQL 的！
     */
    @Bean
    public VectorStore vectorStore(
            @Qualifier("vectorJdbcTemplate") JdbcTemplate jdbcTemplate,
            EmbeddingModel dashscopeEmbeddingModel
    ) {
        return PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                .dimensions(dimensions)                // ⚠️注意：请确认 DashScope 模型的输出维度是否为 1536
                .distanceType(COSINE_DISTANCE)   // 距离算法：余弦相似度
                .indexType(HNSW)                 // 索引类型：HNSW (性能更好)
                .initializeSchema(true)          // 自动初始化表结构
                .schemaName("public")
                .vectorTableName("picture") // 表名
                .maxDocumentBatchSize(10000)
                .build();
    }
}