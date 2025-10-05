package com.example.moviesapi.controller;

import com.example.moviesapi.BO.ApiResponse;
import com.example.moviesapi.Entity.Error;
import com.example.moviesapi.Entity.Movie;
import com.example.moviesapi.Entity.MovieCreate;
import com.example.moviesapi.service.MoviesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class Movies {

    @Autowired
    MoviesService moviesService;

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

    @GetMapping("/movies")
    public ApiResponse moviesGet(String q , Integer year , String genre , String distributor, Integer budget, String mapRating, Integer limit, String cursor) {
        moviesService.moviesGet(q,year,genre,distributor,budget,mapRating,limit,cursor);
        return new ApiResponse<>(200,null,null);

    }
    @PostMapping("/movies")
    public ApiResponse<?> moviesPost(MovieCreate movieCreate)  {
        //鉴权
        if(){
            return new ApiResponse<>(401,null,new Error("UNAUTHORIZED","Missing or invalid authentication information"));
        }
        if (){
            return new ApiResponse<>(403,null,new Error("FORBIDDEN","No permission to perform this operation"));
        }
        //校验必要参数
        if(movieCreate.getTitle() == null || movieCreate.getReleaseDate() == null|| movieCreate.getGenre() == null){
            Error error = new Error("BAD_REQUEST","Invalid parameters");
            return new ApiResponse<>(400,null,error);
        }
        //创建电影
        Movie movie =moviesService.moviesPost(movieCreate);
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Location", Arrays.asList("https://api.example.com/movies/" + movie.getId()));
        return new ApiResponse<>(201,headers,movie);
    }
}
