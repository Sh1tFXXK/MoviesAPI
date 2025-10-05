package com.example.moviesapi.Entity;

import lombok.Data;

import java.util.List;

@Data
public class MoviePage {
    private List<Movie> items;
    private String nextCursor;
}
