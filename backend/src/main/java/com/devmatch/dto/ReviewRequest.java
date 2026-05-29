package com.devmatch.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReviewRequest {
    @NotNull
    private Long projectId;
    @NotNull
    @Min(1) @Max(5)
    private Integer rating;
    private String content;
    private List<String> tags;
}
