-- 创建电影表
CREATE TABLE IF NOT EXISTS movies
(
    id          int auto_increment
        primary key,
    title       varchar(255) not null comment '电影名字',
    genre       varchar(100) not null,
    releaseDate varchar(50) null,
    mpaRating   varchar(50) null,
    lastUpdated datetime    null,
    source      varchar(50) null,
    distributor varchar(255) null,
    budget      bigint      null,
    revenue     bigint      null,
    currency    varchar(50) null comment '货币'
);

-- 创建评分表（修复版本）
CREATE TABLE IF NOT EXISTS ratings
(
    movieTitle varchar(255) not null,
    raterId    varchar(255) not null,
    rating     decimal(2,1) not null,  -- 限制为0.5步长的评分
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp on update current_timestamp,
    -- 添加唯一约束确保同一用户对同一电影只有一个评分
    UNIQUE KEY unique_movie_rater (movieTitle, raterId)
);

-- 创建索引
CREATE INDEX idx_movies_title ON movies(title);
CREATE INDEX idx_movies_genre ON movies(genre);
CREATE INDEX idx_ratings_movietitle ON ratings(movieTitle);
CREATE INDEX idx_ratings_raterid ON ratings(raterId);

