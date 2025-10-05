package com.example.moviesapi.Entity;

import lombok.Data;

@Data
public class RatingResult {
    private String movieTitle;
    private String raterId;
    private Number rating;
}