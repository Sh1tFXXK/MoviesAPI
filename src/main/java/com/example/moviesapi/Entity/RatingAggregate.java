package com.example.moviesapi.Entity;

import lombok.Data;

@Data
public class RatingAggregate {
    private Number average;
    private Integer count;

    public RatingAggregate(Double average, Integer count) {
        this.average = average;
        this.count = count;
    }
}