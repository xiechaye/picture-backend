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

-- 添加默认管理员(用户名admin,密码12345678)
INSERT INTO user (id, userAccount, userPassword, userName, userAvatar, userProfile, userRole, editTime, createTime, updateTime, isDelete, vipExpireTime, vipCode, vipNumber) VALUE
(1, 'admin', '12319ad49c00cabb8d3409b160a5180b', 'admin', 'https://picture-1327683584.cos.ap-guangzhou.myqcloud.com//avatar/2026-01-06_nCmMuRcKRiTdjxjQ.jpg', '111', 'admin', '2026-01-09 19:05:24', '2026-01-09 19:05:34', '2026-01-09 19:05:26', 0, null, null, null);

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

-- 新增 100 条示例提示词数据（中文分类，中英文混合 prompt）
INSERT INTO sample_prompt (title, prompt, category) VALUES
-- 自然风景（25条，前13条中文，后12条英文）
('晨雾森林', '清晨的原始森林，薄雾在树林间缭绕，阳光透过树叶洒下斑驳光影，露珠在草叶上闪烁，宁静祥和的氛围，自然摄影，高清画质', '自然风景'),
('星空银河', 'Milky Way galaxy stretching across a dark night sky, countless stars twinkling, silhouette of mountains in foreground, long exposure astrophotography, deep blue and purple tones, 8k resolution', '自然风景'),
('秋日枫林', '金秋时节的枫树林，红色和橙色的枫叶铺满地面，阳光穿透树冠，温暖的色调，落叶纷飞，秋天的诗意，风光摄影', '自然风景'),
('冰川湖泊', 'Crystal clear glacial lake with turquoise water, massive ice formations floating, snow-capped peaks reflecting in still water, pristine wilderness, dramatic lighting, landscape photography', '自然风景'),
('沙漠驼队', '广袤的沙漠中，驼队行走在金色沙丘上，夕阳西下，长长的影子，远处有绿洲，丝绸之路的意境，电影感构图', '自然风景'),
('瀑布彩虹', 'Majestic waterfall cascading down rocky cliffs, rainbow forming in the mist, lush green vegetation surrounding, powerful water flow, nature\'s beauty, high dynamic range photography', '自然风景'),
('竹林幽径', '翠绿的竹林深处，阳光从竹叶间洒下，石板小路蜿蜒向前，清新宁静，中国风意境，禅意摄影', '自然风景'),
('极光夜空', 'Northern lights dancing across Arctic sky, vibrant green and purple aurora borealis, snowy landscape below, stars visible, magical atmosphere, long exposure night photography', '自然风景'),
('梯田日落', '层层叠叠的梯田，夕阳映照下呈现金黄色，水面反射天空的色彩，农耕文明的美感，航拍视角，壮观景象', '自然风景'),
('海岸礁石', 'Dramatic rocky coastline with waves crashing against cliffs, sea spray in air, golden hour lighting, seabirds flying, powerful ocean, coastal landscape photography', '自然风景'),
('草原牧场', '辽阔的草原上，成群的牛羊在吃草，蓝天白云，远处有蒙古包，自由奔放的感觉，自然纪实摄影', '自然风景'),
('火山熔岩', 'Active volcano with glowing lava flows, molten rock cascading down slopes, smoke and ash rising, dramatic red and orange colors, raw power of nature, geological photography', '自然风景'),
('雨后彩虹', '雨后的山谷，双彩虹横跨天空，阳光穿透云层，绿色的山坡，清新的空气，希望的象征，风光摄影', '自然风景'),
('峡谷深渊', 'Deep canyon with layered rock formations, Colorado River flowing below, dramatic shadows and highlights, geological time visible in strata, aerial perspective, grand scale', '自然风景'),
('花海盛开', '春天的花海，五颜六色的花朵竞相开放，蜜蜂和蝴蝶飞舞，远处有风车，浪漫唯美，微距与广角结合', '自然风景'),
('冰雪世界', 'Frozen winter wonderland, snow-covered pine trees, icicles hanging from branches, soft blue light, peaceful silence, pristine white landscape, winter photography', '自然风景'),
('溶洞奇观', '神秘的地下溶洞，钟乳石和石笋形态各异，彩色灯光照射，地下河流淌，奇幻的地质景观，长曝光摄影', '自然风景'),
('云海日出', 'Sea of clouds at mountain summit, sun rising above cloud layer, golden rays piercing through, mountain peaks emerging like islands, breathtaking vista, landscape photography', '自然风景'),
('荷塘月色', '夏日荷塘，粉色荷花盛开，荷叶田田，月光洒在水面，蛙声阵阵，中国古典诗意，夜景摄影', '自然风景'),
('沙滩海浪', 'Pristine tropical beach with crystal clear turquoise water, gentle waves lapping shore, white sand, palm trees swaying, paradise setting, travel photography', '自然风景'),
('雾凇奇观', '冬日的雾凇景观，树枝上挂满冰晶，银装素裹，阳光照射下晶莹剔透，童话般的世界，冬季摄影', '自然风景'),
('火烧云', 'Dramatic sunset with fiery red and orange clouds, sun setting on horizon, silhouettes of landscape, spectacular sky colors, golden hour photography', '自然风景'),
('湿地候鸟', '湿地保护区，成群的候鸟在水面栖息，芦苇丛生，生态和谐，野生动物摄影，自然纪实', '自然风景'),
('山间溪流', 'Mountain stream flowing over smooth rocks, crystal clear water, moss-covered stones, forest surroundings, peaceful nature sounds, long exposure water photography', '自然风景'),
('田园风光', '乡村田园风光，金色的麦田，远处有农舍和教堂，蓝天白云，宁静祥和，欧洲乡村风格摄影', '自然风景'),

