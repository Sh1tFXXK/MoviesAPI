package com.example.moviesapi.service;

import com.example.moviesapi.Entity.*;
import com.example.moviesapi.Mapper.MoviesMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class MoviesService {

    @Autowired
    private MoviesMapper  moviesMapper;

    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${boxoffice.url:}")
    private String boxOfficeUrl;
    
    @Value("${boxoffice.api.key:}")
    private String boxOfficeApiKey;

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

        // 注意：由于使用了 useGeneratedKeys，movie.getDbId() 现在包含数据库生成的 Long 类型 ID
        Long dbId = movie.getDbId();
        movie.setId("m_" + dbId);

        try {
            // 调用票房API
            if (boxOfficeUrl != null && !boxOfficeUrl.isEmpty()) {
                String url = UriComponentsBuilder.fromHttpUrl(boxOfficeUrl)
                        .queryParam("title", movieCreate.getTitle())
                        .toUriString();
                
                log.info("Calling Box Office API for movie: {}", movieCreate.getTitle());
                
                // 设置请求头，包含 X-API-Key 认证
                HttpHeaders headers = new HttpHeaders();
                if (boxOfficeApiKey != null && !boxOfficeApiKey.isEmpty()) {
                    headers.set("X-API-Key", boxOfficeApiKey);
                }
                HttpEntity<?> entity = new HttpEntity<>(headers);
                
                ResponseEntity<BoxOfficeRecord> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, BoxOfficeRecord.class);
                BoxOfficeRecord boxOfficeRecord = response.getBody();
                
                if (response.getStatusCode().value() == 200 && boxOfficeRecord != null) {
                    log.info("Successfully retrieved box office data for movie: {}", movieCreate.getTitle());
                    
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

                    if (boxOfficeRecord.getRevenue() != null && boxOfficeRecord.getRevenue().getWorldwide() != null) {
                        BoxOffice boxOffice = new BoxOffice();
                        boxOffice.setRevenue(boxOfficeRecord.getRevenue());
                        boxOffice.setCurrency("USD");
                        boxOffice.setSource("BoxOfficeAPI");
                        boxOffice.setLastUpdated(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z");
                        movie.setBoxOffice(boxOffice);
                    } else {
                        movie.setBoxOffice(null);
                    }
                } else {
                    log.warn("Box Office API returned non-200 response or empty body for movie: {}", movieCreate.getTitle());
                    movie.setBoxOffice(null);
                }
            } else {
                log.warn("Box Office URL not configured, skipping box office data retrieval");
                movie.setBoxOffice(null);
            }
        } catch (HttpStatusCodeException e) {
            // 记录具体的HTTP错误状态码和响应
            log.warn("Box Office API returned error {} for movie '{}': {}", 
                     e.getStatusCode(), movieCreate.getTitle(), e.getResponseBodyAsString());
            movie.setBoxOffice(null);
        } catch (Exception e) {
            // 记录其他异常
            log.error("Error calling Box Office API for movie '{}': {}", movieCreate.getTitle(), e.getMessage(), e);
            movie.setBoxOffice(null);
        }
        
        // 更新电影记录
        int updateResult = moviesMapper.update(movie);
        if (updateResult <= 0) {
            return movie;
        }
        
        // 返回更新后的电影记录
        Movie result = moviesMapper.selectById(movie.getDbId());
        if (result != null) {
            if ((result.getId() == null || result.getId().isEmpty()) && result.getDbId() != null) {
                result.setId("m_" + result.getDbId());
            }
            return result;
        }
        return movie;
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
            int pageSize = (limit != null && limit > 0) ? limit : 10;
            params.put("limit", pageSize + 1);

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
            // 为每个返回的条目补齐对外 id
            for (Movie m : items) {
                if (m != null && (m.getId() == null || m.getId().isEmpty()) && m.getDbId() != null) {
                    m.setId("m_" + m.getDbId());
                }
            }
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