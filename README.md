<h1 align="center">Picture Backend</h1>

<p align="center">
  <strong>基于 Spring Boot 3 + AI 的智能图片管理平台</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange" alt="Java 17">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen" alt="Spring Boot 3.2.5">
  <img src="https://img.shields.io/badge/MyBatis%20Plus-3.5.15-blue" alt="MyBatis Plus">
  <img src="https://img.shields.io/badge/Sa--Token-1.39.0-red" alt="Sa-Token">
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License">
</p>

---

## 项目介绍

Picture Backend 是一个功能完善的智能图片管理后端系统，集成了 AI 图像分析、向量语义搜索、多用户协作编辑等能力。系统采用双数据源架构（MySQL + PostgreSQL/pgvector），支持按空间动态分表、实时 WebSocket 协作、AI Agent 智能工具调用等特性。

## 功能特性

### 图片管理
- 图片上传（本地文件 / URL）、批量上传与编辑
- 自动提取图片信息（尺寸、格式、主色调）
- 图片审核流程（待审核 → 通过/拒绝）
- 按颜色搜索、以图搜图、语义搜索

### AI 能力
- AI 图像理解与描述（qwen-vl-max）
- 文本向量化与语义检索（text-embedding-v2 + pgvector）
- AI 图像生成（wanx-v1）
- AI Prompt 优化 Agent（ReAct 模式）
- AI 扩图（image-out-painting）

### 空间与协作
- 私有空间 / 团队空间
- 基于角色的空间权限控制（viewer / editor / admin）
- WebSocket + Disruptor 实时协作编辑

### 系统能力
- Sa-Token 认证 + 自定义空间权限体系
- ShardingSphere 按 spaceId 动态分表
- RabbitMQ 异步消息处理
- Redis + Caffeine 多级缓存
- 腾讯云 COS 对象存储

## 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                     Controller 层                        │
├─────────────────────────────────────────────────────────┤
│                      Service 层                          │
├─────────────────────────────────────────────────────────┤
│                      Manager 层                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────────┐  │
│  │ Auth     │ │ Upload   │ │ Sharding │ │ WebSocket │  │
│  │ 权限管理  │ │ 上传策略  │ │ 分片算法  │ │ 实时通信   │  │
│  └──────────┘ └──────────┘ └──────────┘ └───────────┘  │
├─────────────────────────────────────────────────────────┤
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────────┐  │
│  │ MySQL    │ │ PgVector │ │ Redis    │ │ RabbitMQ  │  │
│  │ 业务数据  │ │ 向量存储  │ │ 缓存     │ │ 消息队列   │  │
│  └──────────┘ └──────────┘ └──────────┘ └───────────┘  │
└─────────────────────────────────────────────────────────┘
```

## 技术选型

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.5 | 基础框架 |
| Java | 17 | 开发语言 |
| MyBatis Plus | 3.5.15 | ORM 框架 |
| ShardingSphere | 5.4.1 | 分库分表 |
| Sa-Token | 1.39.0 | 权限认证 |
| Spring AI Alibaba | 1.1.0.0-RC2 | AI Agent 框架 |
| PostgreSQL + pgvector | - | 向量数据库 |
| Redis | - | 分布式缓存 / Session |
| Caffeine | - | 本地缓存 |
| RabbitMQ | - | 消息队列 |
| Disruptor | 3.4.2 | 高性能事件队列 |
| 腾讯云 COS | 5.6.227 | 对象存储 |
| DashScope SDK | 2.22.13 | 阿里云 AI 服务 |
| Knife4j | 4.5.0 | API 文档 |
| Hutool | 5.8.38 | 工具库 |

## 环境要求

| 环境 | 版本 |
|------|------|
| JDK | 17+ |
| Maven | 3.9+ |
| MySQL | 8.0+ |
| PostgreSQL | 14+（需安装 pgvector 扩展） |
| Redis | 6.0+ |
| RabbitMQ | 3.12+ |

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/xiechaye/picture-backend.git
cd picture-backend
```

### 2. 配置环境

复制本地配置文件并修改数据库连接、API Key 等信息：

```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

需要配置的关键项：
- MySQL 数据库连接
- PostgreSQL 数据库连接
- Redis 连接地址
- RabbitMQ 连接信息
- 腾讯云 COS 密钥
- 阿里云 DashScope API Key

### 3. 初始化数据库

- 创建 MySQL 数据库 `chaye_picture`
- 创建 PostgreSQL 数据库 `chaye_picture`，并安装 pgvector 扩展：
  ```sql
  CREATE EXTENSION IF NOT EXISTS vector;
  ```

### 4. 启动项目

```bash
mvn spring-boot:run
```

启动成功后访问：
- 应用地址：http://localhost:8123/api
- 接口文档：http://localhost:8123/api/doc.html

## 项目结构

```
picture-backend/
├── src/main/java/com/chaye/picturebackend/
│   ├── agent/              # AI Agent 框架（BaseAgent、ReActAgent）
│   ├── annotation/         # 自定义注解（@AuthCheck、@SaSpaceCheckPermission）
│   ├── aop/                # AOP 切面（权限拦截）
│   ├── api/                # 外部 API 封装
│   │   ├── aliyunai/       # 阿里云 AI API
│   │   ├── imageEdit/      # 图片编辑 API
│   │   └── imagesearch/    # 图片搜索 API
│   ├── common/             # 通用类（BaseResponse、PageRequest）
│   ├── config/             # 配置类（WebSocket、RabbitMQ、COS 等）
│   ├── controller/         # 控制器层
│   ├── exception/          # 全局异常处理
│   ├── listener/           # 消息监听器（RabbitMQ）
│   ├── manager/            # 管理层
│   │   ├── auth/           # 空间权限管理
│   │   ├── sharding/       # 分片算法
│   │   ├── upload/         # 文件上传策略（模板方法模式）
│   │   └── websocket/      # WebSocket + Disruptor
│   ├── mapper/             # MyBatis Mapper
│   ├── model/              # 数据模型（entity/dto/vo/enums）
│   ├── service/            # 业务服务层
│   ├── tools/              # AI Agent 工具
│   └── utils/              # 工具类
├── src/main/resources/
│   ├── application.yml     # 主配置
│   ├── application-local.yml   # 本地环境配置
│   └── biz/                # 业务配置文件
├── Dockerfile              # Docker 构建文件
└── pom.xml                 # Maven 依赖管理
```

## 接口文档

项目集成 Knife4j 增强版 Swagger，启动后访问：

```
http://localhost:8123/api/doc.html
```

主要接口模块：
- 用户管理（注册、登录、信息管理）
- 图片管理（上传、编辑、删除、搜索）
- 空间管理（创建、成员管理、权限控制）
- AI 功能（图像生成、Prompt 优化、语义搜索）

## 部署

### Docker 部署

```bash
# 构建镜像
docker build -t picture-backend .

# 运行容器
docker run -d \
  --name picture-backend \
  -p 8123:8123 \
  -e JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC" \
  picture-backend
```

### Maven 打包

```bash
mvn clean package -DskipTests
java -jar target/picture-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## License

[MIT License](LICENSE)
