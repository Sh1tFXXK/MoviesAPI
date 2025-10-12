package com.example.moviesapi.Entity;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
public class Movie {
    @JsonIgnore
    private Long dbId;
    private String id;
    private String title;
    private String releaseDate;
    private String genre;
    private String distributor;
    private Long budget;
    private String mpaRating;
    private BoxOffice  boxOffice;
}
