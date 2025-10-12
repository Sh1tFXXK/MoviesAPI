package com.example.moviesapi.Entity;

import lombok.Data;

@Data
public class MovieCreate {
    private String title;
    private String genre;
    private String releaseDate;
    private String distributor;
    private Long budget;
    private String mpaRating;
}
