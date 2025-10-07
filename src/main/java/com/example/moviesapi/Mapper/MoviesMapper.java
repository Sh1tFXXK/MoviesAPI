package com.example.moviesapi.Mapper;

import com.example.moviesapi.Entity.Movie;
import com.example.moviesapi.Entity.RatingResult;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Mapper
public interface MoviesMapper {

    @Insert("INSERT INTO movies(title, genre, releaseDate, distributor, budget, mpaRating) " +
            "VALUES(#{title}, #{genre}, #{releaseDate}, #{distributor}, #{budget}, #{mpaRating})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Movie movie);

    @Update("UPDATE movies SET title=#{title}, genre=#{genre}, releaseDate=#{releaseDate}, " +
            "distributor=#{distributor}, budget=#{budget}, mpaRating=#{mpaRating} WHERE id=#{id}")
    int update(Movie movie);

    @Select("SELECT * FROM movies WHERE id = #{id}")
    Movie selectById(String id);

    @Select({
            "<script>",
            "SELECT * FROM movies",
            "WHERE 1=1",
            "<if test='title != null'> AND title LIKE CONCAT('%', #{title}, '%')</if>",
            "<if test='year != null'> AND YEAR(releaseDate) = #{year}</if>",
            "<if test='genre != null'> AND genre = #{genre}</if>",
            "<if test='distributor != null'> AND distributor = #{distributor}</if>",
            "<if test='budget != null'> AND budget &lt;= #{budget}</if>",
            "<if test='mpaRating != null'> AND mpaRating = #{mpaRating}</if>",
            "ORDER BY id",
            "LIMIT #{offset}, #{limit}",
            "</script>"
    })
    List<Movie> selectWithFilters(Map<String, Object> params);

    @Select("SELECT * FROM ratings WHERE movieTitle = #{title} AND raterId = #{raterId}")
    RatingResult selectRating(@Param("title") String title, @Param("raterId") String raterId);

    @Insert("INSERT INTO ratings(movieTitle, raterId, rating) VALUES(#{title}, #{raterId}, #{rating}) ON DUPLICATE KEY UPDATE rating = VALUES(rating)")
    int upsertRating(@Param("title") String title, @Param("raterId") String raterId, @Param("rating") Number rating);

    @Select("SELECT AVG(rating) as average, COUNT(*) as count FROM ratings WHERE movieTitle = #{title}")
    Map<String, Object> selectRatingAggregate(@Param("title") String title);
    
    @Select("SELECT * FROM movies WHERE title = #{title} LIMIT 1")
    Movie selectByTitle(@Param("title") String title);
}