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
    @Options(useGeneratedKeys = true, keyProperty = "dbId")
    int insert(Movie movie);

    @Update("UPDATE movies SET title=#{title}, genre=#{genre}, releaseDate=#{releaseDate}, " +
            "distributor=#{distributor}, budget=#{budget}, mpaRating=#{mpaRating}, " +
            "revenue=#{boxOffice.revenue.worldwide}, openingWeekendUSA=#{boxOffice.revenue.openingWeekendUSA}, " +
            "currency=#{boxOffice.currency}, source=#{boxOffice.source}, lastUpdated=#{boxOffice.lastUpdated} WHERE id=#{dbId}")
    int update(Movie movie);

    @Select("SELECT * FROM movies WHERE id = #{id}")
    @Results({
        @Result(property = "dbId", column = "id"),
        @Result(property = "boxOffice.revenue.worldwide", column = "revenue"),
        @Result(property = "boxOffice.revenue.openingWeekendUSA", column = "openingWeekendUSA"),
        @Result(property = "boxOffice.currency", column = "currency"),
        @Result(property = "boxOffice.source", column = "source"),
        @Result(property = "boxOffice.lastUpdated", column = "lastUpdated")
    })
    Movie selectById(Long id);

    @Select({
            "<script>",
            "SELECT * FROM movies",
            "WHERE 1=1",
            "<if test='title != null'> AND title LIKE CONCAT('%', #{title}, '%')</if>",
            "<if test='year != null'> AND YEAR(releaseDate) = #{year}</if>",
            "<if test='genre != null'> AND genre = #{genre}</if>",
            "<if test='distributor != null'> AND distributor = #{distributor}</if>",
            "<if test='budget != null'><![CDATA[ AND budget <= #{budget}]]></if>",
            "<if test='mpaRating != null'> AND mpaRating = #{mpaRating}</if>",
            "ORDER BY id",
            "LIMIT #{offset}, #{limit}",
            "</script>"
    })
    @Results({
        @Result(property = "dbId", column = "id"),
        @Result(property = "boxOffice.revenue.worldwide", column = "revenue"),
        @Result(property = "boxOffice.revenue.openingWeekendUSA", column = "openingWeekendUSA"),
        @Result(property = "boxOffice.currency", column = "currency"),
        @Result(property = "boxOffice.source", column = "source"),
        @Result(property = "boxOffice.lastUpdated", column = "lastUpdated")
    })
    List<Movie> selectWithFilters(Map<String, Object> params);

    @Select("SELECT * FROM ratings WHERE movieTitle = #{title} AND raterId = #{raterId}")
    RatingResult selectRating(@Param("title") String title, @Param("raterId") String raterId);

    @Insert("INSERT INTO ratings(movieTitle, raterId, rating) VALUES(#{title}, #{raterId}, #{rating}) ON DUPLICATE KEY UPDATE rating = VALUES(rating)")
    int upsertRating(@Param("title") String title, @Param("raterId") String raterId, @Param("rating") Number rating);

    @Select("SELECT AVG(rating) as average, COUNT(*) as count FROM ratings WHERE movieTitle = #{title}")
    Map<String, Object> selectRatingAggregate(@Param("title") String title);
    
    @Select("SELECT * FROM movies WHERE title = #{title} LIMIT 1")
    @Results({
        @Result(property = "dbId", column = "id"),
        @Result(property = "boxOffice.revenue.worldwide", column = "revenue"),
        @Result(property = "boxOffice.revenue.openingWeekendUSA", column = "openingWeekendUSA"),
        @Result(property = "boxOffice.currency", column = "currency"),
        @Result(property = "boxOffice.source", column = "source"),
        @Result(property = "boxOffice.lastUpdated", column = "lastUpdated")
    })
    Movie selectByTitle(@Param("title") String title);
}