-- 人物肖像（20条，前10条中文，后10条英文）
('古装美人', '身穿华丽汉服的古典美人，精致的妆容，优雅的姿态，手持团扇，背景是古典园林，柔和的光线，古风人像摄影', '人物肖像'),
('商务精英', 'Professional business executive in tailored suit, confident posture, modern office background, natural lighting, corporate headshot, sharp focus, professional photography', '人物肖像'),
('街头少年', '时尚的街头少年，穿着潮牌服装，酷炫的发型，城市涂鸦墙背景，阳光侧逆光，街拍风格，青春活力', '人物肖像'),
('优雅老人', 'Elderly person with wise expression, weathered face showing life experience, soft natural light, black and white portrait, emotional depth, documentary style photography', '人物肖像'),
('芭蕾舞者', '优美的芭蕾舞者，身穿白色舞裙，优雅的舞姿，舞台灯光，动作定格，艺术摄影，高雅气质', '人物肖像'),
('摇滚歌手', 'Rock musician performing on stage, electric guitar, dramatic stage lighting, passionate expression, concert atmosphere, music photography, high energy', '人物肖像'),
('民族服饰', '身穿传统民族服饰的少数民族女性，精美的银饰，灿烂的笑容，自然光线，文化纪实摄影', '人物肖像'),
('运动健将', 'Athletic person in action, muscular physique, sports gear, dynamic pose, gym environment, motivational fitness photography, high contrast lighting', '人物肖像'),
('文艺青年', '文艺气质的年轻人，坐在咖啡馆里看书，温暖的室内光线，复古色调，生活方式摄影，安静氛围', '人物肖像'),
('时尚模特', 'Fashion model on runway, haute couture clothing, dramatic makeup, professional catwalk pose, fashion show lighting, editorial photography, high fashion', '人物肖像'),
('儿童纯真', '天真烂漫的儿童，灿烂的笑容，在公园里玩耍，自然光线，抓拍瞬间，儿童摄影，纯真美好', '人物肖像'),
('工匠精神', 'Skilled craftsman at work, focused expression, traditional tools, workshop environment, hands in detail, documentary photography, artisan culture', '人物肖像'),
('新娘婚纱', '美丽的新娘，洁白的婚纱，幸福的笑容，教堂或花园背景，柔和浪漫的光线，婚礼摄影', '人物肖像'),
('都市白领', 'Urban professional commuting in city, business casual attire, modern cityscape background, natural candid moment, lifestyle photography, contemporary style', '人物肖像'),
('瑜伽导师', '瑜伽导师展示高难度体式，身材匀称，专注的表情，自然环境或瑜伽馆，健康生活方式摄影', '人物肖像'),
('朋克少女', 'Punk girl with colorful hair, leather jacket, rebellious attitude, urban graffiti background, edgy fashion photography, alternative style', '人物肖像'),
('传统茶艺', '茶艺师优雅地泡茶，传统茶具，古典服饰，茶室环境，宁静禅意，中国传统文化摄影', '人物肖像'),
('科技极客', 'Tech enthusiast surrounded by gadgets, coding on multiple screens, modern workspace, blue light glow, contemporary lifestyle photography', '人物肖像'),
('街头艺人', '街头艺人在表演，吉他或其他乐器，路人围观，城市街道背景，纪实摄影，生活气息', '人物肖像'),
('医护人员', 'Healthcare worker in scrubs, compassionate expression, hospital environment, professional medical photography, dedication and care', '人物肖像'),

