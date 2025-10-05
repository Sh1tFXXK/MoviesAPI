package com.example.moviesapi.Entity;

import lombok.Data;

@Data
public class Movie {
    private String id;
    private String title;
    private String releaseDate;
    private String genre;
    private String distributor;
    private Integer budget;
    private String mpaRating;
    private BoxOffice  boxOffice;
}
