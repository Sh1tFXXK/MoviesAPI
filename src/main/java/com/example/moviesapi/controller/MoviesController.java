package com.example.moviesapi.controller;

import com.example.moviesapi.Entity.*;
import com.example.moviesapi.Entity.Error;
import com.example.moviesapi.service.MoviesService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.net.URI;

@RestController
@RequestMapping("/movies")
public class MoviesController {

    @Autowired
    MoviesService moviesService;
    
    @Value("${auth.token}")
    private String authToken;

    /**
     * 获取电影列表
     * @param q 搜索关键词
     * @param year 年份
     * @param genre 分类
     * @param distributor 分销商
     * @param budget 预算
     * @param mapRating 评分
     * @param limit 每页数量
     * @param cursor 分页游标
     * @return
     * List and search movies
     *
     */

    @GetMapping
    public ResponseEntity<?> moviesGet(String q , Integer year , String genre , String distributor, Integer budget, String mapRating, Integer limit, String cursor, HttpServletRequest request) {
        //校验必要参数
        
        // 校验 limit 参数
        if (limit != null && (limit < 1 || limit > 100)) {
            Error error = new Error("BAD_REQUEST", "Limit must be between 1 and 100");
            return ResponseEntity.badRequest().body(error);
        }
        
        // 校验 year 参数
        if (year != null && (year < 1900 || year > 2030)) {
            Error error = new Error("BAD_REQUEST", "Year must be between 1900 and 2030");
            return ResponseEntity.badRequest().body(error);
        }
        
        // 校验 budget 参数
        if (budget != null && budget < 0) {
            Error error = new Error("BAD_REQUEST", "Budget must be non-negative");
            return ResponseEntity.badRequest().body(error);
        }
        
        // 校验 mapRating 参数（MPA评级）
        if (mapRating != null && !isValidMpaRating(mapRating)) {
            Error error = new Error("BAD_REQUEST", "Invalid MPA rating. Valid values are: G, PG, PG-13, R, NC-17");
            return ResponseEntity.badRequest().body(error);
        }
        
        MoviePage page = moviesService.moviesGet(q,year,genre,distributor,budget,mapRating,limit,cursor);
        return ResponseEntity.ok(page);
    }
    @PostMapping
    public ResponseEntity<?> moviesPost(@RequestBody MovieCreate movieCreate, HttpServletRequest request)  {
        //鉴权
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            Error error = new Error("UNAUTHORIZED", "Missing or invalid authentication information");
            return ResponseEntity.status(401).body(error);
        }
        // 权限验证 - 检查Bearer token是否有效
        String token = authHeader.substring(7); // 移除 "Bearer " 前缀
        String expectedToken = authToken;
        if (expectedToken == null || expectedToken.isEmpty() || !expectedToken.equals(token)) {
            Error error = new Error("UNAUTHORIZED", "Missing or invalid authentication information");
            return ResponseEntity.status(401).body(error);
        }
        //校验必要参数
        if(movieCreate.getTitle() == null || movieCreate.getTitle().trim().isEmpty() || 
           movieCreate.getReleaseDate() == null || 
           movieCreate.getGenre() == null || movieCreate.getGenre().trim().isEmpty()){
            Error error = new Error("UNPROCESSABLE_ENTITY","Invalid parameters");
            return ResponseEntity.status(422).body(error);
        }
        //创建电影
        Movie movie = moviesService.moviesPost(movieCreate);
        if (movie == null) {
            Error error = new Error("INTERNAL_SERVER_ERROR", "Failed to create movie");
            return ResponseEntity.status(500).body(error);
        }
        // 使用环境变量构建Location头，避免硬编码
        String baseUrl = System.getenv("BASE_URL");
        return ResponseEntity.created(URI.create(baseUrl + "/movies/" + movie.getId()))
                .body(movie);
    }
    
    @PostMapping("/{title}/ratings")
    public ResponseEntity<?> ratingsPost(@PathVariable String title, @RequestBody RatingSubmit ratingSubmit, HttpServletRequest request) {
        String raterId = request.getHeader("X-Rater-Id");
        if (raterId == null || raterId.isEmpty()) {
            Error error = new Error("UNAUTHORIZED", "Missing or invalid authentication information");
            return ResponseEntity.status(401).body(error);
        }
        
        //校验必要参数
        if(title == null || title.isEmpty()) {
            Error error = new Error("BAD_REQUEST", "Title is required");
            return ResponseEntity.status(400).body(error);
        }
        
        // 验证评分值是否在合法范围内（0.5的步长，从0.5到5.0）
        Double rating = ratingSubmit.getRating();
        if(rating == null || !isValidRating(rating)) {
            Error error = new Error("UNPROCESSABLE_ENTITY", "Rating must be one of {0.5, 1.0, 1.5, ..., 5.0}");
            return ResponseEntity.status(422).body(error);
        }
        
        // 评分提交
        MoviesService.RatingSubmissionResult result = moviesService.ratingsPost(title, rating, raterId);
        
        // 检查电影是否存在
        if (result == null) {
            Error error = new Error("NOT_FOUND", "Movie not found");
            return ResponseEntity.status(404).body(error);
        }
        
        // 根据是否是新评分返回不同的状态码
        if (result.wasExisting()) {
            // 更新现有评分，返回200
            return ResponseEntity.ok(result.getRatingResult());
        } else {
            // 创建新评分，返回201
            return ResponseEntity.status(201).body(result.getRatingResult());
        }
    }
    
    /**
     * 验证评分是否为合法值（0.5的步长，从0.5到5.0）
     * @param rating 评分值
     * @return 是否合法
     */
    private boolean isValidRating(Number rating) {
        double value = rating.doubleValue();
        // 检查是否是0.5的倍数，并且在0.5到5.0之间
        return value >= 0.5 && value <= 5.0 && (value * 2) == Math.floor(value * 2);
    }
    
    /**
     * 校验MPA评级是否有效
     * @param rating MPA评级
     * @return 是否有效
     */
    private boolean isValidMpaRating(String rating) {
        if (rating == null || rating.trim().isEmpty()) {
            return false;
        }
        String[] validRatings = {"G", "PG", "PG-13", "R", "NC-17"};
        String upperRating = rating.trim().toUpperCase();
        for (String validRating : validRatings) {
            if (validRating.equals(upperRating)) {
                return true;
            }
        }
        return false;
    }
    @GetMapping("/{title}/rating")
    public ResponseEntity<?> ratingsGet(@PathVariable String title) {
        RatingAggregate ratingAggregate = moviesService.ratingsGet(title);
        if(ratingAggregate == null){
            Error error = new Error("NOT_FOUND", "Resource not found");
            return ResponseEntity.status(404).body(error);
        }
        return ResponseEntity.ok(ratingAggregate);
    }
}