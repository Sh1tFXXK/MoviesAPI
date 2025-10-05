package com.example.moviesapi.BO;

import lombok.Data;

@Data
public class Response {
    private String Description;
    private String Code;
    private Object items;
}

