package com.example.moviesapi.Entity;

import lombok.Data;
import java.time.OffsetDateTime;

/**
 * 电影票房数据结构类
 * 用于封装电影票房相关信息，包括收入、货币类型、数据源及最后更新时间
 */
@Data
public class BoxOffice {
    private String currency;
    private String source;
    private OffsetDateTime lastUpdated;
    private BoxOffice_revenue revenue;
}