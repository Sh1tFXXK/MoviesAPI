package com.example.moviesapi.Entity;

import lombok.Data;

@Data
public class BoxOfficeRecord {
    private String title;
    private String distributor;
    private String releaseDate;
    private Long budget;
    private BoxOffice_revenue revenue;
    private String mpaRating;
}