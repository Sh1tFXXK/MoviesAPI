package com.example.moviesapi.service;

import com.example.moviesapi.Entity.*;
import com.example.moviesapi.Mapper.MoviesMapper;
import lombok.Getter;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class MoviesService {

    @Autowired
    private MoviesMapper  moviesMapper;

    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${boxoffice.url:}")
    private String boxOfficeUrl;

    /**
    *      * POST
    *      * movies 创建电影（成功后同步查询并合并票房数据）
    *      * 创建包含标题、类型和上映日期作为必填字段的电影记录
    *      * 创建成功后，同步调用上游 GET /boxoffice?title=...：
    *      * 上游返回200：将{收入、发行商、预算、MPA评级、货币、来源、最后更新时间}合并到电影记录中，但用户提供的值优先
    *      * 上游返回非200（例如404）：将boxOffice设为null，如果用户未提供发行商、预算、MPA评级，则将其留空，不阻碍创建。
    *      * 优先级规则：用户提供的字段（发行商、预算、MPA评级）始终优先于票房API中的相应数据。
    *      * @param movieCreate*/
    public Movie moviesPost(MovieCreate movieCreate) {
        // 创建电影对象并设置基本信息
        Movie movie = new Movie();
        BeanUtils.copyProperties(movieCreate, movie);
        
        // 插入电影记录
        int insertResult = moviesMapper.insert(movie);
        if (insertResult <= 0) {
            // 插入失败
            return null;
        }
        
        try {
            // 调用票房API
            if (boxOfficeUrl != null && !boxOfficeUrl.isEmpty()) {
                String url = boxOfficeUrl + "?title=" + movieCreate.getTitle();
                ResponseEntity<BoxOfficeRecord> response = restTemplate.getForEntity(url, BoxOfficeRecord.class);
                BoxOfficeRecord boxOfficeRecord = response.getBody();
                
                if (response.getStatusCode().value() == 200 && boxOfficeRecord != null) {
                    // 用户提供的字段优先，只有当用户未提供时才使用API数据
                    if (movie.getDistributor() == null || movie.getDistributor().isEmpty()) {
                        movie.setDistributor(boxOfficeRecord.getDistributor());
                    }
                    if (movie.getBudget() == null) {
                        movie.setBudget(boxOfficeRecord.getBudget());
                    }
                    if (movie.getMpaRating() == null || movie.getMpaRating().isEmpty()) {
                        movie.setMpaRating(boxOfficeRecord.getMpaRating());
                    }
                    
                    // 设置票房信息
                    BoxOffice boxOffice = new BoxOffice();
                    if (boxOfficeRecord.getRevenue() != null) {
                        boxOffice.setRevenue(boxOfficeRecord.getRevenue());
                    }
                    boxOffice.setCurrency(boxOfficeRecord.getCurrency());
                    boxOffice.setSource(boxOfficeRecord.getSource());
                    
                    // 转换时间格式
                    if (boxOfficeRecord.getLastUpdated() != null) {
                        try {
                            boxOffice.setLastUpdated(java.time.OffsetDateTime.parse(boxOfficeRecord.getLastUpdated()));
                        } catch (Exception e) {
                            // 如果时间格式解析失败，设置为当前时间
                            boxOffice.setLastUpdated(java.time.OffsetDateTime.now());
                        }
                    }
                    movie.setBoxOffice(boxOffice);
                } else {
                    movie.setBoxOffice(null);
                }
            }
        } catch (HttpStatusCodeException e) {
            // 只捕获上游异常，不影响创建主流程
            // boxOffice = null
            movie.setBoxOffice(null);
        } catch (Exception e) {
            // 其他异常也兜底，boxOffice = null
            movie.setBoxOffice(null);
        }
        
        // 更新电影记录
        int updateResult = moviesMapper.update(movie);
        if (updateResult <= 0) {
            // 更新失败，但不返回null，返回原始movie
            return movie;
        }
        
        // 返回更新后的电影记录
        Movie result = moviesMapper.selectById(movie.getId());
        return result != null ? result : movie; // 如果查询失败，至少返回原始movie
    }

    public MoviePage moviesGet(String q, Integer year, String genre, String distributor, Integer budget, String mpaRating, Integer limit, String cursor) {
            // 构建查询条件
            Map<String, Object> params = new HashMap<>();

            if (q != null && !q.isEmpty()) {
                params.put("title", q);
            }
            if (year != null) {
                params.put("year", year);
            }
            if (genre != null && !genre.isEmpty()) {
                params.put("genre", genre);
            }
            if (distributor != null && !distributor.isEmpty()) {
                params.put("distributor", distributor);
            }
            if (budget != null) {
                params.put("budget", budget);
            }
            if (mpaRating != null && !mpaRating.isEmpty()) {
                params.put("mpaRating", mpaRating);
            }

            // 设置分页参数
            int pageSize = (limit != null && limit > 0) ? limit : 10; // 默认每页10条
            params.put("limit", pageSize + 1); // 多查一条用于判断是否有下一页

            // 处理游标
            int offset = 0;
            if (cursor != null && !cursor.isEmpty()) {
                try {
                    String decodedCursor = new String(Base64.getDecoder().decode(cursor));
                    offset = Integer.parseInt(decodedCursor);
                } catch (Exception e) {
                    // 如果游标解析失败，从头开始
                    offset = 0;
                }
            }
            params.put("offset", offset);

            // 执行查询
            List<Movie> movies = moviesMapper.selectWithFilters(params);

            // 构建返回结果
            MoviePage page = new MoviePage();
            List<Movie> items = movies.size() > pageSize ?
                    movies.subList(0, pageSize) : movies;
            page.setItems(items);

            // 设置下一个游标
            if (movies.size() > pageSize) {
                int nextOffset = offset + pageSize;
                String nextCursor = Base64.getEncoder().encodeToString(
                        String.valueOf(nextOffset).getBytes());
                page.setNextCursor(nextCursor);
            }

            return page;
        }
    /**
     * 创建评分
     * @param title
     * @param rating
     * @param raterId
     * @return 返回包含是否为新创建评分信息的结果，如果电影不存在返回null
     */
    public RatingSubmissionResult ratingsPost(String title, Number rating, String raterId) {
        // 首先检查电影是否存在
        if (!movieExists(title)) {
            return null;
        }

        boolean wasExisting = ratingExists(title, raterId);
        moviesMapper.upsertRating(title, raterId, rating);
        
        // 返回评分结果
        RatingResult ratingResult = new RatingResult();
        ratingResult.setMovieTitle(title);
        ratingResult.setRaterId(raterId);
        ratingResult.setRating(rating);
        
        return new RatingSubmissionResult(ratingResult, wasExisting);
    }
    
    /**
     * 评分提交结果包装类
     */
    public static class RatingSubmissionResult {
        @Getter
        private final RatingResult ratingResult;
        private final boolean wasExisting;
        
        public RatingSubmissionResult(RatingResult ratingResult, boolean wasExisting) {
            this.ratingResult = ratingResult;
            this.wasExisting = wasExisting;
        }

        public boolean wasExisting() {
            return wasExisting;
        }
    }
    
    /**
     * 检查评分是否已存在
     */
    public boolean ratingExists(String title, String raterId) {
        RatingResult existingRating = moviesMapper.selectRating(title, raterId);
        return existingRating != null;
    }

    public RatingAggregate ratingsGet(String title) {
        // 首先检查电影是否存在
        if (!movieExists(title)) {
            return null; // 电影不存在，返回null让Controller返回404
        }
        
        Map<String, Object> ratingData = moviesMapper.selectRatingAggregate(title);
        
        Double average = null;
        Integer count = 0;
        
        if (ratingData != null) {
            Object avgObj = ratingData.get("average");
            Object countObj = ratingData.get("count");
            
            if (avgObj instanceof Double) {
                average = (Double) avgObj;
            } else if (avgObj instanceof Number) {
                average = ((Number) avgObj).doubleValue();
            }
            
            if (countObj instanceof Long) {
                count = ((Long) countObj).intValue();
            } else if (countObj instanceof Integer) {
                count = (Integer) countObj;
            }
        }
        
        // 将平均值四舍五入到1位小数
        if (average != null) {
            average = Math.round(average * 10.0) / 10.0;
        } else {
            average = 0.0;
        }
        
        return new RatingAggregate(average, count);
    }
    
    /**
     * 检查电影是否存在
     */
    public boolean movieExists(String title) {
        Movie movie = moviesMapper.selectByTitle(title);
        return movie != null;
    }
}