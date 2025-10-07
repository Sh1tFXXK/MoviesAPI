# Movies API 

## 设计思路

### 1. 数据库选型和设计

选择 Mysql8.0 作为数据库：因为该数据库很稳定，针对所提供的字段可以抽象出两个表：movies和 ratings，一个记录电影的基本信息一个记录电影的评分情况。关系型数据库已经满足要求，不需要其他的数据库。

数据库设计
电影表 (movies)
```sql
-- 创建电影表
CREATE TABLE `movies` (
                          `id` int NOT NULL AUTO_INCREMENT,
                          `title` varchar(255) NOT NULL COMMENT '电影名字',
                          `genre` varchar(100) NOT NULL,
                          `releaseDate` varchar(50) DEFAULT NULL,
                          `mpaRating` varchar(50) DEFAULT NULL,
                          `lastUpdated` datetime DEFAULT NULL,
                          `source` varchar(50) DEFAULT NULL,
                          `distributor` varchar(255) DEFAULT NULL,
                          `budget` bigint DEFAULT NULL,
                          `revenue` bigint DEFAULT NULL,
                          `currency` varchar(50) DEFAULT NULL COMMENT '货币',
                          PRIMARY KEY (`id`),
                          KEY `idx_movies_title` (`title`),
                          KEY `idx_movies_genre` (`genre`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 创建评分表
CREATE TABLE `ratings` (
                           `movieTitle` varchar(255) NOT NULL,
                           `raterId` varchar(255) NOT NULL,
                           `rating` decimal(2,1) NOT NULL,
                           `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
                           `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           UNIQUE KEY `unique_movie_rater` (`movieTitle`,`raterId`),
                           KEY `idx_ratings_movietitle` (`movieTitle`),
                           KEY `idx_ratings_raterid` (`raterId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

设计思路
给评分表的(movieTitle, raterId)加上复合唯一键，保证同一个用户不能对同一部电影重复评分，如果已经有了就更新返回200，没有就新增返回201，正好满足题目要求的upsert要求。
可以将票房数据用Json的格式来存储，这样可以提高拓展性，不用以后改动时，不好改动
针对不同的条件可以加上索引，提高查询速度。
数据类型评分字段用上了mysql的DECIMAL(2,1)类型，这样既能保证精度不会丢失，又能支持特定步长的评分要求。

### 2. 后端服务选型和设计

选择Spring Boot+ MyBatis架构，这是常见的后端开发框架，即稳定又高效 Spring Boot可以快速搭建项目，和丰富的生态环境可以很方便的添加模块和使用别的组件。 MyBatis可以支持复杂的sql语句编写，可以利用注解映射数据库字段，可以快速的访问数据库。RestTemplate用于调用票房API maven用来管理依赖和构建

后端服务主要包含两个模块：电影服务和评分服务。电影服务提供电影数据查询和评分服务。评分服务提供评分数据查询和评分服务。
后端采用标准的控制层，服务层，和数据访问层，控制层负责接受和返回请求，也可以进行鉴权，服务层负责处理实际的业务逻辑，数据访问层负责与数据库进行交互，编写sql语句。实体类是文档中规范的类型，数据库的两个表，包含了所有需要的数据。
通过枚举定义不同的错误码，可以统一管理和方便调用，使结构更清晰，可维护。


### 3 后续优化

加上Redis，缓存热门的电影数据以及评分聚合结果，或者提前缓存，可以提高查询速度。
增加数据库连接池 提高并发处理的能力 异步处理票房API调用可以改为异步，提高响应速度
使用游标分页替代偏移分页，可以大幅度提高数据量大的时候的查询性能
API限流防止恶意请求，保护系统稳定性
使用读写分离主从数据库分离的技术，优化查询性能
后续可以把api网关的入口统一放到一个地方，然后统一的添加鉴权，方便管理，