-- 动漫二次元（15条，前8条中文，后7条英文）
('魔法少女', '可爱的魔法少女，手持魔法杖，华丽的变身服装，星星和月亮装饰，梦幻的背景，日系动漫风格，明亮色彩', '动漫二次元'),
('机甲驾驶员', 'Mecha pilot in futuristic cockpit, high-tech interface displays, determined expression, sci-fi anime style, detailed mechanical design, dynamic composition', '动漫二次元'),
('校园青春', '日系校园场景，穿着校服的学生，樱花飘落，青春洋溢，温馨的氛围，动漫插画风格', '动漫二次元'),
('异世界冒险', 'Fantasy adventure scene with hero wielding sword, magical creatures, epic landscape, anime art style, vibrant colors, action-packed composition', '动漫二次元'),
('猫耳少女', '可爱的猫耳娘，大眼睛，甜美的笑容，女仆装，萌系画风，二次元文化，精致细节', '动漫二次元'),
('赛博朋克战士', 'Cyberpunk warrior with neon weapons, futuristic city background, anime style character design, glowing effects, high-tech aesthetic', '动漫二次元'),
('和风武士', '日本武士，手持武士刀，传统盔甲，樱花飞舞，浮世绘风格，动漫化处理，历史与幻想结合', '动漫二次元'),
('偶像歌手', 'Idol singer performing on stage, colorful costume, microphone, sparkles and lights, anime music video style, energetic pose', '动漫二次元'),
('龙族战士', '龙族战士，龙角和龙尾，华丽的铠甲，火焰特效，奇幻动漫风格，史诗感', '动漫二次元'),
('学院魔法师', 'Magic academy student casting spell, wizard robes, floating books, magical runes, fantasy anime setting, mystical atmosphere', '动漫二次元'),
('机器人伙伴', '可爱的机器人伙伴，圆润的造型，发光的眼睛，科幻动漫风格，温馨治愈系', '动漫二次元'),
('忍者刺客', 'Ninja assassin in stealth pose, traditional ninja outfit, moonlit night, anime action style, dynamic shadows, martial arts aesthetic', '动漫二次元'),
('精灵公主', '精灵族公主，尖耳朵，长发飘逸，森林背景，魔法光效，奇幻动漫风格，唯美画风', '动漫二次元'),
('时空旅行者', 'Time traveler with futuristic device, multiple time portals, anime sci-fi style, complex background, adventure theme', '动漫二次元'),
('妖狐少女', '九尾妖狐化身的少女，狐耳和尾巴，妖艳美丽，和风背景，日系动漫风格，神秘气质', '动漫二次元'),

-- 城市建筑（12条，前6条中文，后6条英文）
('东方明珠', '上海东方明珠塔夜景，霓虹灯闪烁，黄浦江倒影，现代都市地标，城市摄影，长曝光', '城市建筑'),
('古镇水乡', 'Ancient water town with traditional architecture, stone bridges over canals, lanterns hanging, peaceful atmosphere, Chinese heritage photography', '城市建筑'),
('摩天大楼', '现代摩天大楼群，玻璃幕墙反射天空，仰拍视角，几何线条，建筑摄影，都市感', '城市建筑'),
('欧式教堂', 'Gothic cathedral with intricate stone carvings, stained glass windows, towering spires, dramatic interior lighting, architectural masterpiece', '城市建筑'),
('胡同小巷', '北京老胡同，灰色砖墙，红色大门，自行车，生活气息，怀旧情怀，纪实摄影', '城市建筑'),
('现代美术馆', 'Contemporary art museum with avant-garde architecture, white curved walls, minimalist design, natural light flooding interior, architectural photography', '城市建筑'),
('夜市街景', '热闹的夜市街道，各种小吃摊位，霓虹招牌，人来人往，烟火气息，街头摄影', '城市建筑'),
('桥梁工程', 'Impressive suspension bridge spanning across water, steel cables, modern engineering, sunset lighting, infrastructure photography', '城市建筑'),
('传统四合院', '北京传统四合院，红墙灰瓦，天井院落，古树，中国传统建筑，文化遗产摄影', '城市建筑'),
('未来建筑', 'Futuristic building with organic shapes, sustainable design, green walls, innovative architecture, conceptual rendering', '城市建筑'),
('火车站大厅', '宏伟的火车站大厅，高挑的穹顶，自然光线，人流涌动，建筑空间摄影，透视感', '城市建筑'),
('海滨别墅', 'Luxury beachfront villa with infinity pool, modern architecture, ocean view, tropical paradise, real estate photography', '城市建筑'),

