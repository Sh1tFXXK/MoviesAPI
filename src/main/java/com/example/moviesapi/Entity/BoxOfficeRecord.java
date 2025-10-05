package com.example.moviesapi.Entity;

import lombok.Data;

@Data
public class BoxOfficeRecord {
    private String title;
    private String distributor;
    private String releaseDate;
    private Integer budget;
    private BoxOffice_revenue revenue;
    private String mpaRating;
    private String currency;
    private String source;
    private String lastUpdated;
}