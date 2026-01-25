-- 创建库
create database if not exists chaye_picture;

-- 切换库
use chaye_picture;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 图片表
create table if not exists picture
(
    id           bigint auto_increment comment 'id' primary key,
    url          varchar(512)                       not null comment '图片 url',
    name         varchar(128)                       not null comment '图片名称',
    introduction varchar(512)                       null comment '简介',
    category     varchar(64)                        null comment '分类',
    tags         varchar(512)                       null comment '标签（JSON 数组）',
    picSize      bigint                             null comment '图片体积',
    picWidth     int                                null comment '图片宽度',
    picHeight    int                                null comment '图片高度',
    picScale     double                             null comment '图片宽高比例',
    picFormat    varchar(32)                        null comment '图片格式',
    userId       bigint                             not null comment '创建用户 id',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime     datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    INDEX idx_name (name),                 -- 提升基于图片名称的查询性能
    INDEX idx_introduction (introduction), -- 用于模糊搜索图片简介
    INDEX idx_category (category),         -- 提升基于分类的查询性能
    INDEX idx_tags (tags),                 -- 提升基于标签的查询性能
    INDEX idx_userId (userId)              -- 提升基于用户 ID 的查询性能
) comment '图片' collate = utf8mb4_unicode_ci;


ALTER TABLE picture
    -- 添加新列
    ADD COLUMN reviewStatus INT DEFAULT 0 NOT NULL COMMENT '审核状态：0-待审核; 1-通过; 2-拒绝',
    ADD COLUMN reviewMessage VARCHAR(512) NULL COMMENT '审核信息',
    ADD COLUMN reviewerId BIGINT NULL COMMENT '审核人 ID',
    ADD COLUMN reviewTime DATETIME NULL COMMENT '审核时间';

-- 创建基于 reviewStatus 列的索引
CREATE INDEX idx_reviewStatus ON picture (reviewStatus);

ALTER TABLE picture
    -- 添加新列
    ADD COLUMN thumbnailUrl varchar(512) NULL COMMENT '缩略图 url';


-- 空间表
create table if not exists space
(
    id         bigint auto_increment comment 'id' primary key,
    spaceName  varchar(128)                       null comment '空间名称',
    spaceLevel int      default 0                 null comment '空间级别：0-普通版 1-专业版 2-旗舰版',
    maxSize    bigint   default 0                 null comment '空间图片的最大总大小',
    maxCount   bigint   default 0                 null comment '空间图片的最大数量',
    totalSize  bigint   default 0                 null comment '当前空间下图片的总大小',
    totalCount bigint   default 0                 null comment '当前空间下的图片数量',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime   datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    -- 索引设计
    index idx_userId (userId),        -- 提升基于用户的查询效率
    index idx_spaceName (spaceName),  -- 提升基于空间名称的查询效率
    index idx_spaceLevel (spaceLevel) -- 提升按空间级别查询的效率
) comment '空间' collate = utf8mb4_unicode_ci;

-- 添加新列
ALTER TABLE picture
    ADD COLUMN spaceId bigint  null comment '空间 id（为空表示公共空间）';

-- 创建索引
CREATE INDEX idx_spaceId ON picture (spaceId);

-- 添加新列
ALTER TABLE picture
    ADD COLUMN picColor varchar(16) null comment '图片主色调';

-- 支持空间类型，添加新列
ALTER TABLE space
    ADD COLUMN spaceType int default 0 not null comment '空间类型：0-私有 1-团队';

CREATE INDEX idx_spaceType ON space (spaceType);

-- 空间成员表
create table if not exists space_user
(
    id         bigint auto_increment comment 'id' primary key,
    spaceId    bigint                                 not null comment '空间 id',
    userId     bigint                                 not null comment '用户 id',
    spaceRole  varchar(128) default 'viewer'          null comment '空间角色：viewer/editor/admin',
    createTime datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    -- 索引设计
    UNIQUE KEY uk_spaceId_userId (spaceId, userId), -- 唯一索引，用户在一个空间中只能有一个角色
    INDEX idx_spaceId (spaceId),                    -- 提升按空间查询的性能
    INDEX idx_userId (userId)                       -- 提升按用户查询的性能
) comment '空间用户关联' collate = utf8mb4_unicode_ci;

-- 扩展用户表：新增会员功能
ALTER TABLE user
    ADD COLUMN vipExpireTime datetime NULL COMMENT '会员过期时间',
    ADD COLUMN vipCode varchar(128) NULL COMMENT '会员兑换码',
    ADD COLUMN vipNumber bigint NULL COMMENT '会员编号';

-- 示例提示词表
create table if not exists sample_prompt
(
    id         bigint auto_increment comment 'id' primary key,
    title      varchar(128)                       not null comment '短中文标题',
    prompt     text                               not null comment '用于AI的长英文提示词',
    category   varchar(64)                        not null comment '分类（如 Scenery, Anime, Cyberpunk）',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    INDEX idx_category (category)
) comment '示例提示词' collate = utf8mb4_unicode_ci;

-- 初始示例数据（4个分类，10条数据）
INSERT INTO sample_prompt (title, prompt, category) VALUES
('日落海滩', 'A breathtaking sunset over a serene beach with golden sand, calm turquoise waters reflecting the vibrant orange and pink sky, silhouettes of palm trees swaying gently, warm lighting, photorealistic, 8k resolution, cinematic composition', 'Scenery'),
('雪山湖泊', 'Majestic snow-capped mountains surrounding a crystal-clear alpine lake, mirror-like water reflection, evergreen forest in the foreground, dramatic clouds, golden hour lighting, landscape photography style, ultra detailed, 4k', 'Scenery'),
('樱花小径', 'A peaceful Japanese garden path lined with blooming cherry blossom trees, pink petals floating in the air, traditional stone lanterns, soft morning light filtering through branches, serene atmosphere, Studio Ghibli inspired, dreamy', 'Scenery'),
('赛博朋克城市', 'Futuristic cyberpunk cityscape at night, towering neon-lit skyscrapers, holographic advertisements, flying vehicles, rain-soaked streets reflecting colorful lights, dense urban environment, Blade Runner aesthetic, moody atmosphere, 8k', 'Cyberpunk'),
('霓虹街道', 'Narrow alleyway in a cyberpunk metropolis, steam rising from vents, neon signs in Japanese and Chinese characters, a lone figure with an umbrella, wet pavement reflections, high contrast lighting, cinematic noir style', 'Cyberpunk'),
('动漫少女', 'Beautiful anime girl with long flowing silver hair, wearing an elegant blue kimono with floral patterns, standing under a full moon, cherry blossoms falling around her, soft ethereal glow, detailed illustration, anime art style', 'Anime'),
('机甲战士', 'Powerful mecha robot in dynamic battle pose, sleek metallic armor with glowing energy cores, dramatic explosion in background, detailed mechanical design, anime style, high contrast lighting, epic composition', 'Anime'),
('现代建筑', 'Award-winning modern architectural masterpiece, minimalist white concrete structure with large glass windows, geometric shapes, surrounded by reflecting pool, clear blue sky, professional architectural photography, symmetrical composition', 'Architecture'),
('古典宫殿', 'Grand European palace interior with ornate baroque decorations, golden chandeliers, marble columns, intricate ceiling frescoes, red velvet curtains, dramatic natural light streaming through tall windows, opulent atmosphere', 'Architecture'),
('未来都市', 'Utopian future city with sustainable architecture, vertical gardens on skyscrapers, clean energy infrastructure, elevated walkways, blue sky with fluffy clouds, optimistic sci-fi aesthetic, detailed urban planning, 4k render', 'Architecture');