-- 科幻未来（10条，前5条中文，后5条英文）
('太空站', '巨大的太空站在地球轨道上，太阳能板展开，宇航员进行太空行走，科幻场景，写实风格', '科幻未来'),
('星际战舰', 'Massive starship in deep space, advanced propulsion systems, detailed hull design, nebula background, sci-fi concept art', '科幻未来'),
('未来城市', '未来主义城市，飞行汽车穿梭，全息广告，高科技建筑，赛博朋克美学，霓虹色调', '科幻未来'),
('机器人工厂', 'Automated robot factory, assembly lines with robotic arms, futuristic manufacturing, blue industrial lighting, technological advancement', '科幻未来'),
('虚拟现实', '人们戴着VR头盔，沉浸在虚拟世界中，数字化场景，科技感，未来生活方式', '科幻未来'),
('外星基地', 'Alien planet colony base, dome structures, red desert landscape, multiple moons in sky, space exploration theme', '科幻未来'),
('时间机器', '复杂的时间机器装置，发光的能量核心，科学实验室，时空扭曲特效，科幻概念', '科幻未来'),
('纳米科技', 'Microscopic view of nanotechnology, molecular structures, futuristic medical scene, scientific visualization, high-tech aesthetic', '科幻未来'),
('人工智能', 'AI consciousness visualization, neural networks, digital brain, glowing connections, futuristic technology concept', '科幻未来'),
('星际之门', '巨大的星际传送门，能量涡旋，科幻场景，宇宙探索，史诗级构图，特效渲染', '科幻未来'),

-- 艺术创意（8条，前4条中文，后4条英文）
('抽象几何', '抽象几何图形组合，鲜艳的色彩，现代艺术风格，视觉冲击力，创意设计', '艺术创意'),
('超现实主义', 'Surrealist artwork with impossible architecture, floating objects, dreamlike atmosphere, Salvador Dali inspired, artistic composition', '艺术创意'),
('水墨意境', '中国水墨画风格，山水意境，留白艺术，墨色渲染，传统与现代结合，艺术创作', '艺术创意'),
('波普艺术', 'Pop art style portrait, bold colors, comic book aesthetic, Andy Warhol inspired, contemporary art', '艺术创意'),
('光影艺术', '光影交错的艺术装置，色彩斑斓，空间感，现代艺术展览，创意摄影', '艺术创意'),
('数字艺术', 'Digital art with glitch effects, cyberpunk colors, abstract patterns, modern graphic design, artistic expression', '艺术创意'),
('拼贴艺术', '创意拼贴艺术，不同元素组合，超现实效果，艺术创作，视觉实验', '艺术创意'),
('极简主义', 'Minimalist art composition, simple shapes, limited color palette, negative space, modern aesthetic', '艺术创意'),

-- 动物生物（6条，前3条中文，后3条英文）
('雄狮王者', '威武的雄狮，鬃毛飘扬，草原之王，凝视远方，野生动物摄影，自然纪实', '动物生物'),
('海豚跃起', 'Dolphins leaping out of ocean water, synchronized movement, splashing water, marine life photography, joyful energy', '动物生物'),
('熊猫憨态', '可爱的大熊猫，憨态可掬，吃竹子，黑白分明，中国国宝，野生动物保护', '动物生物'),
('老鹰翱翔', 'Majestic eagle soaring in sky, wings spread wide, mountain background, bird of prey, wildlife photography', '动物生物'),
('蝴蝶翩翩', '色彩斑斓的蝴蝶，停在花朵上，微距摄影，细节清晰，自然之美', '动物生物'),
('狼群奔跑', 'Wolf pack running through snowy forest, wild nature, pack hunting, winter wildlife, dramatic action photography', '动物生物'),

-- 美食静物（4条，前2条中文，后2条英文）
('精致甜点', '精致的法式甜点，马卡龙和蛋糕，摆盘艺术，柔和光线，美食摄影，诱人色彩', '美食静物'),
('日式料理', 'Japanese sushi platter, fresh sashimi, artistic presentation, traditional ceramic plates, food photography, culinary art', '美食静物'),
('咖啡时光', '一杯拉花咖啡，木质桌面，书本和眼镜，温馨氛围，生活方式摄影，静物构图', '美食静物'),
('水果静物', 'Fresh fruit still life, vibrant colors, natural lighting, artistic arrangement, food photography, healthy lifestyle', '美食静物');
